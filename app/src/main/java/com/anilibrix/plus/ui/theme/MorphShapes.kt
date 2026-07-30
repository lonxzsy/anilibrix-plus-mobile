package com.anilibrix.plus.ui.theme

import android.graphics.Matrix
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * [Shape], форма которой интерполируется между двумя полигонами.
 *
 * Полигоны в [AnilibrixPolygons] построены сразу в единичном боксе
 * (центр 0.5, радиус 0.5), поэтому здесь остаётся только отмасштабировать
 * путь под фактический размер.
 */
class MorphPolygonShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        // Трансформируем android.graphics.Path ДО конвертации в Compose-путь:
        // у Compose Path нет публичного доступа к нижележащему android-пути.
        val androidPath = morph.toPath(progress.coerceIn(0f, 1f))
        androidPath.transform(Matrix().apply { setScale(size.width, size.height) })
        val path: Path = androidPath.asComposePath()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean =
        other is MorphPolygonShape && other.morph === morph && other.progress == progress

    override fun hashCode(): Int = morph.hashCode() * 31 + progress.hashCode()
}

/**
 * Полигоны кэшируются на уровне object: строить [RoundedPolygon] покадрово —
 * единственная реальная ловушка производительности в morph-анимации.
 */
object AnilibrixPolygons {

    /** Спокойная форма — невыбранное состояние. */
    val circle: RoundedPolygon = RoundedPolygon.circle(
        numVertices = 12,
        radius = 0.5f,
        centerX = 0.5f,
        centerY = 0.5f,
    )

    /**
     * Мягкая «печенька» — выбранное состояние индикатора нижней навигации.
     * innerRadius близок к radius, поэтому это не колючая звезда, а
     * волнистый круг.
     */
    val cookie: RoundedPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 8,
        radius = 0.5f,
        innerRadius = 0.42f,
        rounding = CornerRounding(0.25f),
        centerX = 0.5f,
        centerY = 0.5f,
    )

    /** Круг ↔ печенька: индикатор выбранной вкладки. */
    val selectionMorph: Morph = Morph(circle, cookie)

    /** Мягкий четырёхлепестковый «клевер». */
    val clover: RoundedPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 4,
        radius = 0.5f,
        innerRadius = 0.32f,
        rounding = CornerRounding(0.45f),
        centerX = 0.5f,
        centerY = 0.5f,
    )

    /** Скруглённый шестиугольник. */
    val hexagon: RoundedPolygon = RoundedPolygon(
        numVertices = 6,
        radius = 0.5f,
        centerX = 0.5f,
        centerY = 0.5f,
        rounding = CornerRounding(0.3f),
    )

    /**
     * Последовательность превращений для индикатора загрузки.
     *
     * Фигуры подобраны с близким «весом», чтобы перетекание читалось как одна
     * живая форма, а не как подмена картинок.
     */
    val loadingMorphs: List<Morph> = listOf(
        Morph(cookie, clover),
        Morph(clover, hexagon),
        Morph(hexagon, circle),
        Morph(circle, cookie),
    )
}
