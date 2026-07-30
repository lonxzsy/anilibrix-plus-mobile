package com.anilibrix.plus.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Тактильная отдача приложения.
 *
 * До редизайна во всём приложении был ровно ОДИН живой вызов вибро —
 * двойной тап перемотки в плеере, причём через legacy `View.performHapticFeedback`.
 * Утилита `Modifier.hapticOnPress` была написана, но не вызывалась нигде.
 *
 * Здесь используется Compose-API [HapticFeedback], а не View-API: он уважает
 * системные настройки и корректно работает в композиции.
 */
@Immutable
class AppHaptics(
    private val haptics: HapticFeedback,
    private val enabled: Boolean,
) {
    /** Смена сегмента: вкладка, чип, страница пейджера. */
    fun tick() = perform(HapticFeedbackType.SegmentTick)

    /** Частые тики во время жеста: перетаскивание оценки, скраб плеера. */
    fun frequentTick() = perform(HapticFeedbackType.SegmentFrequentTick)

    /** Действие подтверждено: добавлено в избранное, оценка поставлена. */
    fun confirm() = perform(HapticFeedbackType.Confirm)

    /** Действие отклонено или ошибка. */
    fun reject() = perform(HapticFeedbackType.Reject)

    fun toggleOn() = perform(HapticFeedbackType.ToggleOn)

    fun toggleOff() = perform(HapticFeedbackType.ToggleOff)

    /** Жест пересёк порог срабатывания (swipe-to-dismiss, pull-to-refresh). */
    fun threshold() = perform(HapticFeedbackType.GestureThresholdActivate)

    fun longPress() = perform(HapticFeedbackType.LongPress)

    private fun perform(type: HapticFeedbackType) {
        if (enabled) haptics.performHapticFeedback(type)
    }
}

/**
 * @param enabled даёт пользователю выключить вибро целиком — переключатель
 *   «Виброотклик» в профиле; при выключении все вызовы становятся no-op.
 */
@Composable
fun rememberHaptics(enabled: Boolean = true): AppHaptics {
    val haptics = LocalHapticFeedback.current
    return remember(haptics, enabled) { AppHaptics(haptics, enabled) }
}
