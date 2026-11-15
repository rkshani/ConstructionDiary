package com.diary.construction.ui.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.diary.construction.R
import com.diary.construction.domain.model.BudgetLevel
import com.diary.construction.domain.model.ProjectSummary
import com.diary.construction.domain.model.SpendCategory
import com.diary.construction.domain.repository.SettingsRepository
import com.diary.construction.domain.usecase.ObserveProjectSummaryUseCase
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.InfoCard
import com.diary.construction.ui.components.LabelValue
import com.diary.construction.ui.components.ProgressBar
import com.diary.construction.ui.components.SectionHeader
import com.diary.construction.ui.components.balanceColor
import com.diary.construction.ui.components.isThekedar
import com.diary.construction.ui.components.labelRes
import com.diary.construction.ui.components.money
import com.diary.construction.ui.components.roleString
import com.diary.construction.ui.navigation.Routes
import com.diary.construction.ui.theme.CategoryContractor
import com.diary.construction.ui.theme.CategoryLabour
import com.diary.construction.ui.theme.CategoryMaterial
import com.diary.construction.ui.theme.CategoryOther
import com.diary.construction.ui.theme.LocalExtraColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Parts of a project that open from the dashboard. */
enum class ProjectSection(@StringRes val label: Int, val icon: ImageVector) {
    STAGES(R.string.section_stages, Icons.Outlined.Timeline),
    MATERIAL(R.string.section_material, Icons.Outlined.Inventory2),
    SUPPLIERS(R.string.section_suppliers, Icons.Outlined.LocalShipping),
    LABOUR(R.string.section_labour, Icons.Outlined.Groups),
    CONTRACTOR(R.string.section_contractor_owner, Icons.Outlined.Engineering),
    EXPENSES(R.string.section_expenses, Icons.AutoMirrored.Outlined.ReceiptLong),
    PHOTOS(R.string.section_photos, Icons.Outlined.PhotoLibrary),
    REPORTS(R.string.section_reports, Icons.Outlined.Assessment),
    CALCULATOR(R.string.section_calculator, Icons.Outlined.Calculate),
}

@HiltViewModel
class ProjectDashboardViewModel @Inject constructor(
    savedState: SavedStateHandle,
    observeSummary: ObserveProjectSummaryUseCase,
    settings: SettingsRepository,
) : ViewModel() {
    val projectId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_PROJECT_ID))

    val summary: StateFlow<ProjectSummary?> = observeSummary(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Opening a project makes it the one used on the Expenses and Reports tabs.
        viewModelScope.launch { settings.setSelectedProject(projectId) }
    }
}

@Composable
fun ProjectDashboardScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenSection: (projectId: Long, section: ProjectSection) -> Unit,
    viewModel: ProjectDashboardViewModel = hiltViewModel(),
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val s = summary
    Scaffold(
        topBar = {
            AppTopBar(
                title = s?.project?.name.orEmpty(),
                subtitle = s?.project?.siteAddress,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { onEdit(viewModel.projectId) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                },
            )
        },
    ) { padding ->
        if (s == null) return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BudgetWarning(s)
            BudgetCard(s)
            CategoryCard(s)
            DuesCard(s)
            ProgressCard(s)
            SectionHeader(stringResource(R.string.dashboard_open))
            SectionGrid(onOpen = { onOpenSection(viewModel.projectId, it) })
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BudgetWarning(s: ProjectSummary) {
    val extra = LocalExtraColors.current
    val (text, color) = when (s.budgetLevel) {
        BudgetLevel.OVER -> stringResource(R.string.budget_over, money(s.spent - s.project.budget)) to extra.danger
        BudgetLevel.WARNING -> stringResource(R.string.budget_warning, s.budgetUsedPercent) to extra.warning
        else -> return
    }
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = color)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun BudgetCard(s: ProjectSummary) {
    val extra = LocalExtraColors.current
    InfoCard {
        Text(roleString(R.string.dashboard_budget_title_owner, R.string.dashboard_budget_title_thekedar), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        LabelValue(roleString(R.string.budget_owner, R.string.budget_thekedar), money(s.project.budget))
        LabelValue(roleString(R.string.spent, R.string.cost_so_far), money(s.spent), bold = true)
        val remainingLabel = if (isThekedar()) R.string.expected_profit else R.string.remaining
        LabelValue(
            stringResource(remainingLabel),
            money(s.remaining),
            valueColor = if (s.remaining < 0) extra.danger else extra.positive,
            bold = true,
        )
        if (s.project.budget > 0) {
            Spacer(Modifier.height(4.dp))
            ProgressBar(
                s.spent.toFloat() / s.project.budget,
                color = when (s.budgetLevel) {
                    BudgetLevel.OVER -> extra.danger
                    BudgetLevel.WARNING -> extra.warning
                    else -> MaterialTheme.colorScheme.primary
                },
            )
            Text(
                stringResource(R.string.budget_used_percent, s.budgetUsedPercent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun SpendCategory.color(): Color = when (this) {
    SpendCategory.MATERIAL -> CategoryMaterial
    SpendCategory.LABOUR -> CategoryLabour
    SpendCategory.CONTRACTOR -> CategoryContractor
    SpendCategory.OTHER -> CategoryOther
}

@Composable
private fun CategoryCard(s: ProjectSummary) {
    InfoCard {
        Text(stringResource(R.string.dashboard_by_category), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        // Stacked bar
        if (s.spent > 0) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp)),
            ) {
                SpendCategory.entries.forEach { cat ->
                    val amount = s.amountFor(cat)
                    if (amount > 0) {
                        Box(
                            Modifier
                                .weight(amount.toFloat())
                                .fillMaxSize()
                                .background(cat.color()),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        SpendCategory.entries.forEach { cat ->
            val amount = s.amountFor(cat)
            val pct = if (s.spent > 0) (amount * 100 / s.spent).toInt() else 0
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(cat.color()))
                Spacer(Modifier.width(8.dp))
                val label = if (cat == SpendCategory.CONTRACTOR) {
                    roleString(R.string.cat_contractor_owner, R.string.cat_contractor_thekedar)
                } else {
                    stringResource(cat.labelRes)
                }
                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("$pct%  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(money(amount), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DuesCard(s: ProjectSummary) {
    InfoCard {
        Text(stringResource(R.string.pending_dues), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        LabelValue(stringResource(R.string.dues_suppliers), money(s.supplierDue))
        LabelValue(stringResource(R.string.dues_labour), money(s.labourDue))
        LabelValue(roleString(R.string.cat_contractor_owner, R.string.cat_contractor_thekedar), money(s.contractorDue))
        LabelValue(stringResource(R.string.total), money(s.pendingDues), valueColor = balanceColor(s.pendingDues), bold = true)
    }
}

@Composable
private fun ProgressCard(s: ProjectSummary) {
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.work_progress), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${s.progressPercent}%", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
        ProgressBar(s.progressPercent / 100f, color = LocalExtraColors.current.positive)
        Text(
            stringResource(R.string.stages_done_of, s.stagesDone, s.stagesTotal),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionGrid(onOpen: (ProjectSection) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 3,
    ) {
        ProjectSection.entries.forEach { section ->
            val label = if (section == ProjectSection.CONTRACTOR) {
                roleString(R.string.section_contractor_owner, R.string.section_contractor_thekedar)
            } else {
                stringResource(section.label)
            }
            Card(
                onClick = { onOpen(section) },
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(section.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}
