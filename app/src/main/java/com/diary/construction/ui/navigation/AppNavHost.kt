package com.diary.construction.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.diary.construction.ui.contractor.ContractorDetailScreen
import com.diary.construction.ui.contractor.ContractorEditScreen
import com.diary.construction.ui.contractor.ContractorsScreen
import com.diary.construction.ui.dashboard.ProjectDashboardScreen
import com.diary.construction.ui.dashboard.ProjectSection
import com.diary.construction.ui.expenses.ExpenseEditScreen
import com.diary.construction.ui.expenses.ExpensesScreen
import com.diary.construction.ui.labour.LabourDetailScreen
import com.diary.construction.ui.labour.LabourEditScreen
import com.diary.construction.ui.labour.LabourScreen
import com.diary.construction.ui.material.MaterialEntryEditScreen
import com.diary.construction.ui.material.MaterialScreen
import com.diary.construction.ui.material.MaterialTypesScreen
import com.diary.construction.ui.photos.PhotosScreen
import com.diary.construction.ui.projects.ProjectEditScreen
import com.diary.construction.ui.projects.ProjectsScreen
import com.diary.construction.ui.reminders.RemindersScreen
import com.diary.construction.ui.reports.ReportsScreen
import com.diary.construction.ui.role.RoleSelectScreen
import com.diary.construction.ui.settings.SettingsScreen
import com.diary.construction.ui.stages.StageEditScreen
import com.diary.construction.ui.suppliers.SupplierEditScreen
import com.diary.construction.ui.suppliers.SupplierKhataScreen
import com.diary.construction.ui.suppliers.SuppliersScreen
import com.diary.construction.ui.stages.StagesScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable(Routes.ROLE) {
            RoleSelectScreen(onDone = {
                navController.navigate(Routes.PROJECTS) { popUpTo(Routes.ROLE) { inclusive = true } }
            })
        }
        composable(Routes.PROJECTS) {
            ProjectsScreen(
                onOpenProject = { navController.navigate(Routes.project(it)) },
                onAddProject = { navController.navigate(Routes.projectEdit()) },
            )
        }
        composable(Routes.PROJECT_EDIT, arguments = listOf(idArg())) {
            ProjectEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { id, isNew ->
                    if (isNew) {
                        navController.navigate(Routes.project(id)) { popUpTo(Routes.PROJECT_EDIT) { inclusive = true } }
                    } else {
                        navController.popBackStack()
                    }
                },
                onDeleted = { navController.popBackStack(Routes.PROJECTS, inclusive = false) },
            )
        }
        composable(Routes.PROJECT, arguments = listOf(longArg(Routes.ARG_PROJECT_ID))) {
            ProjectDashboardScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.projectEdit(it)) },
                onOpenSection = { projectId, section -> navController.openSection(projectId, section) },
            )
        }
        composable(Routes.STAGES, arguments = listOf(longArg(Routes.ARG_PROJECT_ID))) {
            StagesScreen(
                onBack = { navController.popBackStack() },
                onEdit = { projectId, id -> navController.navigate(Routes.stageEdit(projectId, id)) },
            )
        }
        composable(Routes.STAGE_EDIT, arguments = listOf(longArg(Routes.ARG_PROJECT_ID), idArg())) {
            StageEditScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MATERIAL, arguments = listOf(longArg(Routes.ARG_PROJECT_ID))) {
            MaterialScreen(
                onBack = { navController.popBackStack() },
                onEditEntry = { projectId, id -> navController.navigate(Routes.materialEdit(projectId, id)) },
                onManageTypes = { navController.navigate(Routes.MATERIAL_TYPES) },
            )
        }
        composable(Routes.MATERIAL_EDIT, arguments = listOf(longArg(Routes.ARG_PROJECT_ID), idArg())) {
            MaterialEntryEditScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MATERIAL_TYPES) {
            MaterialTypesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SUPPLIERS, arguments = listOf(idArg(Routes.ARG_PROJECT_ID))) { entry ->
            val projectId = entry.arguments?.getLong(Routes.ARG_PROJECT_ID) ?: 0L
            SuppliersScreen(
                onBack = { navController.popBackStack() },
                onOpen = { navController.navigate(Routes.supplierKhata(it, projectId)) },
                onAdd = { navController.navigate(Routes.supplierEdit()) },
            )
        }
        composable(Routes.SUPPLIER_EDIT, arguments = listOf(idArg())) {
            SupplierEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { id, isNew ->
                    if (isNew) {
                        navController.navigate(Routes.supplierKhata(id)) { popUpTo(Routes.SUPPLIER_EDIT) { inclusive = true } }
                    } else {
                        navController.popBackStack()
                    }
                },
                onDeleted = {
                    if (!navController.popBackStack(Routes.SUPPLIERS, inclusive = false)) navController.popBackStack()
                },
            )
        }
        composable(Routes.SUPPLIER_KHATA, arguments = listOf(longArg(Routes.ARG_ID), idArg(Routes.ARG_PROJECT_ID))) {
            SupplierKhataScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.supplierEdit(it)) },
            )
        }
        composable(Routes.LABOUR, arguments = listOf(longArg(Routes.ARG_PROJECT_ID))) {
            LabourScreen(
                onBack = { navController.popBackStack() },
                onAddLabour = { navController.navigate(Routes.labourEdit(it)) },
                onOpenLabour = { navController.navigate(Routes.labourDetail(it)) },
            )
        }
        composable(Routes.LABOUR_EDIT, arguments = listOf(longArg(Routes.ARG_PROJECT_ID), idArg())) {
            LabourEditScreen(
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack(Routes.LABOUR, inclusive = false) },
            )
        }
        composable(Routes.LABOUR_DETAIL, arguments = listOf(longArg(Routes.ARG_ID))) {
            LabourDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { projectId, id -> navController.navigate(Routes.labourEdit(projectId, id)) },
            )
        }
        composable(Routes.CONTRACTORS, arguments = listOf(longArg(Routes.ARG_PROJECT_ID))) {
            ContractorsScreen(
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate(Routes.contractorEdit(it)) },
                onOpen = { navController.navigate(Routes.contractorDetail(it)) },
            )
        }
        composable(Routes.CONTRACTOR_EDIT, arguments = listOf(longArg(Routes.ARG_PROJECT_ID), idArg())) {
            ContractorEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { id, isNew ->
                    if (isNew) {
                        navController.navigate(Routes.contractorDetail(id)) { popUpTo(Routes.CONTRACTOR_EDIT) { inclusive = true } }
                    } else {
                        navController.popBackStack()
                    }
                },
                onDeleted = { navController.popBackStack(Routes.CONTRACTORS, inclusive = false) },
            )
        }
        composable(Routes.CONTRACTOR_DETAIL, arguments = listOf(longArg(Routes.ARG_ID))) {
            ContractorDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { projectId, id -> navController.navigate(Routes.contractorEdit(projectId, id)) },
            )
        }
        composable(Routes.EXPENSES) {
            ExpensesScreen(
                onBack = null,
                onEdit = { projectId, id -> navController.navigate(Routes.expenseEdit(projectId, id)) },
                onAddProject = { navController.navigate(Routes.projectEdit()) },
            )
        }
        composable(Routes.PROJECT_EXPENSES, arguments = listOf(longArg(Routes.ARG_PROJECT_ID))) {
            ExpensesScreen(
                onBack = { navController.popBackStack() },
                onEdit = { projectId, id -> navController.navigate(Routes.expenseEdit(projectId, id)) },
                onAddProject = { navController.navigate(Routes.projectEdit()) },
            )
        }
        composable(Routes.PHOTOS, arguments = listOf(longArg(Routes.ARG_PROJECT_ID))) {
            PhotosScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.EXPENSE_EDIT, arguments = listOf(longArg(Routes.ARG_PROJECT_ID), idArg())) {
            ExpenseEditScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.REPORTS) {
            ReportsScreen(onBack = null, onAddProject = { navController.navigate(Routes.projectEdit()) })
        }
        composable(Routes.PROJECT_REPORTS, arguments = listOf(longArg(Routes.ARG_PROJECT_ID))) {
            ReportsScreen(onBack = { navController.popBackStack() }, onAddProject = { navController.navigate(Routes.projectEdit()) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onOpenReminders = { navController.navigate(Routes.REMINDERS) })
        }
        composable(Routes.REMINDERS) { RemindersScreen(onBack = { navController.popBackStack() }) }
    }
}

/** Optional Long "id" argument, 0 = new item. */
internal fun idArg(name: String = Routes.ARG_ID) = navArgument(name) {
    type = NavType.LongType
    defaultValue = 0L
}

/** Required Long argument. */
internal fun longArg(name: String) = navArgument(name) { type = NavType.LongType }

/** Opens a part of a project from the dashboard. */
internal fun NavHostController.openSection(projectId: Long, section: ProjectSection) {
    val route: String? = when (section) {
        ProjectSection.STAGES -> Routes.stages(projectId)
        ProjectSection.MATERIAL -> Routes.material(projectId)
        ProjectSection.SUPPLIERS -> Routes.suppliers(projectId)
        ProjectSection.LABOUR -> Routes.labour(projectId)
        ProjectSection.CONTRACTOR -> Routes.contractors(projectId)
        ProjectSection.EXPENSES -> Routes.projectExpenses(projectId)
        ProjectSection.PHOTOS -> Routes.photos(projectId)
        ProjectSection.REPORTS -> Routes.projectReports(projectId)
        else -> null
    }
    if (route != null) navigate(route)
}
