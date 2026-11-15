package com.diary.construction.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.diary.construction.R
import com.diary.construction.ui.components.PlaceholderScreen
import com.diary.construction.ui.dashboard.ProjectDashboardScreen
import com.diary.construction.ui.dashboard.ProjectSection
import com.diary.construction.ui.projects.ProjectEditScreen
import com.diary.construction.ui.projects.ProjectsScreen
import com.diary.construction.ui.role.RoleSelectScreen
import com.diary.construction.ui.stages.StageEditScreen
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
        composable(Routes.EXPENSES) { PlaceholderScreen(R.string.tab_expenses) }
        composable(Routes.REPORTS) { PlaceholderScreen(R.string.tab_reports) }
        composable(Routes.SETTINGS) { PlaceholderScreen(R.string.tab_settings) }
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
        else -> null
    }
    if (route != null) navigate(route)
}
