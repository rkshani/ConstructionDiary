package com.diary.construction.ui.material

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.diary.construction.domain.model.MaterialEntryItem
import com.diary.construction.domain.model.MaterialEntryType
import com.diary.construction.domain.model.MaterialStock
import com.diary.construction.domain.model.MaterialType
import com.diary.construction.domain.repository.MaterialRepository
import com.diary.construction.domain.repository.StageRepository
import com.diary.construction.domain.repository.SupplierRepository
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.EmptyState
import com.diary.construction.ui.components.InfoCard
import com.diary.construction.ui.components.LabelValue
import com.diary.construction.ui.components.ProgressBar
import com.diary.construction.ui.components.StatusChip
import com.diary.construction.ui.components.displayName
import com.diary.construction.ui.components.labelRes
import com.diary.construction.ui.components.money
import com.diary.construction.ui.navigation.Routes
import com.diary.construction.ui.theme.LocalExtraColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StockRow(val type: MaterialType, val stock: MaterialStock)

@HiltViewModel
class MaterialViewModel @Inject constructor(
    savedState: SavedStateHandle,
    material: MaterialRepository,
    suppliers: SupplierRepository,
    stages: StageRepository,
) : ViewModel() {
    val projectId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_PROJECT_ID))

    val filter = MutableStateFlow<Long?>(null)

    val types: StateFlow<List<MaterialType>> = material.observeTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stock: StateFlow<List<StockRow>?> = combine(material.observeStock(projectId), material.observeTypes()) { stock, types ->
        val byId = types.associateBy { it.id }
        stock.mapNotNull { s -> byId[s.materialTypeId]?.let { StockRow(it, s) } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val entries: StateFlow<List<MaterialEntryItem>?> = combine(
        material.observeEntries(projectId),
        material.observeTypes(),
        suppliers.observeSuppliers(),
        stages.observeStages(projectId),
        filter,
    ) { entries, types, sup, stg, f ->
        val t = types.associateBy { it.id }
        val s = sup.associateBy { it.id }
        val st = stg.associateBy { it.id }
        entries.filter { f == null || it.materialTypeId == f }.mapNotNull { e ->
            val type = t[e.materialTypeId] ?: return@mapNotNull null
            MaterialEntryItem(e, type, e.supplierId?.let { s[it]?.name }, e.stageId?.let { st[it] })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun MaterialScreen(
    onBack: () -> Unit,
    onEditEntry: (projectId: Long, entryId: Long) -> Unit,
    onManageTypes: () -> Unit,
    viewModel: MaterialViewModel = hiltViewModel(),
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.section_material),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onManageTypes) {
                        Icon(Icons.Outlined.Category, contentDescription = stringResource(R.string.material_types))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEditEntry(viewModel.projectId, 0) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.material_add)) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.material_tab_stock)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.material_tab_entries)) })
            }
            if (tab == 0) StockTab(viewModel) else EntriesTab(viewModel, onEdit = { onEditEntry(viewModel.projectId, it) })
        }
    }
}

@Composable
private fun StockTab(viewModel: MaterialViewModel) {
    val rows by viewModel.stock.collectAsStateWithLifecycle()
    val list = rows ?: return
    if (list.isEmpty()) {
        EmptyState(title = stringResource(R.string.material_empty), message = stringResource(R.string.material_empty_message), icon = Icons.Outlined.Inventory2)
        return
    }
    val extra = LocalExtraColors.current
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(list, key = { it.type.id }) { row ->
            val unit = stringResource(row.type.unit.labelRes)
            InfoCard {
                Text(row.type.displayName(), style = MaterialTheme.typography.titleMedium)
                LabelValue(stringResource(R.string.stock_received), "${Quantity.format(row.stock.received)} $unit")
                LabelValue(stringResource(R.string.stock_used), "${Quantity.format(row.stock.used)} $unit")
                LabelValue(
                    stringResource(R.string.stock_remaining),
                    "${Quantity.format(row.stock.remaining)} $unit",
                    bold = true,
                    valueColor = if (row.stock.remaining < 0) extra.danger else extra.positive,
                )
                if (row.stock.received > 0) {
                    ProgressBar(row.stock.used.toFloat() / row.stock.received)
                }
                LabelValue(stringResource(R.string.stock_cost), money(row.stock.cost))
                if (row.stock.remaining < 0) {
                    Text(stringResource(R.string.stock_negative), color = extra.danger, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun EntriesTab(viewModel: MaterialViewModel, onEdit: (Long) -> Unit) {
    val items by viewModel.entries.collectAsStateWithLifecycle()
    val types by viewModel.types.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val list = items ?: return
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = filter == null, onClick = { viewModel.filter.value = null }, label = { Text(stringResource(R.string.filter_all)) })
            types.forEach { t ->
                FilterChip(selected = filter == t.id, onClick = { viewModel.filter.value = t.id }, label = { Text(t.displayName()) })
            }
        }
        if (list.isEmpty()) {
            EmptyState(title = stringResource(R.string.material_empty), icon = Icons.Outlined.Inventory2)
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(list, key = { it.entry.id }) { item -> EntryRow(item, onClick = { onEdit(item.entry.id) }) }
        }
    }
}

@Composable
private fun EntryRow(item: MaterialEntryItem, onClick: () -> Unit) {
    val e = item.entry
    val extra = LocalExtraColors.current
    val unit = stringResource(item.type.unit.labelRes)
    InfoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${item.type.displayName()} · ${Quantity.format(e.quantity)} $unit",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val details = listOfNotNull(
                    Dates.format(e.date),
                    item.supplierName,
                    item.stage?.displayName(),
                ).joinToString(" · ")
                Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(
                    stringResource(e.entryType.labelRes),
                    color = if (e.entryType == MaterialEntryType.RECEIVED) extra.positive else MaterialTheme.colorScheme.tertiary,
                )
                if (e.entryType == MaterialEntryType.RECEIVED) {
                    Text(money(e.total), style = MaterialTheme.typography.titleSmall)
                }
            }
            if (e.billPhotoPath != null) {
                Icon(
                    Icons.Outlined.Receipt,
                    contentDescription = stringResource(R.string.bill_photo),
                    modifier = Modifier.padding(start = 8.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
