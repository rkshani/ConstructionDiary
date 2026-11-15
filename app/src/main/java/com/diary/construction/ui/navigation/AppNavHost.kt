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
import com.diary.construction.ui.projects.ProjectEditScreen
import com.diary.construction.ui.projects.ProjectsScreen
import com.diary.construction.ui.role.RoleSelectScreen

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
                onOpenProject = { navController.navigate(Routes.projectEdit(it)) },
                onAddProject = { navController.navigate(Routes.projectEdit()) },
            )
        }
        composable(Routes.PROJECT_EDIT, arguments = listOf(idArg())) {
            ProjectEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { _, _ -> navController.popBackStack() },
                onDeleted = { navController.popBackStack(Routes.PROJECTS, inclusive = false) },
            )
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
