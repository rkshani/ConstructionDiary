package com.diary.construction.ui.stages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.diary.construction.core.util.Money
import com.diary.construction.domain.model.Stage
import com.diary.construction.domain.model.StageStatus
import com.diary.construction.domain.model.StageType
import com.diary.construction.domain.repository.StageRepository
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.ChipSelector
import com.diary.construction.ui.components.ConfirmDeleteDialog
import com.diary.construction.ui.components.DateField
import com.diary.construction.ui.components.DropdownField
import com.diary.construction.ui.components.FormTextField
import com.diary.construction.ui.components.MoneyField
import com.diary.construction.ui.components.labelRes
import com.diary.construction.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StageForm(
    val type: StageType = StageType.CUSTOM,
    val name: String = "",
    val budget: String = "",
    val status: StageStatus = StageStatus.NOT_STARTED,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val notes: String = "",
    val showErrors: Boolean = false,
) {
    val nameError get() = type == StageType.CUSTOM && name.isBlank()
    val dateError get() = startDate != null && endDate != null && endDate < startDate
}

@HiltViewModel
class StageEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val stages: StageRepository,
) : ViewModel() {
    private val projectId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_PROJECT_ID))
    private val id: Long = savedState.get<Long>(Routes.ARG_ID) ?: 0L
    val isNew = id == 0L
    private var original: Stage? = null

    var form by mutableStateOf(StageForm())
        private set

    init {
        if (!isNew) {
            viewModelScope.launch {
                stages.getStage(id)?.let { s ->
                    original = s
                    form = StageForm(
                        type = s.type,
                        name = s.name,
                        budget = Money.toInput(s.budget),
                        status = s.status,
                        startDate = s.startDate,
                        endDate = s.endDate,
                        notes = s.notes,
                    )
                }
            }
        }
    }

    fun update(change: StageForm.() -> StageForm) {
        form = form.change()
    }

    fun save(onDone: () -> Unit) {
        val f = form
        if (f.nameError || f.dateError) {
            form = f.copy(showErrors = true)
            return
        }
        val stage = (original ?: Stage(projectId = projectId, type = f.type)).copy(
            type = f.type,
            name = f.name.trim(),
            budget = Money.parse(f.budget) ?: 0,
            status = f.status,
            startDate = f.startDate,
            endDate = f.endDate,
            notes = f.notes.trim(),
        )
        viewModelScope.launch {
            stages.save(stage)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val s = original ?: return
        viewModelScope.launch {
            stages.delete(s)
            onDone()
        }
    }
}

@Composable
fun StageEditScreen(
    onBack: () -> Unit,
    viewModel: StageEditViewModel = hiltViewModel(),
) {
    val f = viewModel.form
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val save = { viewModel.save(onBack) }
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(if (viewModel.isNew) R.string.stage_add else R.string.stage_edit),
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
            DropdownField(
                label = stringResource(R.string.stage_type),
                options = StageType.entries,
                selected = f.type,
                optionLabel = { stringResource(it.labelRes) },
                onSelect = { v -> viewModel.update { copy(type = v) } },
            )
            FormTextField(
                value = f.name,
                onValueChange = { v -> viewModel.update { copy(name = v) } },
                label = stringResource(if (f.type == StageType.CUSTOM) R.string.stage_name else R.string.stage_name_optional),
                isError = f.showErrors && f.nameError,
                supportingText = if (f.showErrors && f.nameError) stringResource(R.string.error_required) else null,
            )
            MoneyField(
                value = f.budget,
                onValueChange = { v -> viewModel.update { copy(budget = v) } },
                label = stringResource(R.string.stage_budget),
            )
            Text(stringResource(R.string.project_status))
            ChipSelector(
                options = StageStatus.entries,
                selected = f.status,
                optionLabel = { stringResource(it.labelRes) },
                onSelect = { v -> viewModel.update { copy(status = v) } },
            )
            DateField(
                label = stringResource(R.string.stage_start),
                value = f.startDate,
                onValueChange = { v -> viewModel.update { copy(startDate = v) } },
                allowClear = true,
            )
            DateField(
                label = stringResource(R.string.stage_end),
                value = f.endDate,
                onValueChange = { v -> viewModel.update { copy(endDate = v) } },
                allowClear = true,
            )
            if (f.showErrors && f.dateError) {
                Text(stringResource(R.string.error_end_before_start), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }
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
            message = stringResource(R.string.stage_delete_message),
            onConfirm = { viewModel.delete(onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
}
