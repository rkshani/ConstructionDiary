package com.diary.construction.ui.projects

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
import com.diary.construction.core.util.Quantity
import com.diary.construction.domain.model.AreaUnit
import com.diary.construction.domain.model.Project
import com.diary.construction.domain.model.ProjectStatus
import com.diary.construction.domain.repository.ProjectRepository
import com.diary.construction.domain.repository.SettingsRepository
import com.diary.construction.domain.usecase.DeleteProjectUseCase
import com.diary.construction.domain.usecase.SaveProjectUseCase
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.ChipSelector
import com.diary.construction.ui.components.ConfirmDeleteDialog
import com.diary.construction.ui.components.DateField
import com.diary.construction.ui.components.DropdownField
import com.diary.construction.ui.components.FormTextField
import com.diary.construction.ui.components.MoneyField
import com.diary.construction.ui.components.NumberField
import com.diary.construction.ui.components.PhoneField
import com.diary.construction.ui.components.labelRes
import com.diary.construction.ui.components.roleString
import com.diary.construction.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectForm(
    val name: String = "",
    val siteAddress: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val startDate: Long = Dates.today(),
    val budget: String = "",
    val area: String = "",
    val areaUnit: AreaUnit = AreaUnit.MARLA,
    val status: ProjectStatus = ProjectStatus.IN_PROGRESS,
    val notes: String = "",
    val showErrors: Boolean = false,
) {
    val nameError get() = name.isBlank()
}

@HiltViewModel
class ProjectEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val projects: ProjectRepository,
    private val saveProject: SaveProjectUseCase,
    private val deleteProject: DeleteProjectUseCase,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val id: Long = savedState.get<Long>(Routes.ARG_ID) ?: 0L
    val isNew = id == 0L
    private var original: Project? = null

    var form by mutableStateOf(ProjectForm())
        private set

    init {
        viewModelScope.launch {
            if (isNew) {
                form = form.copy(areaUnit = settings.current().areaUnit)
            } else {
                projects.getProject(id)?.let { p ->
                    original = p
                    form = ProjectForm(
                        name = p.name,
                        siteAddress = p.siteAddress,
                        clientName = p.clientName,
                        clientPhone = p.clientPhone,
                        startDate = p.startDate,
                        budget = Money.toInput(p.budget),
                        area = Quantity.toInput(p.area),
                        areaUnit = p.areaUnit,
                        status = p.status,
                        notes = p.notes,
                    )
                }
            }
        }
    }

    fun update(change: ProjectForm.() -> ProjectForm) {
        form = form.change()
    }

    fun save(onSaved: (Long) -> Unit) {
        val f = form
        if (f.nameError) {
            form = f.copy(showErrors = true)
            return
        }
        val base = original ?: Project(name = "", startDate = f.startDate)
        val project = base.copy(
            name = f.name.trim(),
            siteAddress = f.siteAddress.trim(),
            clientName = f.clientName.trim(),
            clientPhone = f.clientPhone.trim(),
            startDate = f.startDate,
            budget = Money.parse(f.budget) ?: 0,
            area = Quantity.parse(f.area) ?: 0,
            areaUnit = f.areaUnit,
            status = f.status,
            notes = f.notes.trim(),
        )
        viewModelScope.launch { onSaved(saveProject(project)) }
    }

    fun delete(onDeleted: () -> Unit) {
        val p = original ?: return
        viewModelScope.launch {
            deleteProject(p)
            onDeleted()
        }
    }
}

@Composable
fun ProjectEditScreen(
    onBack: () -> Unit,
    onSaved: (id: Long, isNew: Boolean) -> Unit,
    onDeleted: () -> Unit,
    viewModel: ProjectEditViewModel = hiltViewModel(),
) {
    val f = viewModel.form
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val save = { viewModel.save { onSaved(it, viewModel.isNew) } }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(if (viewModel.isNew) R.string.project_new else R.string.project_edit),
                onBack = onBack,
                actions = {
                    if (!viewModel.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                    IconButton(onClick = save) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_save))
                    }
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
                label = stringResource(R.string.project_name),
                isError = f.showErrors && f.nameError,
                supportingText = if (f.showErrors && f.nameError) stringResource(R.string.error_required) else null,
            )
            FormTextField(
                value = f.siteAddress,
                onValueChange = { v -> viewModel.update { copy(siteAddress = v) } },
                label = stringResource(R.string.project_address),
                singleLine = false,
            )
            FormTextField(
                value = f.clientName,
                onValueChange = { v -> viewModel.update { copy(clientName = v) } },
                label = roleString(R.string.project_owner_name, R.string.project_client_name),
            )
            PhoneField(
                value = f.clientPhone,
                onValueChange = { v -> viewModel.update { copy(clientPhone = v) } },
                label = roleString(R.string.project_owner_phone, R.string.project_client_phone),
            )
            DateField(
                label = stringResource(R.string.project_start_date),
                value = f.startDate,
                onValueChange = { v -> if (v != null) viewModel.update { copy(startDate = v) } },
            )
            MoneyField(
                value = f.budget,
                onValueChange = { v -> viewModel.update { copy(budget = v) } },
                label = roleString(R.string.budget_owner, R.string.budget_thekedar),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberField(
                    value = f.area,
                    onValueChange = { v -> viewModel.update { copy(area = v) } },
                    label = stringResource(R.string.project_area),
                    modifier = Modifier.weight(1f),
                )
                DropdownField(
                    label = stringResource(R.string.unit),
                    options = AreaUnit.entries,
                    selected = f.areaUnit,
                    optionLabel = { stringResource(it.labelRes) },
                    onSelect = { v -> viewModel.update { copy(areaUnit = v) } },
                    modifier = Modifier.weight(1f),
                )
            }
            Text(stringResource(R.string.project_status))
            ChipSelector(
                options = ProjectStatus.entries,
                selected = f.status,
                optionLabel = { stringResource(it.labelRes) },
                onSelect = { v -> viewModel.update { copy(status = v) } },
            )
            FormTextField(
                value = f.notes,
                onValueChange = { v -> viewModel.update { copy(notes = v) } },
                label = stringResource(R.string.notes),
                singleLine = false,
                minLines = 2,
            )
            Button(onClick = save, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.action_save)) }
        }
    }

    if (confirmDelete) {
        ConfirmDeleteDialog(
            message = stringResource(R.string.project_delete_message),
            onConfirm = { viewModel.delete(onDeleted) },
            onDismiss = { confirmDelete = false },
        )
    }
}
