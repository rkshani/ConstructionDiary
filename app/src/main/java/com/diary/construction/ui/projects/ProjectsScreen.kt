package com.diary.construction.ui.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Apartment
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
import com.diary.construction.domain.model.ProjectListItem
import com.diary.construction.domain.model.ProjectStatus
import com.diary.construction.domain.usecase.ObserveProjectListUseCase
import com.diary.construction.ui.components.AppTopBar
import com.diary.construction.ui.components.EmptyState
import com.diary.construction.ui.components.InfoCard
import com.diary.construction.ui.components.LabelValue
import com.diary.construction.ui.components.ProgressBar
import com.diary.construction.ui.components.StatusChip
import com.diary.construction.ui.components.labelRes
import com.diary.construction.ui.components.money
import com.diary.construction.ui.components.roleString
import com.diary.construction.ui.theme.LocalExtraColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    observeProjects: ObserveProjectListUseCase,
) : ViewModel() {
    /** null while loading. */
    val projects: StateFlow<List<ProjectListItem>?> = observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ProjectsScreen(
    onOpenProject: (Long) -> Unit,
    onAddProject: () -> Unit,
    viewModel: ProjectsViewModel = hiltViewModel(),
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { AppTopBar(roleString(R.string.projects_title_owner, R.string.projects_title_thekedar)) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProject,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.project_new)) },
            )
        },
    ) { padding ->
        val list = projects
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyState(
                title = stringResource(R.string.projects_empty_title),
                message = stringResource(R.string.projects_empty_message),
                icon = Icons.Outlined.Apartment,
                actionLabel = stringResource(R.string.project_new),
                onAction = onAddProject,
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.project.id }) { item ->
                    ProjectCard(item, onClick = { onOpenProject(item.project.id) })
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(item: ProjectListItem, onClick: () -> Unit) {
    val p = item.project
    val extra = LocalExtraColors.current
    InfoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                p.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            StatusChip(
                stringResource(p.status.labelRes),
                color = when (p.status) {
                    ProjectStatus.COMPLETED -> extra.positive
                    ProjectStatus.ON_HOLD -> extra.warning
                    else -> MaterialTheme.colorScheme.primary
                },
            )
        }
        if (p.siteAddress.isNotBlank()) {
            Text(
                p.siteAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(4.dp))
        LabelValue(roleString(R.string.budget_owner, R.string.budget_thekedar), money(p.budget))
        LabelValue(stringResource(R.string.spent), money(item.spent), bold = true)
        if (p.budget > 0) {
            val fraction = item.spent.toFloat() / p.budget
            ProgressBar(
                fraction,
                color = when {
                    fraction > 1f -> extra.danger
                    fraction >= 0.9f -> extra.warning
                    else -> MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}
