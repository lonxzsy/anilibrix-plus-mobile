package com.anilibrix.plus.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.anilibrix.plus.ui.theme.MotionTokens
import kotlinx.coroutines.launch
import com.anilibrix.plus.ui.theme.LocalReducedMotion

/**
 * Отклик на нажатие: элемент слегка «проседает» под пальцем.
 *
 * Что было не так в прежней версии и что здесь исправлено:
 *
 * 1. Она была написана через устаревший `composed { }` — такой модификатор
 *    не переиспользуется и не попадает в инспектор.
 * 2. Она применяла `Modifier.scale()`, то есть дёргала **layout** на каждый
 *    кадр. Здесь — лямбда-форма `graphicsLayer`, где чтение `scale.value`
 *    происходит на этапе ОТРИСОВКИ: ни layout-прохода, ни рекомпозиции.
 * 3. Состояние нажатия было `mutableStateOf`, читаемым в композиции, поэтому
 *    **каждое нажатие рекомпозировало всё поддерево карточки**. Теперь оно
 *    живёт в [Animatable] и композиции не касается.
 *
 * Прежний `Modifier.hapticOnPress` (ноль вызовов, legacy View-API) свёрнут
 * сюда параметром [onPress] — см. [AppHaptics].
 *
 * @param onPress вызывается в момент нажатия, например `haptics::tick`.
 */
@Composable
fun Modifier.pressScale(
    pressedScale: Float = 0.96f,
    enabled: Boolean = true,
    onPress: (() -> Unit)? = null,
): Modifier {
    // Системное «уменьшение анимации» должно отключать и микро-отклик.
    if (!enabled || LocalReducedMotion.current) return this

    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            transformOrigin = TransformOrigin.Center
        }
        .pointerInput(pressedScale) {
            awaitEachGesture {
                // requireUnconsumed = false — жест не перехватывается,
                // поэтому модификатор спокойно сочетается с clickable.
                awaitFirstDown(requireUnconsumed = false)
                onPress?.invoke()
                scope.launch { scale.animateTo(pressedScale, MotionTokens.spatialFast()) }
                waitForUpOrCancellation()
                // Возврат мягче нажатия: отпускание читается как отклик,
                // а не как щелчок.
                scope.launch { scale.animateTo(1f, MotionTokens.spatialDefault()) }
            }
        }
}
