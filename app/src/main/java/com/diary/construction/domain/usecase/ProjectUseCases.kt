package com.diary.construction.domain.usecase

import com.diary.construction.domain.model.Project
import com.diary.construction.domain.repository.ExpenseRepository
import com.diary.construction.domain.repository.FileStore
import com.diary.construction.domain.repository.MaterialRepository
import com.diary.construction.domain.repository.PhotoRepository
import com.diary.construction.domain.repository.ProjectRepository
import com.diary.construction.domain.repository.SettingsRepository
import com.diary.construction.domain.repository.StageRepository
import javax.inject.Inject

/** Saves a project. A new project gets the standard stages and becomes the selected one. */
class SaveProjectUseCase @Inject constructor(
    private val projects: ProjectRepository,
    private val stages: StageRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(project: Project): Long {
        val isNew = project.id == 0L
        val id = projects.save(project)
        if (isNew) {
            stages.addDefaultStages(id)
            settings.setSelectedProject(id)
        }
        return id
    }
}

/** Deletes a project with all its rows (cascade) and its saved photos and bills. */
class DeleteProjectUseCase @Inject constructor(
    private val projects: ProjectRepository,
    private val photos: PhotoRepository,
    private val material: MaterialRepository,
    private val expenses: ExpenseRepository,
    private val files: FileStore,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(project: Project) {
        val paths = photos.photoPaths(project.id) + material.billPhotos(project.id) + expenses.billPhotos(project.id)
        projects.delete(project)
        files.delete(paths)
        if (settings.current().selectedProjectId == project.id) settings.setSelectedProject(null)
    }
}
