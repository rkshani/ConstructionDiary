package com.diary.construction.ui.expenses

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diary.construction.R
import com.diary.construction.core.util.Dates
import com.diary.construction.core.util.Money
import com.diary.construction.data.photo.PhotoStorage
import com.diary.construction.domain.model.Expense
import com.diary.construction.domain.model.ExpenseCategory
import com.diary.construction.domain.model.Stage
import com.diary.construction.domain.repository.ExpenseRepository
import com.diary.construction.domain.repository.StageRepository
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.BillPhotoField
import com.diary.construction.ui.components.ConfirmDeleteDialog
import com.diary.construction.ui.components.DateField
import com.diary.construction.ui.components.DropdownField
import com.diary.construction.ui.components.FormTextField
import com.diary.construction.ui.components.MoneyField
import com.diary.construction.ui.components.displayName
import com.diary.construction.ui.components.labelRes
import com.diary.construction.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpenseForm(
    val category: ExpenseCategory = ExpenseCategory.TRANSPORT,
    val amount: String = "",
    val stageId: Long? = null,
    val date: Long = Dates.today(),
    val note: String = "",
    val billPhotoPath: String? = null,
    val showErrors: Boolean = false,
) {
    val amountError get() = (Money.parse(amount) ?: 0) <= 0
}

@HiltViewModel
class ExpenseEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val expenses: ExpenseRepository,
    private val stageRepo: StageRepository,
    private val photos: PhotoStorage,
) : ViewModel() {
    private val projectId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_PROJECT_ID))
    private val id: Long = savedState.get<Long>(Routes.ARG_ID) ?: 0L
    val isNew = id == 0L
    private var original: Expense? = null
    private val replacedPhotos = mutableListOf<String>()

    var form by mutableStateOf(ExpenseForm())
        private set
    var stages by mutableStateOf<List<Stage>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            stages = stageRepo.getStages(projectId)
            if (!isNew) {
                expenses.getExpense(id)?.let { e ->
                    original = e
                    form = ExpenseForm(e.category, Money.toInput(e.amount), e.stageId, e.date, e.note, e.billPhotoPath)
                }
            }
        }
    }

    fun update(change: ExpenseForm.() -> ExpenseForm) {
        form = form.change()
    }

    fun newCameraUri(): Uri = photos.newCameraUri()

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val path = photos.import(uri) ?: return@launch
            form.billPhotoPath?.let { replacedPhotos += it }
            form = form.copy(billPhotoPath = path)
        }
    }

    fun removePhoto() {
        form.billPhotoPath?.let { replacedPhotos += it }
        form = form.copy(billPhotoPath = null)
    }

    fun save(onDone: () -> Unit) {
        val f = form
        if (f.amountError) {
            form = f.copy(showErrors = true)
            return
        }
        val expense = (original ?: Expense(projectId = projectId, category = f.category, amount = 0, date = f.date)).copy(
            category = f.category,
            amount = Money.parse(f.amount) ?: 0,
            stageId = f.stageId,
            date = f.date,
            note = f.note.trim(),
            billPhotoPath = f.billPhotoPath,
        )
        viewModelScope.launch {
            expenses.save(expense)
            photos.delete(replacedPhotos.filter { it != expense.billPhotoPath })
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val e = original ?: return
        viewModelScope.launch {
            expenses.delete(e)
            photos.delete(listOfNotNull(e.billPhotoPath, form.billPhotoPath) + replacedPhotos)
            onDone()
        }
    }
}

@Composable
fun ExpenseEditScreen(
    onBack: () -> Unit,
    viewModel: ExpenseEditViewModel = hiltViewModel(),
) {
    val f = viewModel.form
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val save = { viewModel.save(onBack) }
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(if (viewModel.isNew) R.string.expense_add else R.string.expense_edit),
                onBack = onBack,
                actions = {
                    if (!viewModel.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                    IconButton(onClick = save) { Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_save)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.category))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExpenseCategory.entries.forEach { c ->
                    FilterChip(
                        selected = f.category == c,
                        onClick = { viewModel.update { copy(category = c) } },
                        label = { Text(stringResource(c.labelRes)) },
                    )
                }
            }
            MoneyField(
                value = f.amount,
                onValueChange = { v -> viewModel.update { copy(amount = v) } },
                label = stringResource(R.string.amount),
                isError = f.showErrors && f.amountError,
                supportingText = if (f.showErrors && f.amountError) stringResource(R.string.error_amount) else null,
            )
            DropdownField(
                label = stringResource(R.string.stage_type),
                options = listOf<Stage?>(null) + viewModel.stages,
                selected = viewModel.stages.firstOrNull { it.id == f.stageId },
                optionLabel = { it?.displayName() ?: stringResource(R.string.none) },
                onSelect = { v -> viewModel.update { copy(stageId = v?.id) } },
            )
            DateField(
                label = stringResource(R.string.date),
                value = f.date,
                onValueChange = { v -> if (v != null) viewModel.update { copy(date = v) } },
            )
            FormTextField(
                value = f.note,
                onValueChange = { v -> viewModel.update { copy(note = v) } },
                label = stringResource(R.string.expense_detail),
                singleLine = false,
            )
            BillPhotoField(
                path = f.billPhotoPath,
                onPicked = viewModel::onPhotoPicked,
                onRemove = viewModel::removePhoto,
                newCameraUri = viewModel::newCameraUri,
            )
            Button(onClick = save, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.action_save)) }
        }
    }
    if (confirmDelete) {
        ConfirmDeleteDialog(
            message = stringResource(R.string.entry_delete_message),
            onConfirm = { viewModel.delete(onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
}
