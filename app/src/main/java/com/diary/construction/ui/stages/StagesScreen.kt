package com.diary.construction.ui.stages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.diary.construction.core.util.Dates
import com.diary.construction.domain.model.StageStatus
import com.diary.construction.domain.model.StageSummary
import com.diary.construction.domain.repository.StageRepository
import com.diary.construction.domain.usecase.ObserveStageSummariesUseCase
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StagesViewModel @Inject constructor(
    savedState: SavedStateHandle,
    observeStages: ObserveStageSummariesUseCase,
    private val stages: StageRepository,
) : ViewModel() {
    val projectId: Long = checkNotNull(savedState.get<Long>(Routes.ARG_PROJECT_ID))

    val items: StateFlow<List<StageSummary>?> = observeStages(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun move(index: Int, by: Int) {
        val list = items.value?.map { it.stage }?.toMutableList() ?: return
        val target = index + by
        if (target !in list.indices) return
        val item = list.removeAt(index)
        list.add(target, item)
        viewModelScope.launch { stages.reorder(list) }
    }

    fun addDefaults() {
        viewModelScope.launch { stages.addDefaultStages(projectId) }
    }
}

@Composable
fun StagesScreen(
    onBack: () -> Unit,
    onEdit: (projectId: Long, stageId: Long) -> Unit,
    viewModel: StagesViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.section_stages), onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEdit(viewModel.projectId, 0) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.stage_add)) },
            )
        },
    ) { padding ->
        val list = items ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.stages_empty),
                icon = Icons.Outlined.Timeline,
                actionLabel = stringResource(R.string.stages_add_defaults),
                onAction = viewModel::addDefaults,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(list, key = { _, row -> row.stage.id }) { index, item ->
                StageCard(
                    item = item,
                    canMoveUp = index > 0,
                    canMoveDown = index < list.lastIndex,
                    onMoveUp = { viewModel.move(index, -1) },
                    onMoveDown = { viewModel.move(index, 1) },
                    onClick = { onEdit(viewModel.projectId, item.stage.id) },
                )
            }
        }
    }
}

@Composable
private fun StageCard(
    item: StageSummary,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onClick: () -> Unit,
) {
    val stage = item.stage
    val extra = LocalExtraColors.current
    InfoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stage.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val dates = listOfNotNull(stage.startDate, stage.endDate).joinToString(" → ") { Dates.formatShort(it) }
                if (dates.isNotEmpty()) {
                    Text(dates, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            StatusChip(
                stringResource(stage.status.labelRes),
                color = when (stage.status) {
                    StageStatus.COMPLETED -> extra.positive
                    StageStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                    StageStatus.NOT_STARTED -> MaterialTheme.colorScheme.outline
                },
            )
            Column {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
                }
            }
        }
        LabelValue(stringResource(R.string.stage_budget), money(stage.budget))
        LabelValue(stringResource(R.string.spent), money(item.spent), bold = true)
        if (stage.budget > 0) {
            val f = item.spent.toFloat() / stage.budget
            ProgressBar(f, color = if (f > 1f) extra.danger else MaterialTheme.colorScheme.primary)
        }
        val endDate = stage.endDate
        if (endDate != null && stage.status != StageStatus.COMPLETED && endDate < Dates.today()) {
            Text(stringResource(R.string.stage_late), color = extra.danger, style = MaterialTheme.typography.bodySmall)
        }
    }
}
