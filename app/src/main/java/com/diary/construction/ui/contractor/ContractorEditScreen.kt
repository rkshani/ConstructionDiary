package com.diary.construction.ui.contractor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import com.diary.construction.core.util.Quantity
import com.diary.construction.domain.model.AgreementType
import com.diary.construction.domain.model.Contractor
import com.diary.construction.domain.repository.ContractorRepository
import com.diary.construction.domain.repository.ProjectRepository
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.ChipSelector
import com.diary.construction.ui.components.ConfirmDeleteDialog
import com.diary.construction.ui.components.FormTextField
import com.diary.construction.ui.components.LabelValue
import com.diary.construction.ui.components.MoneyField
import com.diary.construction.ui.components.NumberField
import com.diary.construction.ui.components.PhoneField
import com.diary.construction.ui.components.labelRes
import com.diary.construction.ui.components.money
import com.diary.construction.ui.components.roleString
import com.diary.construction.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContractorForm(
    val name: String = "",
    val phone: String = "",
    val work: String = "",
    val type: AgreementType = AgreementType.FIXED,
    /** FIXED: total amount. PER_SQ_FT: rate per sq ft. */
    val amount: String = "",
    val areaSqFt: String = "",
    val workDone: Int = 0,
    val note: String = "",
    val showErrors: Boolean = false,
) {
    val nameError get() = name.isBlank()
    val amountError get() = (Money.parse(amount) ?: 0) <= 0

    val agreedPreview: Long
        get() = when (type) {
            AgreementType.FIXED -> Money.parse(amount) ?: 0
            AgreementType.PER_SQ_FT -> Money.multiply(Money.parse(amount) ?: 0, Quantity.parse(areaSqFt) ?: 0)
        }
}

@HiltViewModel
class ContractorEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val contractors: ContractorRepository,
    private val projects: ProjectRepository,
) : ViewModel() {
    private val projectId: Long = savedState.get<Long>(Routes.ARG_PROJECT_ID) ?: 0L
    private val id: Long = savedState.get<Long>(Routes.ARG_ID) ?: 0L
    val isNew = id == 0L
    private var original: Contractor? = null

    var form by mutableStateOf(ContractorForm())
        private set

    init {
        viewModelScope.launch {
            if (isNew) {
                // Use the plot area as a starting point for per sq ft deals.
                projects.getProject(projectId)?.let { form = form.copy(areaSqFt = Quantity.toInput(it.areaSqFt)) }
            } else {
                contractors.getContractor(id)?.let { c ->
                    original = c
                    form = ContractorForm(
                        name = c.name,
                        phone = c.phone,
                        work = c.work,
                        type = c.agreementType,
                        amount = Money.toInput(c.rate),
                        areaSqFt = Quantity.toInput(c.areaSqFt),
                        workDone = c.workDonePercent,
                        note = c.note,
                    )
                }
            }
        }
    }

    fun update(change: ContractorForm.() -> ContractorForm) {
        form = form.change()
    }

    fun save(onDone: (Long) -> Unit) {
        val f = form
        if (f.nameError || f.amountError) {
            form = f.copy(showErrors = true)
            return
        }
        val c = (original ?: Contractor(projectId = projectId, name = "", rate = 0)).copy(
            name = f.name.trim(),
            phone = f.phone.trim(),
            work = f.work.trim(),
            agreementType = f.type,
            rate = Money.parse(f.amount) ?: 0,
            areaSqFt = if (f.type == AgreementType.PER_SQ_FT) Quantity.parse(f.areaSqFt) ?: 0 else 0,
            workDonePercent = f.workDone,
            note = f.note.trim(),
        )
        viewModelScope.launch { onDone(contractors.save(c)) }
    }

    fun delete(onDone: () -> Unit) {
        val c = original ?: return
        viewModelScope.launch {
            contractors.delete(c)
            onDone()
        }
    }
}

@Composable
fun ContractorEditScreen(
    onBack: () -> Unit,
    onSaved: (id: Long, isNew: Boolean) -> Unit,
    onDeleted: () -> Unit,
    viewModel: ContractorEditViewModel = hiltViewModel(),
) {
    val f = viewModel.form
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val save = { viewModel.save { onSaved(it, viewModel.isNew) } }
    Scaffold(
        topBar = {
            AppTopBar(
                title = if (viewModel.isNew) {
                    roleString(R.string.contractor_add_owner, R.string.contractor_add_thekedar)
                } else {
                    stringResource(R.string.contractor_edit)
                },
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
            FormTextField(
                value = f.work,
                onValueChange = { v -> viewModel.update { copy(work = v) } },
                label = stringResource(R.string.contractor_work),
            )
            Text(stringResource(R.string.agreement))
            ChipSelector(
                options = AgreementType.entries,
                selected = f.type,
                optionLabel = { stringResource(it.labelRes) },
                onSelect = { v -> viewModel.update { copy(type = v) } },
            )
            if (f.type == AgreementType.FIXED) {
                MoneyField(
                    value = f.amount,
                    onValueChange = { v -> viewModel.update { copy(amount = v) } },
                    label = stringResource(R.string.agreed_amount),
                    isError = f.showErrors && f.amountError,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoneyField(
                        value = f.amount,
                        onValueChange = { v -> viewModel.update { copy(amount = v) } },
                        label = stringResource(R.string.rate_per_sqft),
                        isError = f.showErrors && f.amountError,
                        modifier = Modifier.weight(1f),
                    )
                    NumberField(
                        value = f.areaSqFt,
                        onValueChange = { v -> viewModel.update { copy(areaSqFt = v) } },
                        label = stringResource(R.string.covered_area),
                        suffix = stringResource(R.string.mu_sq_ft),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            LabelValue(stringResource(R.string.agreed_amount), money(f.agreedPreview), bold = true)
            Text(stringResource(R.string.work_done_value, f.workDone), style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = f.workDone.toFloat(),
                onValueChange = { v -> viewModel.update { copy(workDone = v.toInt()) } },
                valueRange = 0f..100f,
                steps = 19,
            )
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
            message = stringResource(R.string.contractor_delete_message),
            onConfirm = { viewModel.delete(onDeleted) },
            onDismiss = { confirmDelete = false },
        )
    }
}
