package com.diary.construction.ui.navigation

/** All navigation routes. Arguments are plain Long ids. */
object Routes {
    // Bottom bar tabs
    const val PROJECTS = "projects"
    const val EXPENSES = "expenses"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"

    // Settings
    const val REMINDERS = "reminders"
    const val CALCULATOR = "calculator"
    const val BACKUP = "backup"

    // First launch
    const val ONBOARDING = "onboarding"
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

    // Suppliers (shared by all projects; projectId = where we came from, 0 = none)
    const val SUPPLIERS = "suppliers?$ARG_PROJECT_ID={$ARG_PROJECT_ID}"
    fun suppliers(projectId: Long = 0) = "suppliers?$ARG_PROJECT_ID=$projectId"
    const val SUPPLIER_EDIT = "supplier_edit?$ARG_ID={$ARG_ID}"
    fun supplierEdit(id: Long = 0) = "supplier_edit?$ARG_ID=$id"
    const val SUPPLIER_KHATA = "supplier/{$ARG_ID}?$ARG_PROJECT_ID={$ARG_PROJECT_ID}"
    fun supplierKhata(id: Long, projectId: Long = 0) = "supplier/$id?$ARG_PROJECT_ID=$projectId"

    // Labour
    const val LABOUR = "project/{$ARG_PROJECT_ID}/labour"
    fun labour(projectId: Long) = "project/$projectId/labour"
    const val LABOUR_EDIT = "project/{$ARG_PROJECT_ID}/labour_edit?$ARG_ID={$ARG_ID}"
    fun labourEdit(projectId: Long, id: Long = 0) = "project/$projectId/labour_edit?$ARG_ID=$id"
    const val LABOUR_DETAIL = "labour/{$ARG_ID}"
    fun labourDetail(id: Long) = "labour/$id"

    // Contractor (thekedar / sub-contractor)
    const val CONTRACTORS = "project/{$ARG_PROJECT_ID}/contractors"
    fun contractors(projectId: Long) = "project/$projectId/contractors"
    const val CONTRACTOR_EDIT = "project/{$ARG_PROJECT_ID}/contractor_edit?$ARG_ID={$ARG_ID}"
    fun contractorEdit(projectId: Long, id: Long = 0) = "project/$projectId/contractor_edit?$ARG_ID=$id"
    const val CONTRACTOR_DETAIL = "contractor/{$ARG_ID}"
    fun contractorDetail(id: Long) = "contractor/$id"

    // Other expenses
    const val PROJECT_EXPENSES = "project/{$ARG_PROJECT_ID}/expenses"
    fun projectExpenses(projectId: Long) = "project/$projectId/expenses"
    const val EXPENSE_EDIT = "project/{$ARG_PROJECT_ID}/expense_edit?$ARG_ID={$ARG_ID}"
    fun expenseEdit(projectId: Long, id: Long = 0) = "project/$projectId/expense_edit?$ARG_ID=$id"

    // Site photos
    const val PHOTOS = "project/{$ARG_PROJECT_ID}/photos"
    fun photos(projectId: Long) = "project/$projectId/photos"

    // Reports of one project (the Reports tab uses REPORTS)
    const val PROJECT_REPORTS = "project/{$ARG_PROJECT_ID}/reports"
    fun projectReports(projectId: Long) = "project/$projectId/reports"
}
