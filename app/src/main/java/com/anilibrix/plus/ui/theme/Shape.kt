package com.anilibrix.plus.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Шкала форм MD3: 4 / 8 / 12 / 16 / 28.
 *
 * Раньше здесь было 2/4/6/8/16 — примерно вдвое меньше спеки, из-за чего
 * почти каждый компонент обходил шкалу инлайновым радиусом (сосуществовали
 * 12, 16, 18, 20 dp и хак RoundedCornerShape(999.dp)).
 */
val AnilibrixShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Именованные формы, которых нет в [Shapes] material3 1.4.0
 * (в 1.5 они приедут как Shapes.LargeIncreased / ExtraLargeIncreased / ExtraExtraLarge).
 */
object AnilibrixShapeExtras {
    /** Поглощает существующие 18.dp (TitleCard) и 20.dp (HeroCarousel, ProfileScreen). */
    val largeIncreased = RoundedCornerShape(20.dp)
    val extraLargeIncreased = RoundedCornerShape(32.dp)
    val extraExtraLarge = RoundedCornerShape(48.dp)

    /**
     * Точнее и честнее, чем RoundedCornerShape(999.dp): масштабируется вместе
     * с компонентом и не полагается на клампинг.
     */
    val pill = RoundedCornerShape(percent = 50)

    /** Постеры тайтлов и аватары персонажей. */
    val poster = RoundedCornerShape(12.dp)

    /** Страницы hero-карусели. */
    val hero = RoundedCornerShape(20.dp)

    val bottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val topOnlyMedium = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
}
