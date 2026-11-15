package com.diary.construction.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.diary.construction.R
import com.diary.construction.ui.components.PlaceholderScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable(Routes.PROJECTS) { PlaceholderScreen(R.string.tab_projects) }
        composable(Routes.EXPENSES) { PlaceholderScreen(R.string.tab_expenses) }
        composable(Routes.REPORTS) { PlaceholderScreen(R.string.tab_reports) }
        composable(Routes.SETTINGS) { PlaceholderScreen(R.string.tab_settings) }
    }
}
