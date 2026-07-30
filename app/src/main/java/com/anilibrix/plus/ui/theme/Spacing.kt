package com.anilibrix.plus.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Отступы. Намеренно плоский `object`, а не CompositionLocal.
 *
 * В проекте ~250 сырых .dp-литералов. Текстовая замена на токен возможна только
 * если токен — обычный `val`: он подставляется внутри PaddingValues, в
 * не-composable хелперах, в дефолтах data class. CompositionLocal читается
 * только из @Composable и заблокировал бы большую часть этих мест.
 *
 * Отступы, в отличие от [AnilibrixExtendedColors], не зависят от темы —
 * они зависят от размера окна, а это другая ось.
 */
object Spacing {
    val none = 0.dp
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp

    /** Боковые поля экрана. */
    val screenHorizontal = lg
    /** Расстояние между секциями. */
    val sectionGap = xl
    /** Между карточками в сетке. */
    val gridGutter = md
    /** Между строками списка. */
    val listItemGap = sm
}

/** Уровни elevation MD3. */
object Elevation {
    val level0 = 0.dp
    val level1 = 1.dp
    val level2 = 3.dp
    val level3 = 6.dp
    val level4 = 8.dp
    val level5 = 12.dp
}

/**
 * Размеры.
 *
 * Важно: `.dp` внутри size()/height()/width() — это Sizing, а НЕ [Spacing].
 * Не смешивать при миграции литералов.
 */
object Sizing {
    /** Минимальный тач-таргет MD3. */
    val touchTarget = 48.dp

    val iconXs = 14.dp
    val iconSm = 16.dp
    val iconMd = 24.dp
    val iconLg = 32.dp
    val iconXl = 48.dp
    /** Иконка в пустом состоянии. */
    val iconEmptyState = 64.dp

    val avatarSm = 40.dp
    val avatarMd = 60.dp
    val avatarLg = 96.dp

    val heroHeight = 240.dp
    val detailHeaderHeight = 280.dp
    val tabRowHeight = 48.dp

    /** Ширина карточки в горизонтальной рейке. */
    val railCardWidth = 150.dp
    /** Миниатюра в строке списка. */
    val listThumbWidth = 80.dp
    val listThumbHeight = 112.dp

    /** Стандартное соотношение сторон постера аниме (2:3). */
    const val POSTER_ASPECT = 2f / 3f
}
