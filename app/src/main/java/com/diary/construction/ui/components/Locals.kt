package com.diary.construction.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import com.diary.construction.core.util.Money
import com.diary.construction.domain.model.AppSettings
import com.diary.construction.domain.model.UserRole

/** Current settings, provided once at the top of the UI. */
val LocalAppSettings = staticCompositionLocalOf { AppSettings() }

/** Formats paisa with the user's currency symbol. */
@Composable
@ReadOnlyComposable
fun money(paisa: Long): String = Money.format(paisa, LocalAppSettings.current.currency)

/** Picks the label that fits the user's role (Owner or Thekedar). */
@Composable
@ReadOnlyComposable
fun roleString(@StringRes owner: Int, @StringRes thekedar: Int): String =
    if (LocalAppSettings.current.role == UserRole.THEKEDAR) stringResource(thekedar) else stringResource(owner)

@Composable
@ReadOnlyComposable
fun isThekedar(): Boolean = LocalAppSettings.current.role == UserRole.THEKEDAR
