package com.diary.construction.ui.labour

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.diary.construction.core.util.Money
import com.diary.construction.domain.model.Labour
import com.diary.construction.domain.model.LabourRole
import com.diary.construction.domain.repository.LabourRepository
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.ConfirmDeleteDialog
import com.diary.construction.ui.components.DropdownField
import com.diary.construction.ui.components.FormTextField
import com.diary.construction.ui.components.MoneyField
import com.diary.construction.ui.components.PhoneField
import com.diary.construction.ui.components.labelRes
import com.diary.construction.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LabourForm(
    val name: String = "",
    val phone: String = "",
    val role: LabourRole = LabourRole.LABOUR,
    val wage: String = "",
    val active: Boolean = true,
    val note: String = "",
    val showErrors: Boolean = false,
) {
    val nameError get() = name.isBlank()
    val wageError get() = (Money.parse(wage) ?: 0) <= 0
}

@HiltViewModel
class LabourEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val labourRepo: LabourRepository,
) : ViewModel() {
    private val projectId: Long = savedState.get<Long>(Routes.ARG_PROJECT_ID) ?: 0L
    private val id: Long = savedState.get<Long>(Routes.ARG_ID) ?: 0L
    val isNew = id == 0L
    private var original: Labour? = null

    var form by mutableStateOf(LabourForm())
        private set

    init {
        if (!isNew) {
            viewModelScope.launch {
                labourRepo.getLabour(id)?.let { l ->
                    original = l
                    form = LabourForm(l.name, l.phone, l.role, Money.toInput(l.dailyWage), l.active, l.note)
                }
            }
        }
    }

    fun update(change: LabourForm.() -> LabourForm) {
        form = form.change()
    }

    fun save(onDone: () -> Unit) {
        val f = form
        if (f.nameError || f.wageError) {
            form = f.copy(showErrors = true)
            return
        }
        val labour = (original ?: Labour(projectId = projectId, name = "", dailyWage = 0)).copy(
            name = f.name.trim(),
            phone = f.phone.trim(),
            role = f.role,
            dailyWage = Money.parse(f.wage) ?: 0,
            active = f.active,
            note = f.note.trim(),
        )
        viewModelScope.launch {
            labourRepo.save(labour)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val l = original ?: return
        viewModelScope.launch {
            labourRepo.delete(l)
            onDone()
        }
    }
}

@Composable
fun LabourEditScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: LabourEditViewModel = hiltViewModel(),
) {
    val f = viewModel.form
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val save = { viewModel.save(onBack) }
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(if (viewModel.isNew) R.string.labour_add else R.string.labour_edit),
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
            FormTextField(
                value = f.name,
                onValueChange = { v -> viewModel.update { copy(name = v) } },
                label = stringResource(R.string.labour_name),
                isError = f.showErrors && f.nameError,
                supportingText = if (f.showErrors && f.nameError) stringResource(R.string.error_required) else null,
            )
            PhoneField(value = f.phone, onValueChange = { v -> viewModel.update { copy(phone = v) } }, label = stringResource(R.string.phone))
            DropdownField(
                label = stringResource(R.string.labour_role),
                options = LabourRole.entries,
                selected = f.role,
                optionLabel = { stringResource(it.labelRes) },
                onSelect = { v -> viewModel.update { copy(role = v) } },
            )
            MoneyField(
                value = f.wage,
                onValueChange = { v -> viewModel.update { copy(wage = v) } },
                label = stringResource(R.string.daily_wage),
                isError = f.showErrors && f.wageError,
                supportingText = if (f.showErrors && f.wageError) stringResource(R.string.error_amount) else stringResource(R.string.daily_wage_hint),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.labour_active), modifier = Modifier.weight(1f))
                Switch(checked = f.active, onCheckedChange = { v -> viewModel.update { copy(active = v) } })
            }
            FormTextField(
                value = f.note,
                onValueChange = { v -> viewModel.update { copy(note = v) } },
                label = stringResource(R.string.notes),
                singleLine = false,
            )
            Button(onClick = save, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.action_save)) }
        }
    }
    if (confirmDelete) {
        ConfirmDeleteDialog(
            message = stringResource(R.string.labour_delete_message),
            onConfirm = { viewModel.delete(onDeleted) },
            onDismiss = { confirmDelete = false },
        )
    }
}
