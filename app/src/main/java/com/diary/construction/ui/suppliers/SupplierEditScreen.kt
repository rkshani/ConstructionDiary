package com.diary.construction.ui.suppliers

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
import com.diary.construction.domain.model.Supplier
import com.diary.construction.domain.repository.SupplierRepository
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.ConfirmDeleteDialog
import com.diary.construction.ui.components.DateField
import com.diary.construction.ui.components.FormTextField
import com.diary.construction.ui.components.PhoneField
import com.diary.construction.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierForm(
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val materials: String = "",
    val paymentDueDate: Long? = null,
    val note: String = "",
    val showErrors: Boolean = false,
)

@HiltViewModel
class SupplierEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val suppliers: SupplierRepository,
) : ViewModel() {
    private val id: Long = savedState.get<Long>(Routes.ARG_ID) ?: 0L
    val isNew = id == 0L
    private var original: Supplier? = null

    var form by mutableStateOf(SupplierForm())
        private set

    init {
        if (!isNew) {
            viewModelScope.launch {
                suppliers.getSupplier(id)?.let { s ->
                    original = s
                    form = SupplierForm(s.name, s.phone, s.address, s.materials, s.paymentDueDate, s.note)
                }
            }
        }
    }

    fun update(change: SupplierForm.() -> SupplierForm) {
        form = form.change()
    }

    fun save(onDone: (Long) -> Unit) {
        val f = form
        if (f.name.isBlank()) {
            form = f.copy(showErrors = true)
            return
        }
        val supplier = (original ?: Supplier(name = "")).copy(
            name = f.name.trim(),
            phone = f.phone.trim(),
            address = f.address.trim(),
            materials = f.materials.trim(),
            paymentDueDate = f.paymentDueDate,
            note = f.note.trim(),
        )
        viewModelScope.launch { onDone(suppliers.save(supplier)) }
    }

    fun delete(onDone: () -> Unit) {
        val s = original ?: return
        viewModelScope.launch {
            suppliers.delete(s)
            onDone()
        }
    }
}

@Composable
fun SupplierEditScreen(
    onBack: () -> Unit,
    onSaved: (id: Long, isNew: Boolean) -> Unit,
    onDeleted: () -> Unit,
    viewModel: SupplierEditViewModel = hiltViewModel(),
) {
    val f = viewModel.form
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val save = { viewModel.save { onSaved(it, viewModel.isNew) } }
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(if (viewModel.isNew) R.string.supplier_add else R.string.supplier_edit),
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
            val nameError = f.showErrors && f.name.isBlank()
            FormTextField(
                value = f.name,
                onValueChange = { v -> viewModel.update { copy(name = v) } },
                label = stringResource(R.string.supplier_name),
                isError = nameError,
                supportingText = if (nameError) stringResource(R.string.error_required) else null,
            )
            PhoneField(value = f.phone, onValueChange = { v -> viewModel.update { copy(phone = v) } }, label = stringResource(R.string.phone))
            FormTextField(
                value = f.materials,
                onValueChange = { v -> viewModel.update { copy(materials = v) } },
                label = stringResource(R.string.supplier_materials),
            )
            FormTextField(
                value = f.address,
                onValueChange = { v -> viewModel.update { copy(address = v) } },
                label = stringResource(R.string.address),
                singleLine = false,
            )
            DateField(
                label = stringResource(R.string.supplier_due_date),
                value = f.paymentDueDate,
                onValueChange = { v -> viewModel.update { copy(paymentDueDate = v) } },
                allowClear = true,
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
            message = stringResource(R.string.supplier_delete_message),
            onConfirm = { viewModel.delete(onDeleted) },
            onDismiss = { confirmDelete = false },
        )
    }
}
