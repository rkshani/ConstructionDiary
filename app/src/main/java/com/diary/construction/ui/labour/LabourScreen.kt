package com.diary.construction.ui.labour

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diary.construction.R
import com.diary.construction.core.util.Dates
import com.diary.construction.domain.model.AttendanceStatus
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.DatePickDialog
import com.diary.construction.ui.components.EmptyState
import com.diary.construction.ui.components.InfoCard
import com.diary.construction.ui.components.LabelValue
import com.diary.construction.ui.components.balanceColor
import com.diary.construction.ui.components.labelRes
import com.diary.construction.ui.components.money
import com.diary.construction.ui.theme.LocalExtraColors

@Composable
fun LabourScreen(
    onBack: () -> Unit,
    onAddLabour: (projectId: Long) -> Unit,
    onOpenLabour: (labourId: Long) -> Unit,
    viewModel: LabourViewModel = hiltViewModel(),
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.section_labour), onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddLabour(viewModel.projectId) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.labour_add)) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.labour_tab_attendance)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.labour_tab_list)) })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.labour_tab_weekly)) })
            }
            val list = summaries ?: return@Column
            if (list.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.labour_empty),
                    message = stringResource(R.string.labour_empty_message),
                    icon = Icons.Outlined.Groups,
                    actionLabel = stringResource(R.string.labour_add),
                    onAction = { onAddLabour(viewModel.projectId) },
                )
                return@Column
            }
            when (tab) {
                0 -> AttendanceTab(viewModel)
                1 -> LabourListTab(viewModel, onOpenLabour)
                else -> WeeklyTab(viewModel)
            }
        }
    }
}

@Composable
private fun AttendanceTab(viewModel: LabourViewModel) {
    val day by viewModel.day.collectAsStateWithLifecycle()
    val rows by viewModel.dayRows.collectAsStateWithLifecycle()
    var pickDate by rememberSaveable { mutableStateOf(false) }
    val dayTotal = rows.sumOf { r -> r.status?.let { r.labour.dailyWage * it.wagePercent / 100 } ?: 0L }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.moveDay(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous))
                }
                TextButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f)) {
                    Text(
                        if (day == Dates.today()) stringResource(R.string.today_with_date, Dates.formatDay(day)) else Dates.formatDay(day),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                IconButton(onClick = { viewModel.moveDay(1) }, enabled = day < Dates.today()) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next))
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.day_wages, money(dayTotal)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = viewModel::markAllPresent) {
                    Icon(Icons.Outlined.DoneAll, contentDescription = null)
                    Text("  " + stringResource(R.string.mark_all_present))
                }
            }
        }
        items(rows, key = { it.labour.id }) { row ->
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(row.labour.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            stringResource(row.labour.role.labelRes) + " · " + money(row.labour.dailyWage),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AttendanceStatus.entries.forEach { status ->
                        val color = when (status) {
                            AttendanceStatus.PRESENT -> LocalExtraColors.current.positive
                            AttendanceStatus.HALF_DAY -> LocalExtraColors.current.warning
                            AttendanceStatus.ABSENT -> LocalExtraColors.current.danger
                        }
                        FilterChip(
                            selected = row.status == status,
                            onClick = { viewModel.mark(row.labour, status, row.status) },
                            label = { Text(stringResource(status.labelRes)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.25f),
                            ),
                        )
                    }
                }
            }
        }
    }
    if (pickDate) DatePickDialog(initial = day, onPick = { viewModel.setDay(it); pickDate = false }, onDismiss = { pickDate = false })
}

@Composable
private fun LabourListTab(viewModel: LabourViewModel, onOpen: (Long) -> Unit) {
    val list by viewModel.summaries.collectAsStateWithLifecycle()
    val items = list.orEmpty()
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            val due = items.sumOf { it.balance.coerceAtLeast(0) }
            InfoCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                LabelValue(stringResource(R.string.labour_total_earned), money(items.sumOf { it.earned }))
                LabelValue(stringResource(R.string.paid), money(items.sumOf { it.paid }))
                LabelValue(stringResource(R.string.balance_due), money(due), bold = true, valueColor = balanceColor(due))
            }
        }
        items(items, key = { it.labour.id }) { s ->
            InfoCard(onClick = { onOpen(s.labour.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        s.labour.name + if (!s.labour.active) " (" + stringResource(R.string.inactive) + ")" else "",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(stringResource(s.labour.role.labelRes), style = MaterialTheme.typography.bodySmall)
                }
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
            }
        }
    }
}

@Composable
private fun WeeklyTab(viewModel: LabourViewModel) {
    val weekStart by viewModel.weekStart.collectAsStateWithLifecycle()
    val rows by viewModel.weekRows.collectAsStateWithLifecycle()
    var settling by rememberSaveable { mutableStateOf<Long?>(null) }
    val weekEnd = Dates.addDays(weekStart, 6)

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.moveWeek(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous))
                }
                Text(
                    "${Dates.formatShort(weekStart)} – ${Dates.format(weekEnd)}",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.moveWeek(1) }, enabled = weekEnd < Dates.today()) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next))
                }
            }
        }
        item {
            InfoCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                LabelValue(stringResource(R.string.week_wages), money(rows.sumOf { it.earnedThisWeek }), bold = true)
                LabelValue(stringResource(R.string.week_paid), money(rows.sumOf { it.paidThisWeek }))
                val due = rows.sumOf { it.summary.balance.coerceAtLeast(0) }
                LabelValue(stringResource(R.string.balance_due), money(due), valueColor = balanceColor(due), bold = true)
            }
        }
        items(rows, key = { it.summary.labour.id }) { row ->
            val s = row.summary
            InfoCard {
                Text(s.labour.name, style = MaterialTheme.typography.titleSmall)
                LabelValue(stringResource(R.string.week_days), formatDays(row.days))
                LabelValue(stringResource(R.string.week_wages), money(row.earnedThisWeek))
                LabelValue(stringResource(R.string.week_paid), money(row.paidThisWeek))
                LabelValue(stringResource(R.string.balance_due), money(s.balance), bold = true, valueColor = balanceColor(s.balance))
                if (s.balance > 0) {
                    Row {
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { settling = s.labour.id }) { Text(stringResource(R.string.settle_pay, money(s.balance))) }
                    }
                }
            }
        }
    }
    settling?.let { id ->
        val row = rows.firstOrNull { it.summary.labour.id == id }
        if (row != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { settling = null },
                title = { Text(stringResource(R.string.settle_title)) },
                text = { Text(stringResource(R.string.settle_message, money(row.summary.balance), row.summary.labour.name)) },
                confirmButton = {
                    TextButton(onClick = { viewModel.settle(row); settling = null }) { Text(stringResource(R.string.action_ok)) }
                },
                dismissButton = { TextButton(onClick = { settling = null }) { Text(stringResource(R.string.action_cancel)) } },
            )
        }
    }
}

private fun formatDays(days: Double): String =
    if (days % 1.0 == 0.0) days.toInt().toString() else String.format(java.util.Locale.US, "%.1f", days)
