package com.diary.construction.ui.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.diary.construction.R
import com.diary.construction.domain.model.MaterialKind
import com.diary.construction.domain.model.MaterialType
import com.diary.construction.domain.model.MaterialUnit
import com.diary.construction.domain.repository.MaterialRepository
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.ConfirmDeleteDialog
import com.diary.construction.ui.components.DropdownField
import com.diary.construction.ui.components.FormTextField
import com.diary.construction.ui.components.InfoCard
import com.diary.construction.ui.components.displayName
import com.diary.construction.ui.components.labelRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MaterialTypesViewModel @Inject constructor(
    private val material: MaterialRepository,
) : ViewModel() {
    val types: StateFlow<List<MaterialType>> = material.observeTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(type: MaterialType) {
        viewModelScope.launch { material.saveType(type) }
    }

    fun delete(type: MaterialType) {
        viewModelScope.launch { material.deleteType(type) }
    }
}

/** Built-in materials can change their unit; custom ones can also be renamed or deleted. */
@Composable
fun MaterialTypesScreen(
    onBack: () -> Unit,
    viewModel: MaterialTypesViewModel = hiltViewModel(),
) {
    val types by viewModel.types.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<MaterialType?>(null) }
    var deleting by remember { mutableStateOf<MaterialType?>(null) }

    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.material_types), onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = MaterialType(kind = MaterialKind.CUSTOM, unit = MaterialUnit.PIECE) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.material_type_add)) },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(types, key = { it.id }) { type ->
                InfoCard(onClick = { editing = type }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(type.displayName(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(stringResource(type.unit.labelRes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    editing?.let { type ->
        TypeDialog(
            type = type,
            onSave = { viewModel.save(it); editing = null },
            onDelete = if (!type.isBuiltIn && type.id != 0L) {
                { deleting = type; editing = null }
            } else {
                null
            },
            onDismiss = { editing = null },
        )
    }
    deleting?.let { type ->
        ConfirmDeleteDialog(
            message = stringResource(R.string.material_type_delete_message),
            onConfirm = { viewModel.delete(type) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun TypeDialog(
    type: MaterialType,
    onSave: (MaterialType) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(type.name) }
    var unit by remember { mutableStateOf(type.unit) }
    val custom = !type.isBuiltIn
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type.id == 0L) stringResource(R.string.material_type_add) else type.displayName()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FormTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(if (custom) R.string.material_name else R.string.stage_name_optional),
                )
                DropdownField(
                    label = stringResource(R.string.unit),
                    options = MaterialUnit.entries,
                    selected = unit,
                    optionLabel = { stringResource(it.labelRes) },
                    onSelect = { unit = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(type.copy(name = name.trim(), unit = unit)) },
                enabled = !custom || name.isNotBlank(),
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}
