package com.diary.construction.ui.material

import android.net.Uri
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
import com.diary.construction.data.photo.PhotoStorage
import com.diary.construction.domain.model.MaterialEntry
import com.diary.construction.domain.model.MaterialEntryType
import com.diary.construction.domain.model.MaterialType
import com.diary.construction.domain.model.Stage
import com.diary.construction.domain.model.Supplier
import com.diary.construction.domain.repository.MaterialRepository
import com.diary.construction.domain.repository.StageRepository
import com.diary.construction.domain.repository.SupplierRepository
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.BillPhotoField
import com.diary.construction.ui.components.ChipSelector
import com.diary.construction.ui.components.ConfirmDeleteDialog
import com.diary.construction.ui.components.DateField
import com.diary.construction.ui.components.DropdownField
import com.diary.construction.ui.components.FormTextField
import com.diary.construction.ui.components.MoneyField
import com.diary.construction.ui.components.NumberField
import com.diary.construction.ui.components.displayName
import com.diary.construction.ui.components.labelRes
import com.diary.construction.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MaterialEntryForm(
    val entryType: MaterialEntryType = MaterialEntryType.RECEIVED,
    val typeId: Long? = null,
    val quantity: String = "",
    val rate: String = "",
    val total: String = "",
    /** True after the user typed a total by hand; then it is not auto-filled any more. */
    val totalEdited: Boolean = false,
    val supplierId: Long? = null,
    val stageId: Long? = null,
    val date: Long = Dates.today(),
    val billPhotoPath: String? = null,
    val note: String = "",
    val showErrors: Boolean = false,
) {
    val typeError get() = typeId == null
    val quantityError get() = (Quantity.parse(quantity) ?: 0) <= 0
}

@HiltViewModel
class MaterialEntryEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val material: MaterialRepository,
    private val supplierRepo: SupplierRepository,
    private val stageRepo: StageRepository,
    private val photos: PhotoStorage,
) : ViewModel() {
    private val projectId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_PROJECT_ID))
    private val id: Long = savedState.get<Long>(Routes.ARG_ID) ?: 0L
    val isNew = id == 0L
    private var original: MaterialEntry? = null
    /** Photos to delete from disk once the entry is saved. */
    private val replacedPhotos = mutableListOf<String>()

    var form by mutableStateOf(MaterialEntryForm())
        private set
    var types by mutableStateOf<List<MaterialType>>(emptyList())
        private set
    var suppliers by mutableStateOf<List<Supplier>>(emptyList())
        private set
    var stages by mutableStateOf<List<Stage>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            types = material.getTypes()
            suppliers = supplierRepo.getSuppliers()
            stages = stageRepo.getStages(projectId)
            if (isNew) {
                form = form.copy(typeId = types.firstOrNull()?.id)
            } else {
                material.getEntry(id)?.let { e ->
                    original = e
                    form = MaterialEntryForm(
                        entryType = e.entryType,
                        typeId = e.materialTypeId,
                        quantity = Quantity.toInput(e.quantity),
                        rate = Money.toInput(e.rate),
                        total = Money.toInput(e.total),
                        totalEdited = e.total != Money.multiply(e.rate, e.quantity),
                        supplierId = e.supplierId,
                        stageId = e.stageId,
                        date = e.date,
                        billPhotoPath = e.billPhotoPath,
                        note = e.note,
                    )
                }
            }
        }
    }

    fun update(change: MaterialEntryForm.() -> MaterialEntryForm) {
        form = form.change()
    }

    /** Quantity or rate changed: fill the total unless the user set it. */
    fun updateAmounts(quantity: String = form.quantity, rate: String = form.rate) {
        val auto = Money.multiply(Money.parse(rate) ?: 0, Quantity.parse(quantity) ?: 0)
        form = form.copy(
            quantity = quantity,
            rate = rate,
            total = if (form.totalEdited) form.total else Money.toInput(auto),
        )
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
        if (f.typeError || f.quantityError) {
            form = f.copy(showErrors = true)
            return
        }
        val received = f.entryType == MaterialEntryType.RECEIVED
        val entry = (original ?: MaterialEntry(projectId = projectId, materialTypeId = f.typeId!!, quantity = 0, date = f.date)).copy(
            materialTypeId = f.typeId!!,
            entryType = f.entryType,
            quantity = Quantity.parse(f.quantity) ?: 0,
            rate = if (received) Money.parse(f.rate) ?: 0 else 0,
            total = if (received) Money.parse(f.total) ?: 0 else 0,
            supplierId = if (received) f.supplierId else null,
            stageId = f.stageId,
            date = f.date,
            billPhotoPath = f.billPhotoPath,
            note = f.note.trim(),
        )
        viewModelScope.launch {
            material.saveEntry(entry)
            photos.delete(replacedPhotos.filter { it != entry.billPhotoPath })
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val e = original ?: return
        viewModelScope.launch {
            material.deleteEntry(e)
            photos.delete(listOfNotNull(e.billPhotoPath, form.billPhotoPath) + replacedPhotos)
            onDone()
        }
    }
}

