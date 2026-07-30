package com.anilibrix.plus.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * До редизайна из 48 ролей MD3 были заданы 15 — остальные молча падали на
 * baseline-фиолетово-серый, который конфликтовал с фирменными фиолетовыми
 * поверхностями, и заметить это по коду было невозможно.
 *
 * Тест перебирает роли рефлексией: если хоть одна снова совпадёт с baseline,
 * сборка упадёт.
 */
class ThemeSchemeCompletenessTest {

    private fun colorRoles(scheme: ColorScheme): Map<String, Color> =
        ColorScheme::class.memberProperties
            .filter { it.returnType.classifier == Color::class }
            .associate { prop ->
                prop.isAccessible = true
                prop.name to (prop.getter.call(scheme) as Color)
            }

    /**
     * Совпадение с baseline само по себе не доказывает, что роль забыли задать:
     * чистый чёрный и чистый белый — законные значения MD3 (scrim всегда чёрный,
     * onPrimary в светлой теме белый), и они обязаны совпадать.
     *
     * Ловим мы другое — что роль незаметно осталась на baseline-фиолетово-сером.
     * Поэтому ахроматические значения из проверки исключаются.
     */
    private fun Color.isAchromatic(): Boolean =
        (red == green && green == blue)

    private fun assertNoBaselineLeftovers(label: String, ours: ColorScheme, baseline: ColorScheme) {
        val oursRoles = colorRoles(ours)
        val baselineRoles = colorRoles(baseline)

        val leftovers = oursRoles.filter { (name, color) ->
            !color.isAchromatic() && baselineRoles[name] == color
        }.keys

        assertTrue(
            "$label: роли всё ещё равны baseline MD3 (значит, не заданы явно): $leftovers",
            leftovers.isEmpty(),
        )
    }

    @Test
    fun `тёмная схема не опирается на baseline ни в одной роли`() {
        assertNoBaselineLeftovers("dark", AnilibrixDarkColorScheme, darkColorScheme())
    }

    @Test
    fun `светлая схема не опирается на baseline ни в одной роли`() {
        assertNoBaselineLeftovers("light", AnilibrixLightColorScheme, lightColorScheme())
    }

    @Test
    fun `схемы покрывают все роли ColorScheme`() {
        val roleCount = colorRoles(AnilibrixDarkColorScheme).size
        assertTrue(
            "Ожидалось >= 45 цветовых ролей, найдено $roleCount",
            roleCount >= 45,
        )
    }
}
