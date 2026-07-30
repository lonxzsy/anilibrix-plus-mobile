package com.anilibrix.plus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Фирменная палитра AnilibrixPlus.
 *
 * Все 48 ролей MD3 заданы явно. Раньше их было 15, остальные молча падали на
 * baseline-фиолетово-серый, который конфликтовал с фирменными фиолетовыми
 * поверхностями. [ThemeSchemeCompletenessTest] следит, чтобы это не повторилось.
 *
 * Тёмная лестница поверхностей намеренно сжата вниз относительно спеки
 * (спека даёт surface на тоне ~6, у нас ~2), чтобы сохранить OLED-чёрный,
 * ради которого приложение и задумывалось.
 *
 * Шесть якорей ниже помечены как BRAND — это исходные значения проекта,
 * остальное сгенерировано так, чтобы быть с ними непрерывным.
 * Контраст всех пар «on-роль + её контейнер» проверен: каждая проходит WCAG AA.
 */
val AnilibrixDarkColorScheme = darkColorScheme(
    primary = Color(0xFFC7B8FF),                 // BRAND
    onPrimary = Color(0xFF211646),               // BRAND — 9.25:1 к primary
    primaryContainer = Color(0xFF5A43C8),        // BRAND
    onPrimaryContainer = Color(0xFFF4EEFF),
    inversePrimary = Color(0xFF5A43C8),
    secondary = Color(0xFF84D7FF),               // BRAND
    onSecondary = Color(0xFF003547),
    secondaryContainer = Color(0xFF004D66),
    onSecondaryContainer = Color(0xFFC0E8FF),
    tertiary = Color(0xFFFFB1C0),
    onTertiary = Color(0xFF59192B),
    tertiaryContainer = Color(0xFF7A2B40),
    onTertiaryContainer = Color(0xFFFFD9DF),
    error = Color(0xFFFFB3AD),
    onError = Color(0xFF640B15),
    errorContainer = Color(0xFF881D24),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0A0712),              // BRAND
    onBackground = Color(0xFFE4E1EC),
    surface = Color(0xFF0A0712),                 // BRAND
    onSurface = Color(0xFFF0ECF6),               // BRAND
    surfaceVariant = Color(0xFF383266),
    onSurfaceVariant = Color(0xFFC9C4D9),
    surfaceTint = Color(0xFFC7B8FF),
    inverseSurface = Color(0xFFE4E1EC),
    inverseOnSurface = Color(0xFF332B4F),
    outline = Color(0xFF938DAB),
    outlineVariant = Color(0xFF383266),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3D335A),
    surfaceDim = Color(0xFF0A0712),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF130E1D),
    surfaceContainer = Color(0xFF181226),        // BRAND
    surfaceContainerHigh = Color(0xFF241A38),    // BRAND
    surfaceContainerHighest = Color(0xFF2D2546),
    primaryFixed = Color(0xFFE8DDFF),
    primaryFixedDim = Color(0xFFC7B8FF),
    onPrimaryFixed = Color(0xFF1C153E),
    onPrimaryFixedVariant = Color(0xFF43369E),
    secondaryFixed = Color(0xFFC0E8FF),
    secondaryFixedDim = Color(0xFF84D7FF),
    onSecondaryFixed = Color(0xFF0A1E27),
    onSecondaryFixedVariant = Color(0xFF004D66),
    tertiaryFixed = Color(0xFFFFD9DF),
    tertiaryFixedDim = Color(0xFFFFB1C0),
    onTertiaryFixed = Color(0xFF390918),
    onTertiaryFixedVariant = Color(0xFF7A2B40),
)

