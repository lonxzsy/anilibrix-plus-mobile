package com.anilibrix.plus.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

/**
 * Тема приложения.
 *
 * `MaterialExpressiveTheme` использовать нельзя — в material3 1.4.0 он
 * `internal`. Expressive-motion приходит из [MotionTokens], где пружины заданы
 * теми же числами, что в `ExpressiveMotionTokens` этой же библиотеки.
 *
 * У [darkTheme] намеренно НЕТ значения по умолчанию, а [dynamicColor] по
 * умолчанию выключен. Раньше было наоборот (`isSystemInDarkTheme()` и `true`),
 * и поскольку minSdk = 33, ветка dynamic срабатывала всегда — вся фирменная
 * палитра оказывалась недостижимым кодом. Обязательный параметр заставляет
 * каждое место вызова заявить намерение явно.
 */
@Composable
fun AnilibrixTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AnilibrixDarkColorScheme
        else -> AnilibrixLightColorScheme
    }

    CompositionLocalProvider(
        LocalExtendedColors provides if (darkTheme) ExtendedDarkColors else ExtendedLightColors,
        LocalReducedMotion provides rememberSystemReducedMotion(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = AnilibrixShapes,
            typography = AnilibrixTypography,
            content = content,
        )
    }
}

/**
 * Обёртка для `@Preview`: следует системной теме, чтобы превью светлой и тёмной
 * можно было ставить парой, не дублируя аргументы.
 */
@Composable
fun AnilibrixThemePreview(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) = AnilibrixTheme(darkTheme = darkTheme, dynamicColor = false, content = content)
