package com.diary.construction.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Orange50,
    onPrimary = Color.White,
    primaryContainer = Orange90,
    onPrimaryContainer = Orange10,
    secondary = Grey40,
    onSecondary = Color.White,
    secondaryContainer = Grey90,
    onSecondaryContainer = Grey10,
    tertiary = Steel40,
    onTertiary = Color.White,
    tertiaryContainer = Steel90,
    onTertiaryContainer = Steel10,
    error = ErrorRed40,
    onError = Color.White,
    errorContainer = ErrorRed90,
    onErrorContainer = ErrorRed10,
    background = SurfaceLight,
    onBackground = Grey10,
    surface = SurfaceLight,
    onSurface = Grey10,
    surfaceVariant = Grey95,
    onSurfaceVariant = Grey30,
    outline = Grey40,
)

private val DarkColors = darkColorScheme(
    primary = Orange80,
    onPrimary = Orange20,
    primaryContainer = Orange30,
    onPrimaryContainer = Orange90,
    secondary = Grey80,
    onSecondary = Grey20,
    secondaryContainer = Grey30,
    onSecondaryContainer = Grey90,
    tertiary = Steel80,
    onTertiary = Steel10,
    tertiaryContainer = Steel40,
    onTertiaryContainer = Steel90,
    error = ErrorRed80,
    onError = ErrorRed10,
    errorContainer = ErrorRed40,
    onErrorContainer = ErrorRed90,
    background = SurfaceDark,
    onBackground = Grey90,
    surface = SurfaceDark,
    onSurface = Grey90,
    surfaceVariant = Grey30,
    onSurfaceVariant = Grey80,
    outline = Grey80,
)

/** Extra colours that Material does not have a slot for. */
@Immutable
data class ExtraColors(
    val positive: Color,
    val warning: Color,
    val danger: Color,
)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(positive = MoneyGreen, warning = WarnAmber, danger = DangerRed)
}

@Composable
fun ConstructionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extra = if (darkTheme) {
        ExtraColors(positive = MoneyGreenDark, warning = WarnAmber, danger = ErrorRed80)
    } else {
        ExtraColors(positive = MoneyGreen, warning = WarnAmber, danger = DangerRed)
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
