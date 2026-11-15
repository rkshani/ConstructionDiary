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
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.diary.construction.domain.model.ContractorSummary
import com.diary.construction.domain.usecase.ObserveContractorSummariesUseCase
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.ContactIcons
import com.diary.construction.ui.components.EmptyState
import com.diary.construction.ui.components.InfoCard
import com.diary.construction.ui.components.LabelValue
import com.diary.construction.ui.components.ProgressBar
import com.diary.construction.ui.components.balanceColor
import com.diary.construction.ui.components.money
import com.diary.construction.ui.components.roleString
import com.diary.construction.ui.navigation.Routes
import com.diary.construction.ui.theme.LocalExtraColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ContractorsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    observe: ObserveContractorSummariesUseCase,
) : ViewModel() {
    val projectId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_PROJECT_ID))
    val items: StateFlow<List<ContractorSummary>?> = observe(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ContractorsScreen(
    onBack: () -> Unit,
    onAdd: (projectId: Long) -> Unit,
    onOpen: (contractorId: Long) -> Unit,
    viewModel: ContractorsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val addLabel = roleString(R.string.contractor_add_owner, R.string.contractor_add_thekedar)
    Scaffold(
        topBar = { AppTopBar(roleString(R.string.contractor_khata_owner, R.string.contractor_khata_thekedar), onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAdd(viewModel.projectId) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(addLabel) },
            )
        },
    ) { padding ->
        val list = items ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.contractor_empty),
                message = roleString(R.string.contractor_empty_message_owner, R.string.contractor_empty_message_thekedar),
                icon = Icons.Outlined.Engineering,
                actionLabel = addLabel,
                onAction = { onAdd(viewModel.projectId) },
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(list, key = { it.contractor.id }) { s -> ContractorCard(s, onClick = { onOpen(s.contractor.id) }) }
        }
    }
}

@Composable
internal fun ContractorCard(s: ContractorSummary, onClick: (() -> Unit)? = null, showContact: Boolean = true) {
    val c = s.contractor
    InfoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (c.work.isNotBlank()) {
                    Text(c.work, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (showContact) ContactIcons(c.phone)
        }
        LabelValue(stringResource(R.string.agreed_amount), money(s.agreed))
        LabelValue(stringResource(R.string.paid), money(s.paid))
        LabelValue(stringResource(R.string.work_done), "${c.workDonePercent}%  (${money(c.workDoneValue)})")
        ProgressBar(c.workDonePercent / 100f, color = LocalExtraColors.current.positive)
        LabelValue(stringResource(R.string.due_now), money(s.dueNow), bold = true, valueColor = balanceColor(s.dueNow))
        LabelValue(stringResource(R.string.remaining_on_contract), money(s.remaining))
        if (s.paid > c.workDoneValue && c.workDonePercent < 100) {
            Text(
                stringResource(R.string.paid_ahead, money(s.paid - c.workDoneValue)),
                style = MaterialTheme.typography.bodySmall,
                color = LocalExtraColors.current.warning,
            )
        }
    }
}
