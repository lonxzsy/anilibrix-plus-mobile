package com.anilibrix.plus.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.anilibrix.plus.ui.theme.AnilibrixPolygons
import com.anilibrix.plus.ui.theme.LocalReducedMotion
import com.anilibrix.plus.ui.theme.MorphPolygonShape
import com.anilibrix.plus.ui.theme.Sizing

private const val MORPH_CYCLE_MS = 4400

/**
 * Индикатор загрузки в духе MD3 Expressive: фигура непрерывно перетекает
 * между полигонами и медленно вращается.
 *
 * Обычный `CircularProgressIndicator` — крутящаяся дуга — в приложении, где
 * загрузка занимает секунды, читается как «системный» и безликий. Здесь форма
 * живая, но за кадр не происходит ни одной рекомпозиции: и прогресс морфинга,
 * и угол поворота читаются на этапе отрисовки.
 *
 * Форма собирается из `androidx.graphics:graphics-shapes`; полигоны кэшируются
 * в [AnilibrixPolygons] — пересоздавать их покадрово нельзя.
 *
 * При системном «уменьшении анимации» вырождается в статичную фигуру.
 */
@Composable
fun AnilibrixLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = Sizing.iconXl,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val reduceMotion = LocalReducedMotion.current
    val morphs = AnilibrixPolygons.loadingMorphs

    if (reduceMotion) {
        Box(
            modifier = modifier
                .size(size)
                .clip(MorphPolygonShape(morphs.first(), progress = 0f))
                .background(color)
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "loadingIndicator")

    // Один непрерывный прогресс 0..1 на весь цикл; целая часть выбирает пару
    // фигур, дробная — положение между ними. Так переходы стыкуются без рывка.
    val cycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = morphs.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(MORPH_CYCLE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "loadingMorphCycle",
    )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(MORPH_CYCLE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "loadingRotation",
    )

    val index = cycle.toInt().coerceIn(0, morphs.lastIndex)
    val progress = cycle - index

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation }
            .clip(MorphPolygonShape(morphs[index], progress))
            .background(color)
    )
}

/**
 * Мелкий индикатор для футеров догрузки и инлайн-мест, где крупная фигура
 * была бы избыточной.
 */
@Composable
fun AnilibrixLoadingIndicatorSmall(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    AnilibrixLoadingIndicator(
        modifier = modifier,
        size = Sizing.iconMd,
        color = color,
    )
}

/**
 * Детерминированный прогресс остаётся дугой: морфинг подходит для
 * неопределённого ожидания, а не для показа доли выполненного.
 */
@Composable
fun AnilibrixProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    CircularProgressIndicator(progress = progress, modifier = modifier)
}
