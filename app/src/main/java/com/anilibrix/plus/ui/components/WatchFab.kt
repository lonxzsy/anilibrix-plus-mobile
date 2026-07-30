package com.anilibrix.plus.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.anilibrix.plus.ui.theme.MotionTokens

/**
 * Главное действие экрана тайтла — «Смотреть» / «Продолжить · Серия N».
 *
 * Заменяет иконочный [androidx.compose.material3.FloatingActionButton], который
 * в интерфейсе читался как «маленькая квадратная кнопка непонятно чего»:
 * без подписи, без реакции на скролл и без анимации появления.
 *
 * @param expanded обычно `rememberIsScrollingUp(...)`: при листании вниз кнопка
 *   сворачивается в иконку и перестаёт закрывать контент, при листании вверх —
 *   разворачивается обратно. Ширину и текст анимирует сам ExtendedFAB.
 */
@Composable
fun WatchFab(
    label: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(MotionTokens.spatialDefault(), initialScale = 0.8f) +
            fadeIn(MotionTokens.effectsDefault()),
        exit = scaleOut(MotionTokens.spatialFast(), targetScale = 0.8f) +
            fadeOut(MotionTokens.effectsFast()),
        modifier = modifier,
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            expanded = expanded,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                )
            },
            text = { Text(label) },
        )
    }
}
