package com.anilibrix.plus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val md3_dark_primary = Color(0xFFC7B8FF)
val md3_dark_onPrimary = Color(0xFF211646)
val md3_dark_primaryContainer = Color(0xFF5A43C8)
val md3_dark_onPrimaryContainer = Color(0xFFF0E9FF)
val md3_dark_secondary = Color(0xFF84D7FF)
val md3_dark_onSecondary = Color(0xFF062C3D)
val md3_dark_surface = Color(0xFF0A0712)
val md3_dark_onSurface = Color(0xFFF0ECF6)
val md3_dark_surfaceContainer = Color(0xFF181226)
val md3_dark_surfaceContainerHigh = Color(0xFF241A38)
val md3_dark_background = Color(0xFF0A0712)
val md3_dark_onBackground = Color(0xFFE6E1E5)
val md3_dark_error = Color(0xFFF2B8B5)
val md3_dark_outline = Color(0xFF8B9198)
val md3_dark_surfaceVariant = Color(0xFF1E1D23)

val md3_light_primary = Color(0xFF6750A4)
val md3_light_onPrimary = Color(0xFFFFFFFF)
val md3_light_primaryContainer = Color(0xFFEADEFF)
val md3_light_onPrimaryContainer = Color(0xFF21005D)
val md3_light_secondary = Color(0xFF625B71)
val md3_light_onSecondary = Color(0xFFFFFFFF)
val md3_light_surface = Color(0xFFFEF7FF)
val md3_light_onSurface = Color(0xFF1C1B1F)
val md3_light_surfaceContainer = Color(0xFFF3EDF7)
val md3_light_surfaceContainerHigh = Color(0xFFECE6F0)
val md3_light_background = Color(0xFFFEF7FF)
val md3_light_onBackground = Color(0xFF1C1B1F)
val md3_light_error = Color(0xFFB3261E)
val md3_light_onError = Color(0xFFFFFFFF)
val md3_light_outline = Color(0xFF79747E)
val md3_light_surfaceVariant = Color(0xFFE7E0EC)

val glassOverlay = Color(0x33FFFFFF)
val glassBorder = Color(0x1FFFFFFF)
val shimmerBase = Color(0x33FFFFFF)
val shimmerHighlight = Color(0x66FFFFFF)
val cardOverlay = Color(0x19FFFFFF)

val successGreen = Color(0xFF4CAF50)
val warningAmber = Color(0xFFFFC107)
val infoBlue = Color(0xFF2196F3)

object AnilibrixBrushes {
    val primaryGradient: Brush
        @Composable
        get() = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
            )
        )

    val heroOverlay: Brush
        @Composable
        get() = Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.Transparent,
                0.45f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f),
                1.0f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.9f)
            )
        )

    val cardGlow: Brush
        @Composable
        get() = Brush.radialGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                Color.Transparent
            )
        )

    val shimmerBrush: Brush
        @Composable
        get() = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceContainer,
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.colorScheme.surfaceContainer
            )
        )
}
