package com.diary.construction.ui.labour

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.diary.construction.R
import com.diary.construction.core.util.Dates
import com.diary.construction.domain.model.Attendance
import com.diary.construction.domain.model.AttendanceStatus
import com.diary.construction.domain.model.LabourPayment
import com.diary.construction.domain.model.LabourPaymentType
import com.diary.construction.domain.model.LabourSummary
import com.diary.construction.domain.repository.LabourRepository
import com.diary.construction.domain.usecase.ObserveLabourSummariesUseCase
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.ChipSelector
import com.diary.construction.ui.components.ConfirmDeleteDialog
import com.diary.construction.ui.components.ContactButtons
import com.diary.construction.ui.components.EmptyState
import com.diary.construction.ui.components.InfoCard
import com.diary.construction.ui.components.LabelValue
import com.diary.construction.ui.components.PaymentDialog
import com.diary.construction.ui.components.StatusChip
import com.diary.construction.ui.components.balanceColor
import com.diary.construction.ui.components.labelRes
import com.diary.construction.ui.components.money
import com.diary.construction.ui.navigation.Routes
import com.diary.construction.ui.theme.LocalExtraColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LabourDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val labourRepo: LabourRepository,
    observeSummaries: ObserveLabourSummariesUseCase,
) : ViewModel() {
    val labourId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_ID))

    val summary: StateFlow<LabourSummary?> = labourRepo.observeLabour(labourId).filterNotNull()
        .flatMapLatest { labour -> observeSummaries(labour.projectId).map { list -> list.firstOrNull { it.labour.id == labourId } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val payments: StateFlow<List<LabourPayment>> = labourRepo.observePayments(labourId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val attendance: StateFlow<List<Attendance>> = labourRepo.observeAttendanceForLabour(labourId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addPayment(amount: Long, date: Long, note: String, type: LabourPaymentType) {
        val labour = summary.value?.labour ?: return
        viewModelScope.launch {
            labourRepo.addPayment(LabourPayment(labourId = labour.id, projectId = labour.projectId, date = date, amount = amount, type = type, note = note))
        }
    }

    fun deletePayment(id: Long) {
        viewModelScope.launch { labourRepo.deletePayment(id) }
    }
}

@Composable
fun LabourDetailScreen(
    onBack: () -> Unit,
    onEdit: (projectId: Long, labourId: Long) -> Unit,
    viewModel: LabourDetailViewModel = hiltViewModel(),
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    var payType by rememberSaveable { mutableStateOf<LabourPaymentType?>(null) }
    var deleteId by remember { mutableStateOf<Long?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val s = summary

    Scaffold(
        topBar = {
            AppTopBar(
                title = s?.labour?.name.orEmpty(),
                subtitle = s?.let { stringResource(it.labour.role.labelRes) },
                onBack = onBack,
                actions = {
                    if (s != null) {
                        IconButton(onClick = { onEdit(s.labour.projectId, s.labour.id) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (s == null) return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                InfoCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    LabelValue(stringResource(R.string.daily_wage), money(s.labour.dailyWage))
                    LabelValue(stringResource(R.string.days_worked), stringResource(R.string.days_value, s.daysPresent, s.daysHalf))
                    LabelValue(stringResource(R.string.earned), money(s.earned))
                    LabelValue(stringResource(R.string.paid), money(s.paid))
                    LabelValue(
                        if (s.balance >= 0) stringResource(R.string.balance_due) else stringResource(R.string.advance_extra),
                        money(kotlin.math.abs(s.balance)),
                        bold = true,
                        valueColor = balanceColor(s.balance),
                    )
                    ContactButtons(
                        phone = s.labour.phone,
                        message = stringResource(R.string.khata_whatsapp_message, s.labour.name, money(s.balance)),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { payType = LabourPaymentType.ADVANCE }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.give_advance))
                    }
                    Button(onClick = { payType = LabourPaymentType.PAYMENT }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.payment_add))
                    }
                }
            }
            item {
                PrimaryTabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.payment_history)) })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.labour_tab_attendance)) })
                }
            }
            if (tab == 0) {
                if (payments.isEmpty()) item { EmptyState(title = stringResource(R.string.no_payments), modifier = Modifier.padding(top = 16.dp)) }
                items(payments, key = { it.id }) { p ->
                    InfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(money(p.amount), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    listOf(Dates.format(p.date), p.note).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            StatusChip(stringResource(p.type.labelRes))
                            IconButton(onClick = { deleteId = p.id }) {
                                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete))
                            }
                        }
                    }
                }
            } else {
                if (attendance.isEmpty()) item { EmptyState(title = stringResource(R.string.no_attendance), modifier = Modifier.padding(top = 16.dp)) }
                items(attendance, key = { it.id }) { a ->
                    val extra = LocalExtraColors.current
                    InfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(Dates.formatDay(a.date), modifier = Modifier.weight(1f))
                            StatusChip(
                                stringResource(a.status.labelRes),
                                color = when (a.status) {
                                    AttendanceStatus.PRESENT -> extra.positive
                                    AttendanceStatus.HALF_DAY -> extra.warning
                                    AttendanceStatus.ABSENT -> extra.danger
                                },
                            )
                            Text("  " + money(a.earned), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    payType?.let { initialType ->
        var type by rememberSaveable { mutableStateOf(initialType) }
        PaymentDialog(
            title = stringResource(R.string.payment_to, s?.labour?.name.orEmpty()),
            initialAmount = if (initialType == LabourPaymentType.PAYMENT) (s?.balance ?: 0).coerceAtLeast(0) else 0,
            onSave = { viewModel.addPayment(it.amount, it.date, it.note, type) },
            onDismiss = { payType = null },
            extra = {
                ChipSelector(
                    options = LabourPaymentType.entries,
                    selected = type,
                    optionLabel = { stringResource(it.labelRes) },
                    onSelect = { type = it },
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
