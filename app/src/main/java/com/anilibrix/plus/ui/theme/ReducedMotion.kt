package com.anilibrix.plus.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Включено ли системное «уменьшение анимации».
 *
 * До редизайна это не обрабатывалось нигде: приложение анимировало всё
 * независимо от системных настроек доступности, что нарушает пункт
 * motion-чеклиста «duration должны уважать Remove animations».
 *
 * Читаем `ANIMATOR_DURATION_SCALE`: значение 0 означает, что пользователь
 * (или режим энергосбережения) отключил анимации на уровне системы.
 */
val LocalReducedMotion = compositionLocalOf { false }

@Composable
fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}
