package com.diary.construction.ui.expenses

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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diary.construction.R
import com.diary.construction.core.util.Dates
import com.diary.construction.domain.model.Expense
import com.diary.construction.domain.model.ExpenseCategory
import com.diary.construction.domain.model.Stage
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.EmptyState
import com.diary.construction.ui.components.InfoCard
import com.diary.construction.ui.components.LabelValue
import com.diary.construction.ui.components.ProjectPicker
import com.diary.construction.ui.components.displayName
import com.diary.construction.ui.components.labelRes
import com.diary.construction.ui.components.money

/**
 * Other expenses of one project. Used both as the Expenses tab (with a project picker)
 * and from a project dashboard.
 */
@Composable
fun ExpensesScreen(
    onBack: (() -> Unit)?,
    onEdit: (projectId: Long, expenseId: Long) -> Unit,
    onAddProject: () -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state
    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.section_expenses), onBack = onBack) },
        floatingActionButton = {
            val projectId = s?.project?.id
            if (projectId != null) {
                ExtendedFloatingActionButton(
                    onClick = { onEdit(projectId, 0) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.expense_add)) },
                )
            }
        },
    ) { padding ->
        if (s == null) return@Scaffold
        if (s.project == null) {
            EmptyState(
                title = stringResource(R.string.no_project_title),
                message = stringResource(R.string.no_project_message),
                actionLabel = stringResource(R.string.project_new),
                onAction = onAddProject,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (s.projects.size > 1 && viewModel.canSwitchProject) {
                item {
                    ProjectPicker(projects = s.projects, selected = s.project, onSelect = viewModel::selectProject)
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = s.filter == null, onClick = { viewModel.setFilter(null) }, label = { Text(stringResource(R.string.filter_all)) })
                    ExpenseCategory.entries.forEach { c ->
                        FilterChip(selected = s.filter == c, onClick = { viewModel.setFilter(c) }, label = { Text(stringResource(c.labelRes)) })
                    }
                }
            }
            item {
                InfoCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    LabelValue(stringResource(R.string.total), money(s.total), bold = true)
                    if (s.filter == null) {
                        s.byCategory.forEach { (cat, amount) -> LabelValue(stringResource(cat.labelRes), money(amount)) }
                    }
                }
            }
            if (s.expenses.isEmpty()) {
                item { EmptyState(title = stringResource(R.string.expenses_empty), icon = Icons.AutoMirrored.Outlined.ReceiptLong, modifier = Modifier.padding(top = 24.dp)) }
            }
            items(s.expenses, key = { it.id }) { e ->
                ExpenseRow(e, s.stages.firstOrNull { it.id == e.stageId }, onClick = { onEdit(e.projectId, e.id) })
            }
        }
    }
}

@Composable
private fun ExpenseRow(e: Expense, stage: Stage?, onClick: () -> Unit) {
    InfoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(e.category.labelRes), style = MaterialTheme.typography.titleSmall)
                val details = listOfNotNull(Dates.format(e.date), stage?.displayName(), e.note.takeIf { it.isNotBlank() }).joinToString(" · ")
                Text(
                    details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(money(e.amount), style = MaterialTheme.typography.titleSmall)
            if (e.billPhotoPath != null) {
                Icon(Icons.Outlined.Receipt, contentDescription = stringResource(R.string.bill_photo), modifier = Modifier.padding(start = 8.dp), tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
