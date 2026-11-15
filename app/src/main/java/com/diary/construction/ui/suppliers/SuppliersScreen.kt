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
import androidx.compose.material.icons.outlined.LocalShipping
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.diary.construction.R
import com.diary.construction.domain.model.SupplierBalance
import com.diary.construction.domain.usecase.ObserveSupplierBalancesUseCase
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.ContactIcons
import com.diary.construction.ui.components.EmptyState
import com.diary.construction.ui.components.InfoCard
import com.diary.construction.ui.components.LabelValue
import com.diary.construction.ui.components.balanceColor
import com.diary.construction.ui.components.money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SuppliersViewModel @Inject constructor(
    observeBalances: ObserveSupplierBalancesUseCase,
) : ViewModel() {
    val suppliers: StateFlow<List<SupplierBalance>?> = observeBalances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun SuppliersScreen(
    onBack: () -> Unit,
    onOpen: (supplierId: Long) -> Unit,
    onAdd: () -> Unit,
    viewModel: SuppliersViewModel = hiltViewModel(),
) {
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.suppliers_khata), onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.supplier_add)) },
            )
        },
    ) { padding ->
        val list = suppliers ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.suppliers_empty),
                message = stringResource(R.string.suppliers_empty_message),
                icon = Icons.Outlined.LocalShipping,
                actionLabel = stringResource(R.string.supplier_add),
                onAction = onAdd,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                val due = list.sumOf { it.balance.coerceAtLeast(0) }
                InfoCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    LabelValue(stringResource(R.string.suppliers_total_due), money(due), bold = true, valueColor = balanceColor(due))
                }
            }
            items(list, key = { it.supplier.id }) { item ->
                InfoCard(onClick = { onOpen(item.supplier.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.supplier.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (item.supplier.materials.isNotBlank()) {
                                Text(
                                    item.supplier.materials,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        ContactIcons(item.supplier.phone)
                    }
                    LabelValue(stringResource(R.string.supplier_taken), money(item.taken))
                    LabelValue(stringResource(R.string.paid), money(item.paid))
                    LabelValue(stringResource(R.string.balance_due), money(item.balance), bold = true, valueColor = balanceColor(item.balance))
                }
            }
        }
    }
}
