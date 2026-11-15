package com.diary.construction.ui.navigation

/** All navigation routes. Arguments are plain Long ids. */
object Routes {
    // Bottom bar tabs
    const val PROJECTS = "projects"
    const val EXPENSES = "expenses"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"

    // First launch
    const val ROLE = "role"

    const val ARG_PROJECT_ID = "projectId"
    const val ARG_ID = "id"

    // Projects
    const val PROJECT_EDIT = "project_edit?$ARG_ID={$ARG_ID}"
    fun projectEdit(id: Long = 0) = "project_edit?$ARG_ID=$id"
    const val PROJECT = "project/{$ARG_PROJECT_ID}"
    fun project(projectId: Long) = "project/$projectId"

    // Stages
    const val STAGES = "project/{$ARG_PROJECT_ID}/stages"
    fun stages(projectId: Long) = "project/$projectId/stages"
    const val STAGE_EDIT = "project/{$ARG_PROJECT_ID}/stage_edit?$ARG_ID={$ARG_ID}"
    fun stageEdit(projectId: Long, id: Long = 0) = "project/$projectId/stage_edit?$ARG_ID=$id"

    // Material
    const val MATERIAL = "project/{$ARG_PROJECT_ID}/material"
    fun material(projectId: Long) = "project/$projectId/material"
    const val MATERIAL_EDIT = "project/{$ARG_PROJECT_ID}/material_edit?$ARG_ID={$ARG_ID}"
    fun materialEdit(projectId: Long, id: Long = 0) = "project/$projectId/material_edit?$ARG_ID=$id"
    const val MATERIAL_TYPES = "material_types"
}
