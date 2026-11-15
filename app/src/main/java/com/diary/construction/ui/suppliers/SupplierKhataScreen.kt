package com.diary.construction.ui.suppliers

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.diary.construction.R
import com.diary.construction.core.util.Dates
import com.diary.construction.core.util.Quantity
import com.diary.construction.domain.model.MaterialEntry
import com.diary.construction.domain.model.MaterialEntryType
import com.diary.construction.domain.model.MaterialType
import com.diary.construction.domain.model.Project
import com.diary.construction.domain.model.Supplier
import com.diary.construction.domain.model.SupplierPayment
import com.diary.construction.domain.repository.MaterialRepository
import com.diary.construction.domain.repository.ProjectRepository
import com.diary.construction.domain.repository.SupplierRepository
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.ConfirmDeleteDialog
import com.diary.construction.ui.components.ContactButtons
import com.diary.construction.ui.components.DropdownField
import com.diary.construction.ui.components.EmptyState
import com.diary.construction.ui.components.InfoCard
import com.diary.construction.ui.components.LabelValue
import com.diary.construction.ui.components.PaymentDialog
import com.diary.construction.ui.components.balanceColor
import com.diary.construction.ui.components.displayName
import com.diary.construction.ui.components.labelRes
import com.diary.construction.ui.components.money
import com.diary.construction.ui.navigation.Routes
import com.diary.construction.ui.theme.LocalExtraColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One khata line. Exactly one of [entry] / [payment] is set. */
data class KhataRow(
    val date: Long,
    val entry: MaterialEntry? = null,
    val type: MaterialType? = null,
    val payment: SupplierPayment? = null,
    val projectName: String? = null,
    /** Balance after this line. */
    val balance: Long,
)

data class KhataState(
    val supplier: Supplier,
    val rows: List<KhataRow>,
    val taken: Long,
    val paid: Long,
    val projects: List<Project>,
) {
    val balance: Long get() = taken - paid
}

