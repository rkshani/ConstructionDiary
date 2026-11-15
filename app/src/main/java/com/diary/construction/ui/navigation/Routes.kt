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
}