@Composable
fun MaterialEntryEditScreen(
    onBack: () -> Unit,
    viewModel: MaterialEntryEditViewModel = hiltViewModel(),
) {
    val f = viewModel.form
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val save = { viewModel.save(onBack) }
    val selectedType = viewModel.types.firstOrNull { it.id == f.typeId }
    val unitLabel = selectedType?.let { stringResource(it.unit.labelRes) }
    val received = f.entryType == MaterialEntryType.RECEIVED

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(if (viewModel.isNew) R.string.material_add else R.string.material_edit),
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
            ChipSelector(
                options = MaterialEntryType.entries,
                selected = f.entryType,
                optionLabel = { stringResource(it.labelRes) },
                onSelect = { v -> viewModel.update { copy(entryType = v) } },
            )
            Text(
                stringResource(if (received) R.string.material_received_hint else R.string.material_used_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (viewModel.types.isNotEmpty()) {
                DropdownField(
                    label = stringResource(R.string.material),
                    options = viewModel.types,
                    selected = selectedType ?: viewModel.types.first(),
                    optionLabel = { it.displayName() },
                    onSelect = { v -> viewModel.update { copy(typeId = v.id) } },
                )
            }
            NumberField(
                value = f.quantity,
                onValueChange = { viewModel.updateAmounts(quantity = it) },
                label = stringResource(R.string.quantity),
                suffix = unitLabel,
                isError = f.showErrors && f.quantityError,
                supportingText = if (f.showErrors && f.quantityError) stringResource(R.string.error_quantity) else null,
            )
            if (received) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoneyField(
                        value = f.rate,
                        onValueChange = { viewModel.updateAmounts(rate = it) },
                        label = stringResource(R.string.rate_per_unit, unitLabel.orEmpty()),
                        modifier = Modifier.weight(1f),
                    )
                    MoneyField(
                        value = f.total,
                        onValueChange = { v -> viewModel.update { copy(total = v, totalEdited = v.isNotEmpty()) } },
                        label = stringResource(R.string.total),
                        modifier = Modifier.weight(1f),
                    )
                }
                DropdownField(
                    label = stringResource(R.string.supplier),
                    options = listOf<Supplier?>(null) + viewModel.suppliers,
                    selected = viewModel.suppliers.firstOrNull { it.id == f.supplierId },
                    optionLabel = { it?.name ?: stringResource(R.string.none_cash) },
                    onSelect = { v -> viewModel.update { copy(supplierId = v?.id) } },
                )
            }
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
            if (received) {
                BillPhotoField(
                    path = f.billPhotoPath,
                    onPicked = viewModel::onPhotoPicked,
                    onRemove = viewModel::removePhoto,
                    newCameraUri = viewModel::newCameraUri,
                )
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
            message = stringResource(R.string.entry_delete_message),
            onConfirm = { viewModel.delete(onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
}