@HiltViewModel
class SupplierKhataViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val suppliers: SupplierRepository,
    material: MaterialRepository,
    projects: ProjectRepository,
) : ViewModel() {
    val supplierId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_ID))
    /** Project we came from, used as the default for new payments (0 = none). */
    val fromProjectId: Long = savedState.get<Long>(Routes.ARG_PROJECT_ID) ?: 0L

    val state: StateFlow<KhataState?> = combine(
        suppliers.observeSupplier(supplierId).filterNotNull(),
        material.observeEntriesForSupplier(supplierId),
        suppliers.observePayments(supplierId),
        material.observeTypes(),
        projects.observeProjects(),
    ) { supplier, entries, payments, types, projectList ->
        val t = types.associateBy { it.id }
        val p = projectList.associateBy { it.id }
        val received = entries.filter { it.entryType == MaterialEntryType.RECEIVED }
        // Oldest first to build the running balance, newest first on screen.
        val lines = (
            received.map { Triple(it.date, it.id, it as Any) } +
                payments.map { Triple(it.date, -it.id, it as Any) }
            ).sortedWith(compareBy({ it.first }, { it.second }))
        var running = 0L
        val rows = lines.map { (date, _, item) ->
            when (item) {
                is MaterialEntry -> {
                    running += item.total
                    KhataRow(date, entry = item, type = t[item.materialTypeId], projectName = p[item.projectId]?.name, balance = running)
                }
                else -> {
                    val pay = item as SupplierPayment
                    running -= pay.amount
                    KhataRow(date, payment = pay, projectName = pay.projectId?.let { p[it]?.name }, balance = running)
                }
            }
        }.reversed()
        KhataState(supplier, rows, received.sumOf { it.total }, payments.sumOf { it.amount }, projectList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addPayment(amount: Long, date: Long, note: String, projectId: Long?) {
        viewModelScope.launch {
            suppliers.savePayment(SupplierPayment(supplierId = supplierId, projectId = projectId, date = date, amount = amount, note = note))
        }
    }

    fun deletePayment(id: Long) {
        viewModelScope.launch { suppliers.deletePayment(id) }
    }
}

@Composable
fun SupplierKhataScreen(
    onBack: () -> Unit,
    onEdit: (supplierId: Long) -> Unit,
    viewModel: SupplierKhataViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var addPayment by rememberSaveable { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<Long?>(null) }
    val s = state

    Scaffold(
        topBar = {
            AppTopBar(
                title = s?.supplier?.name.orEmpty(),
                subtitle = stringResource(R.string.supplier_khata),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { onEdit(viewModel.supplierId) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { addPayment = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.payment_add)) },
            )
        },
    ) { padding ->
        if (s == null) return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                InfoCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    LabelValue(stringResource(R.string.supplier_taken), money(s.taken))
                    LabelValue(stringResource(R.string.paid), money(s.paid))
                    LabelValue(stringResource(R.string.balance_due), money(s.balance), bold = true, valueColor = balanceColor(s.balance))
                    s.supplier.paymentDueDate?.let {
                        LabelValue(stringResource(R.string.supplier_due_date), Dates.format(it))
                    }
                    ContactButtons(
                        phone = s.supplier.phone,
                        message = stringResource(R.string.khata_whatsapp_message, s.supplier.name, money(s.balance)),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            if (s.rows.isEmpty()) {
                item { EmptyState(title = stringResource(R.string.khata_empty), modifier = Modifier.padding(top = 24.dp)) }
            }
            items(s.rows, key = { (it.entry?.id ?: -(it.payment?.id ?: 0)).toString() + "_" + (it.entry != null) }) { row ->
                KhataRowCard(row, onDeletePayment = { deleteId = it })
            }
        }
    }

    if (addPayment && s != null) {
        var projectId by rememberSaveable { mutableStateOf(viewModel.fromProjectId.takeIf { it != 0L }) }
        PaymentDialog(
            title = stringResource(R.string.payment_to, s.supplier.name),
            initialAmount = s.balance.coerceAtLeast(0),
            onSave = { viewModel.addPayment(it.amount, it.date, it.note, projectId) },
            onDismiss = { addPayment = false },
            extra = {
                DropdownField(
                    label = stringResource(R.string.project),
                    options = listOf<Project?>(null) + s.projects,
                    selected = s.projects.firstOrNull { it.id == projectId },
                    optionLabel = { it?.name ?: stringResource(R.string.none) },
                    onSelect = { projectId = it?.id },
                )
            },
        )
    }
    deleteId?.let { id ->
        ConfirmDeleteDialog(
            message = stringResource(R.string.payment_delete_message),
            onConfirm = { viewModel.deletePayment(id) },
            onDismiss = { deleteId = null },
        )
    }
}

@Composable
private fun KhataRowCard(row: KhataRow, onDeletePayment: (Long) -> Unit) {
    val extra = LocalExtraColors.current
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val entry = row.entry
                val payment = row.payment
                val title = when {
                    entry != null -> {
                        val unit = row.type?.let { stringResource(it.unit.labelRes) }.orEmpty()
                        "${row.type?.displayName().orEmpty()} · ${Quantity.format(entry.quantity)} $unit"
                    }
                    else -> stringResource(R.string.payment_given)
                }
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val detail = listOfNotNull(
                    Dates.format(row.date),
                    row.projectName,
                    (entry?.note ?: payment?.note)?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (row.entry != null) {
                    Text("+ " + money(row.entry.total), color = extra.danger, style = MaterialTheme.typography.titleSmall)
                } else if (row.payment != null) {
                    Text("- " + money(row.payment.amount), color = extra.positive, style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    stringResource(R.string.balance_short, money(row.balance)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            row.payment?.let { pay ->
                IconButton(onClick = { onDeletePayment(pay.id) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }
        }
    }
}
