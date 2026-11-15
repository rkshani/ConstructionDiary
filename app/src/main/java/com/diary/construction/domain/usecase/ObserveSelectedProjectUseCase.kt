package com.diary.construction.domain.usecase

import com.diary.construction.domain.model.Project
import com.diary.construction.domain.repository.ProjectRepository
import com.diary.construction.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class SelectedProject(val projects: List<Project>, val project: Project?)

/**
 * The project used by the Expenses and Reports tabs: [fixedProjectId] when it is set (opened
 * from a project), else the one picked last, else the newest project.
 */
class ObserveSelectedProjectUseCase @Inject constructor(
    private val projects: ProjectRepository,
    private val settings: SettingsRepository,
) {
    operator fun invoke(fixedProjectId: Long = 0): Flow<SelectedProject> = combine(
        projects.observeProjects(),
        settings.settings.map { it.selectedProjectId }.distinctUntilChanged(),
    ) { list, selectedId ->
        val id = if (fixedProjectId != 0L) fixedProjectId else selectedId
        SelectedProject(list, list.firstOrNull { it.id == id } ?: list.firstOrNull())
    }.distinctUntilChanged()
}
