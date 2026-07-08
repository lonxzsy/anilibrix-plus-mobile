package com.anilibrix.plus.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = md3_dark_primary,
    onPrimary = md3_dark_onPrimary,
    primaryContainer = md3_dark_primaryContainer,
    onPrimaryContainer = md3_dark_onPrimaryContainer,
    secondary = md3_dark_secondary,
    onSecondary = md3_dark_onSecondary,
    surface = md3_dark_surface,
    onSurface = md3_dark_onSurface,
    surfaceContainer = md3_dark_surfaceContainer,
    surfaceContainerHigh = md3_dark_surfaceContainerHigh,
    background = md3_dark_background,
    onBackground = md3_dark_onBackground,
    error = md3_dark_error,
    outline = md3_dark_outline,
    surfaceVariant = md3_dark_surfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = md3_light_primary,
    onPrimary = md3_light_onPrimary,
    primaryContainer = md3_light_primaryContainer,
    onPrimaryContainer = md3_light_onPrimaryContainer,
    secondary = md3_light_secondary,
    onSecondary = md3_light_onSecondary,
    surface = md3_light_surface,
    onSurface = md3_light_onSurface,
    surfaceContainer = md3_light_surfaceContainer,
    surfaceContainerHigh = md3_light_surfaceContainerHigh,
    background = md3_light_background,
    onBackground = md3_light_onBackground,
    error = md3_light_error,
    onError = md3_light_onError,
    outline = md3_light_outline,
    surfaceVariant = md3_light_surfaceVariant
)

@Composable
fun AnilibrixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AnilibrixTypography,
        shapes = AnilibrixShapes,
        content = content
    )
}
