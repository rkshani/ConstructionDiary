package com.diary.construction.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.diary.construction.R

enum class TopLevelDestination(
    val route: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
    @StringRes val label: Int,
) {
    PROJECTS(Routes.PROJECTS, Icons.Filled.Apartment, Icons.Outlined.Apartment, R.string.tab_projects),
    EXPENSES(Routes.EXPENSES, Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, R.string.tab_expenses),
    REPORTS(Routes.REPORTS, Icons.Filled.Assessment, Icons.Outlined.Assessment, R.string.tab_reports),
    SETTINGS(Routes.SETTINGS, Icons.Filled.Settings, Icons.Outlined.Settings, R.string.tab_settings),
}
