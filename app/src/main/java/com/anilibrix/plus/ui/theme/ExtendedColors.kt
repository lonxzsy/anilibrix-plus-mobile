package com.anilibrix.plus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Семантические цвета, которых нет в [androidx.compose.material3.ColorScheme].
 *
 * ColorScheme — финальный класс с 48 фиксированными слотами, поэтому семантика
 * вроде success/warning/info живёт сбоку. Здесь CompositionLocal уместен
 * (в отличие от [Spacing]): значения меняются вместе с активной схемой.
 *
 * Все пары «on-роль + её контейнер» проверены на контраст — минимум AA, фактически AAA.
 */
@Immutable
data class AnilibrixExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    /** Золотой для звёзд и оценок. Заменяет хардкод Color(0xFFFFC107) в 4 местах. */
    val rating: Color,
    /** Раздача/скачивание в списке торрентов. */
    val seeders: Color,
    val leechers: Color,
    /**
     * Тёмная подложка поверх медиа — обвязка плеера, бейджи на постере,
     * градиент под текстом. Всегда тёмная независимо от темы приложения
     * (контент под ней — чужая картинка), поэтому это настоящие токены,
     * а не роли схемы.
     */
    val mediaScrim: Color,
    val onMediaScrim: Color,
    /** Полупрозрачные вуали поверх постеров. */
    val glassOverlay: Color,
    val glassBorder: Color,
    /** Точки градиента под текстом на постере — см. AnilibrixBrushes.heroOverlay. */
    val scrimGradientTop: Color,
    val scrimGradientMid: Color,
    val scrimGradientBottom: Color,
)

internal val ExtendedDarkColors = AnilibrixExtendedColors(
    success = Color(0xFF97D5A6),
    onSuccess = Color(0xFF003918),
    successContainer = Color(0xFF005227),
    onSuccessContainer = Color(0xFFBDEEC8),
    warning = Color(0xFFEAC07F),
    onWarning = Color(0xFF422D00),
    warningContainer = Color(0xFF5F4100),
    onWarningContainer = Color(0xFFFFDEAE),
    info = Color(0xFF84D7FF),
    onInfo = Color(0xFF003547),
    infoContainer = Color(0xFF004D66),
    onInfoContainer = Color(0xFFC0E8FF),
    rating = Color(0xFFFFC864),
    seeders = Color(0xFF97D5A6),
    leechers = Color(0xFFEAC07F),
    mediaScrim = Color(0xB3000000),
    onMediaScrim = Color(0xFFFFFFFF),
    glassOverlay = Color(0x14FFFFFF),
    glassBorder = Color(0x1FFFFFFF),
    scrimGradientTop = Color(0x00000000),
    scrimGradientMid = Color(0x40000000),
    scrimGradientBottom = Color(0xE6000000),
)

internal val ExtendedLightColors = AnilibrixExtendedColors(
    success = Color(0xFF0C6D38),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFBDEEC8),
    onSuccessContainer = Color(0xFF00260C),
    warning = Color(0xFF7D5800),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFDEAE),
    onWarningContainer = Color(0xFF2B1D00),
    info = Color(0xFF005976),
    onInfo = Color(0xFFFFFFFF),
    infoContainer = Color(0xFFC0E8FF),
    onInfoContainer = Color(0xFF09222D),
    rating = Color(0xFF8A6300),
    seeders = Color(0xFF0C6D38),
    leechers = Color(0xFF7D5800),
    // Плеер остаётся тёмным и в светлой теме — видео всегда на чёрном.
    mediaScrim = Color(0xB3000000),
    onMediaScrim = Color(0xFFFFFFFF),
    glassOverlay = Color(0x0F000000),
    glassBorder = Color(0x14000000),
    scrimGradientTop = Color(0x00000000),
    scrimGradientMid = Color(0x33000000),
    scrimGradientBottom = Color(0xCC000000),
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedDarkColors }

/** `MaterialTheme.extended.rating` и т.п. */
val MaterialTheme.extended: AnilibrixExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

/**
 * Цвета обвязки плеера.
 *
 * Плеер всегда тёмный поверх видео независимо от темы приложения, поэтому это
 * константы, а не роли схемы. Раньше по файлам плеера были разбросаны
 * `Color(0x80000000)`, `0x99000000`, `0xCC000000`, `0xDD000000` и `Color.White`.
 */
object AnilibrixPlayerColors {
    val scrim = Color(0x80000000)
    val scrimStrong = Color(0xCC000000)
    val onScrim = Color(0xFFFFFFFF)
}