val AnilibrixLightColorScheme = lightColorScheme(
    primary = Color(0xFF5A43C8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8DDFF),
    onPrimaryContainer = Color(0xFF1F1749),
    inversePrimary = Color(0xFFC7B8FF),
    secondary = Color(0xFF005976),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC0E8FF),
    onSecondaryContainer = Color(0xFF09222D),
    tertiary = Color(0xFF9C3F57),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9DF),
    onTertiaryContainer = Color(0xFF3F0C1B),
    error = Color(0xFFAD3035),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF480104),
    background = Color(0xFFFAF9FB),
    onBackground = Color(0xFF1E1732),
    surface = Color(0xFFFAF9FB),
    onSurface = Color(0xFF1E1732),
    surfaceVariant = Color(0xFFE4E0F1),
    onSurfaceVariant = Color(0xFF474070),
    surfaceTint = Color(0xFF5A43C8),
    inverseSurface = Color(0xFF332B4F),
    inverseOnSurface = Color(0xFFF2F0F5),
    // Внимание: в светлой теме outline даёт 4.25:1 к surface — этого хватает
    // для границ (порог 3:1), но НЕ для текста. Под текст — onSurfaceVariant.
    outline = Color(0xFF7A7394),
    outlineVariant = Color(0xFFC9C4D9),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFAF9FB),
    surfaceDim = Color(0xFFDCD8E4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F3F7),
    surfaceContainer = Color(0xFFEFEDF4),
    surfaceContainerHigh = Color(0xFFEAE7F0),
    surfaceContainerHighest = Color(0xFFE4E1EC),
    // *Fixed роли не зависят от темы — совпадают с тёмной схемой.
    primaryFixed = Color(0xFFE8DDFF),
    primaryFixedDim = Color(0xFFC7B8FF),
    onPrimaryFixed = Color(0xFF1C153E),
    onPrimaryFixedVariant = Color(0xFF43369E),
    secondaryFixed = Color(0xFFC0E8FF),
    secondaryFixedDim = Color(0xFF84D7FF),
    onSecondaryFixed = Color(0xFF0A1E27),
    onSecondaryFixedVariant = Color(0xFF004D66),
    tertiaryFixed = Color(0xFFFFD9DF),
    tertiaryFixedDim = Color(0xFFFFB1C0),
    onTertiaryFixed = Color(0xFF390918),
    onTertiaryFixedVariant = Color(0xFF7A2B40),
)

// --- Временные алиасы -------------------------------------------------------
// Оставлены только чтобы не ломать ещё не переписанные экраны.
// Удаляются вместе с последним местом использования.

@Deprecated(
    "Белая вуаль 0x33FFFFFF невидима на светлой теме. Используй Modifier.shimmer().",
    ReplaceWith("MaterialTheme.colorScheme.surfaceContainer")
)
val shimmerBase = Color(0x33FFFFFF)

@Deprecated(
    "Фиксированный оттенок мимо схемы. Используй MaterialTheme.extended.success*.",
    ReplaceWith("MaterialTheme.extended.success")
)
val successGreen = Color(0xFF4CAF50)

@Deprecated(
    "Фиксированный оттенок мимо схемы. Используй MaterialTheme.extended.warning*.",
    ReplaceWith("MaterialTheme.extended.warning")
)
val warningAmber = Color(0xFFFFC107)

@Deprecated(
    "Фиксированный оттенок мимо схемы. Используй MaterialTheme.extended.info*.",
    ReplaceWith("MaterialTheme.extended.info")
)
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

    /**
     * Затемнение под текстом поверх постера. Берёт цвета из [AnilibrixExtendedColors],
     * а не из colorScheme.scrim — иначе в светлой теме градиент уходит в чистый чёрный.
     */
    val heroOverlay: Brush
        @Composable
        get() = Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to MaterialTheme.extended.scrimGradientTop,
                0.45f to MaterialTheme.extended.scrimGradientMid,
                1.0f to MaterialTheme.extended.scrimGradientBottom
            )
        )

    @Deprecated(
        "Статичный градиент — он не едет, поэтому это пульсация, а не shimmer. " +
            "Используй Modifier.shimmer() из ui/components/Shimmer.kt."
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
