package com.diary.construction.ui.contractor

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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.diary.construction.domain.model.ContractorPayment
import com.diary.construction.domain.model.ContractorSummary
import com.diary.construction.domain.repository.ContractorRepository
import com.diary.construction.domain.usecase.ObserveContractorSummariesUseCase
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.ConfirmDeleteDialog
import com.diary.construction.ui.components.ContactButtons
import com.diary.construction.ui.components.EmptyState
import com.diary.construction.ui.components.InfoCard
import com.diary.construction.ui.components.PaymentDialog
import com.diary.construction.ui.components.SectionHeader
import com.diary.construction.ui.components.money
import com.diary.construction.ui.navigation.Routes
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
class ContractorDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val contractors: ContractorRepository,
    observeSummaries: ObserveContractorSummariesUseCase,
) : ViewModel() {
    val contractorId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_ID))

    val summary: StateFlow<ContractorSummary?> = contractors.observeContractor(contractorId).filterNotNull()
        .flatMapLatest { c -> observeSummaries(c.projectId).map { list -> list.firstOrNull { it.contractor.id == contractorId } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val payments: StateFlow<List<ContractorPayment>> = contractors.observePayments(contractorId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setWorkDone(percent: Int) {
        val c = summary.value?.contractor ?: return
        viewModelScope.launch { contractors.save(c.copy(workDonePercent = percent.coerceIn(0, 100))) }
    }

    fun addPayment(amount: Long, date: Long, note: String) {
        val c = summary.value?.contractor ?: return
        viewModelScope.launch {
            contractors.addPayment(ContractorPayment(contractorId = c.id, projectId = c.projectId, date = date, amount = amount, note = note))
        }
    }

    fun deletePayment(id: Long) {
        viewModelScope.launch { contractors.deletePayment(id) }
    }
}

@Composable
fun ContractorDetailScreen(
    onBack: () -> Unit,
    onEdit: (projectId: Long, contractorId: Long) -> Unit,
    viewModel: ContractorDetailViewModel = hiltViewModel(),
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    var addPayment by rememberSaveable { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<Long?>(null) }
    val s = summary

    Scaffold(
        topBar = {
            AppTopBar(
                title = s?.contractor?.name.orEmpty(),
                subtitle = s?.contractor?.work,
                onBack = onBack,
                actions = {
                    if (s != null) {
                        IconButton(onClick = { onEdit(s.contractor.projectId, s.contractor.id) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
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
            item { ContractorCard(s, showContact = false) }
            item {
                ContactButtons(
                    phone = s.contractor.phone,
                    message = stringResource(R.string.khata_whatsapp_message, s.contractor.name, money(s.dueNow)),
                )
            }
            item {
                // Local slider value; saved when the finger lifts.
                var value by remember(s.contractor.workDonePercent) { mutableFloatStateOf(s.contractor.workDonePercent.toFloat()) }
                InfoCard {
                    Text(stringResource(R.string.work_done_value, value.toInt()), style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = value,
                        onValueChange = { value = it },
                        onValueChangeFinished = { viewModel.setWorkDone(value.toInt()) },
                        valueRange = 0f..100f,
                        steps = 19,
                    )
                }
            }
            item { SectionHeader(stringResource(R.string.payment_history)) }
            if (payments.isEmpty()) item { EmptyState(title = stringResource(R.string.no_payments), modifier = Modifier.padding(top = 8.dp)) }
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
                        IconButton(onClick = { deleteId = p.id }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            }
        }
    }

    if (addPayment && s != null) {
        PaymentDialog(
            title = stringResource(R.string.payment_to, s.contractor.name),
            initialAmount = s.dueNow,
            onSave = { viewModel.addPayment(it.amount, it.date, it.note) },
            onDismiss = { addPayment = false },
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
