package com.anilibrix.plus.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.anilibrix.plus.ui.theme.AnilibrixShapeExtras
import com.anilibrix.plus.ui.theme.MotionTokens

/**
 * Прогресс блика, общий для всех скелетонов внутри [ShimmerHost].
 *
 * `null` означает «хоста нет» — тогда [shimmer] заводит собственную анимацию,
 * чтобы одиночный скелетон вне хоста всё равно работал.
 */
val LocalShimmerProgress = compositionLocalOf<State<Float>?> { null }

/**
 * Один источник анимации на всё поддерево.
 *
 * Раньше каждый `ShimmerBox` заводил свой `rememberInfiniteTransition`:
 * в одной строке их было 12, и они расфазировались за секунду — блики
 * «плыли» вразнобой. Здесь анимация одна, поэтому все скелетоны идут синхронно.
 */
@Composable
fun ShimmerHost(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // Restart, а не Reverse: блик проходит в одну сторону.
            // Reverse давал механическую «треугольную» пульсацию туда-сюда.
            animation = tween(MotionTokens.SHIMMER_PERIOD, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    CompositionLocalProvider(LocalShimmerProgress provides progress, content = content)
}

/**
 * Скелетон-заглушка с бегущим бликом.
 *
 * Прежняя реализация мигала прозрачностью поверх СТАТИЧНОГО градиента — то есть
 * была пульсацией, а не shimmer'ом. Здесь градиент реально едет по горизонтали.
 *
 * Прогресс читается внутри `onDrawBehind`, то есть на этапе отрисовки:
 * рекомпозиции на кадр не происходит вообще.
 */
@Composable
fun Modifier.shimmer(shape: Shape = AnilibrixShapeExtras.poster): Modifier {
    val hosted = LocalShimmerProgress.current
    val progress = hosted ?: run {
        val transition = rememberInfiniteTransition(label = "shimmerStandalone")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(MotionTokens.SHIMMER_PERIOD, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shimmerStandaloneProgress",
        )
    }

    // Берём цвета из схемы: прежний shimmerBase = 0x33FFFFFF был белой вуалью
    // и на светлой теме становился невидимым.
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest

    return this
        .clip(shape)
        .drawWithCache {
            val band = size.width * 0.55f
            val travel = size.width + band * 2f
            onDrawBehind {
                drawRect(base)
                val x = travel * progress.value - band
                drawRect(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.5f to highlight,
                            1f to Color.Transparent,
                        ),
                        start = Offset(x - band, 0f),
                        end = Offset(x + band, 0f),
                    )
                )
            }
        }
}
