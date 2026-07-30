package com.anilibrix.plus.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Единый motion-язык приложения.
 *
 * Почему свои токены, а не `MaterialTheme.motionScheme`: в material3 1.4.0
 * (последний стабильный) `MotionScheme`, `MaterialExpressiveTheme` и даже
 * аннотация `ExperimentalMaterial3ExpressiveApi` объявлены `internal` —
 * компилятор их не пускает. Публичными они станут только с 1.5.
 *
 * Поэтому числа ниже **извлечены из самого артефакта**
 * (`androidx.compose.material3.tokens.ExpressiveMotionTokens` в material3 1.4.0),
 * а не подобраны на глаз: наш motion численно идентичен expressive-схеме Google.
 * Когда 1.5 станет стабильной, этот объект превращается в фасад над
 * `MaterialTheme.motionScheme`, и ни одно место использования не меняется.
 *
 * Правило: **spatial** — всё, что двигается или меняет размер (пружинит
 * намеренно); **effects** — alpha и цвет (никогда не пружинит, прыгающая
 * непрозрачность выглядит сломанной).
 *
 * До этого в проекте было 16 анимаций на всё приложение, 13 из 14 tween'ов
 * без easing, и все четыре AnimatedVisibility без animationSpec.
 */
object MotionTokens {

    // --- Easing (точные кривые MD3) ---
    val standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val standardAccelerate = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    val standardDecelerate = CubicBezierEasing(0f, 0f, 0f, 1f)
    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    // --- Длительности MD3 (мс) ---
    const val SHORT_1 = 50
    const val SHORT_2 = 100
    const val SHORT_3 = 150
    const val SHORT_4 = 200
    const val MEDIUM_1 = 250
    const val MEDIUM_2 = 300
    const val MEDIUM_3 = 350
    const val MEDIUM_4 = 400
    const val LONG_1 = 450
    const val LONG_2 = 500
    const val EXTRA_LONG_1 = 700

    /** Проявление картинки после загрузки. */
    const val IMAGE_CROSSFADE = MEDIUM_2

    /** Период прохода shimmer-блика. */
    const val SHIMMER_PERIOD = 1200

    // --- Пружины: значения ExpressiveMotionTokens из material3 1.4.0 ---

    private const val SPATIAL_FAST_DAMPING = 0.6f
    private const val SPATIAL_FAST_STIFFNESS = 800f
    private const val SPATIAL_DEFAULT_DAMPING = 0.8f
    private const val SPATIAL_DEFAULT_STIFFNESS = 380f
    private const val SPATIAL_SLOW_DAMPING = 0.8f
    private const val SPATIAL_SLOW_STIFFNESS = 200f

    private const val EFFECTS_FAST_DAMPING = 1f
    private const val EFFECTS_FAST_STIFFNESS = 3800f
    private const val EFFECTS_DEFAULT_DAMPING = 1f
    private const val EFFECTS_DEFAULT_STIFFNESS = 1600f
    private const val EFFECTS_SLOW_DAMPING = 1f
    private const val EFFECTS_SLOW_STIFFNESS = 800f

    /** Быстрое движение: нажатие, мелкие сдвиги. */
    fun <T> spatialFast(): FiniteAnimationSpec<T> =
        spring(SPATIAL_FAST_DAMPING, SPATIAL_FAST_STIFFNESS)

    /** Основное движение: появление элементов, переходы, layout. */
    fun <T> spatialDefault(): FiniteAnimationSpec<T> =
        spring(SPATIAL_DEFAULT_DAMPING, SPATIAL_DEFAULT_STIFFNESS)

    /** Крупные «весомые» перемещения: shared element, сворачивание хедера. */
    fun <T> spatialSlow(): FiniteAnimationSpec<T> =
        spring(SPATIAL_SLOW_DAMPING, SPATIAL_SLOW_STIFFNESS)

    fun <T> effectsFast(): FiniteAnimationSpec<T> =
        spring(EFFECTS_FAST_DAMPING, EFFECTS_FAST_STIFFNESS)

    fun <T> effectsDefault(): FiniteAnimationSpec<T> =
        spring(EFFECTS_DEFAULT_DAMPING, EFFECTS_DEFAULT_STIFFNESS)

    fun <T> effectsSlow(): FiniteAnimationSpec<T> =
        spring(EFFECTS_SLOW_DAMPING, EFFECTS_SLOW_STIFFNESS)

    // --- Навигация ---
    // Входящий элемент замедляется (decelerate), уходящий ускоряется (accelerate).
    // Раньше здесь были голые tween(300)/tween(400) с дефолтным easing.

    fun <T> navEnter(): FiniteAnimationSpec<T> =
        tween(MEDIUM_4, easing = emphasizedDecelerate)

    fun <T> navExit(): FiniteAnimationSpec<T> =
        tween(MEDIUM_2, easing = emphasizedAccelerate)
}
