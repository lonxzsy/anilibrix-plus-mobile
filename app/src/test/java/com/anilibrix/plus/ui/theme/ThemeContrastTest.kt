package com.anilibrix.plus.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Страховка от того класса багов, из-за которого в ToastHost белый текст стоял
 * на янтарном фоне: контраст 1.63:1 при требуемых 4.5:1.
 *
 * Проверяет каждую пару «on-роль + её фон» в обеих схемах.
 */
class ThemeContrastTest {

    private fun Color.relativeLuminance(): Double {
        fun channel(v: Float): Double {
            val c = v.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
    }

    private fun contrast(foreground: Color, background: Color): Double {
        val a = foreground.relativeLuminance()
        val b = background.relativeLuminance()
        val hi = maxOf(a, b)
        val lo = minOf(a, b)
        return (hi + 0.05) / (lo + 0.05)
    }

    /** Пары, где on-цвет несёт текст → порог WCAG AA 4.5:1. */
    private fun textPairs(s: ColorScheme): List<Triple<String, Color, Color>> = listOf(
        Triple("onPrimary/primary", s.onPrimary, s.primary),
        Triple("onPrimaryContainer/primaryContainer", s.onPrimaryContainer, s.primaryContainer),
        Triple("onSecondary/secondary", s.onSecondary, s.secondary),
        Triple("onSecondaryContainer/secondaryContainer", s.onSecondaryContainer, s.secondaryContainer),
        Triple("onTertiary/tertiary", s.onTertiary, s.tertiary),
        Triple("onTertiaryContainer/tertiaryContainer", s.onTertiaryContainer, s.tertiaryContainer),
        Triple("onError/error", s.onError, s.error),
        Triple("onErrorContainer/errorContainer", s.onErrorContainer, s.errorContainer),
        Triple("onBackground/background", s.onBackground, s.background),
        Triple("onSurface/surface", s.onSurface, s.surface),
        Triple("onSurfaceVariant/surface", s.onSurfaceVariant, s.surface),
        Triple("onSurfaceVariant/surfaceVariant", s.onSurfaceVariant, s.surfaceVariant),
        Triple("onSurface/surfaceContainerLowest", s.onSurface, s.surfaceContainerLowest),
        Triple("onSurface/surfaceContainerLow", s.onSurface, s.surfaceContainerLow),
        Triple("onSurface/surfaceContainer", s.onSurface, s.surfaceContainer),
        Triple("onSurface/surfaceContainerHigh", s.onSurface, s.surfaceContainerHigh),
        Triple("onSurface/surfaceContainerHighest", s.onSurface, s.surfaceContainerHighest),
        Triple("inverseOnSurface/inverseSurface", s.inverseOnSurface, s.inverseSurface),
        Triple("onPrimaryFixed/primaryFixed", s.onPrimaryFixed, s.primaryFixed),
        Triple("onSecondaryFixed/secondaryFixed", s.onSecondaryFixed, s.secondaryFixed),
        Triple("onTertiaryFixed/tertiaryFixed", s.onTertiaryFixed, s.tertiaryFixed),
    )

    private fun extendedTextPairs(e: AnilibrixExtendedColors): List<Triple<String, Color, Color>> = listOf(
        Triple("onSuccess/success", e.onSuccess, e.success),
        Triple("onSuccessContainer/successContainer", e.onSuccessContainer, e.successContainer),
        Triple("onWarning/warning", e.onWarning, e.warning),
        Triple("onWarningContainer/warningContainer", e.onWarningContainer, e.warningContainer),
        Triple("onInfo/info", e.onInfo, e.info),
        Triple("onInfoContainer/infoContainer", e.onInfoContainer, e.infoContainer),
        Triple("onMediaScrim/mediaScrim", e.onMediaScrim, Color(0xFF000000)),
    )

    private fun assertAllPass(label: String, pairs: List<Triple<String, Color, Color>>) {
        val failures = pairs
            .map { (name, fg, bg) -> name to contrast(fg, bg) }
            .filter { (_, ratio) -> ratio < AA_TEXT }
        assertTrue(
            "$label: пары ниже WCAG AA ($AA_TEXT:1): " +
                failures.joinToString { (n, r) -> "$n = ${"%.2f".format(r)}" },
            failures.isEmpty(),
        )
    }

    @Test
    fun `тёмная схема — все текстовые пары проходят AA`() {
        assertAllPass("dark", textPairs(AnilibrixDarkColorScheme))
    }

    @Test
    fun `светлая схема — все текстовые пары проходят AA`() {
        assertAllPass("light", textPairs(AnilibrixLightColorScheme))
    }

    @Test
    fun `расширенные цвета — все текстовые пары проходят AA`() {
        assertAllPass("extended dark", extendedTextPairs(ExtendedDarkColors))
        assertAllPass("extended light", extendedTextPairs(ExtendedLightColors))
    }

    /**
     * outline несёт границы, а не текст, поэтому порог — 3:1.
     * В светлой теме он даёт 4.25:1: для границ достаточно, для текста нет.
     */
    @Test
    fun `outline проходит порог для нетекстовых элементов`() {
        listOf(
            "dark" to AnilibrixDarkColorScheme,
            "light" to AnilibrixLightColorScheme,
        ).forEach { (label, scheme) ->
            val ratio = contrast(scheme.outline, scheme.surface)
            assertTrue(
                "$label: outline/surface = ${"%.2f".format(ratio)}, нужно >= $AA_NON_TEXT",
                ratio >= AA_NON_TEXT,
            )
        }
    }

    private companion object {
        const val AA_TEXT = 4.5
        const val AA_NON_TEXT = 3.0
    }
}
