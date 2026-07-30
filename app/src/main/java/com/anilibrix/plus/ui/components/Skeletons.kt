package com.anilibrix.plus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anilibrix.plus.ui.theme.AnilibrixShapeExtras
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing

/**
 * Скелетоны загрузки.
 *
 * Единая стратегия на всё приложение: скелетон повторяет **форму реального
 * контента** и живёт внутри настоящей обвязки экрана (топбар, табы и заголовки
 * секций рисуются по-настоящему). Никаких полноэкранных подмен и никаких
 * спиннеров на месте первой загрузки.
 *
 * Раньше сосуществовали три несовместимые реализации: `ShimmerBox` с
 * alpha-пульсацией, `HeroCarouselShimmer` с пульсацией масштаба и
 * `TitleCardShimmer` вообще без анимации — причём с другим радиусом скругления
 * (12 против 18), из-за чего при подстановке настоящей карточки был виден скачок.
 *
 * Оборачивайте экран в [ShimmerHost], чтобы все блики шли синхронно.
 */

@Composable
fun TitleCardGridSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .aspectRatio(Sizing.POSTER_ASPECT)
                .shimmer(MaterialTheme.shapes.large)
        )
        Spacer(Modifier.height(Spacing.sm))
        Spacer(
            Modifier
                .fillMaxWidth(0.85f)
                .height(14.dp)
                .shimmer(MaterialTheme.shapes.extraSmall)
        )
        Spacer(Modifier.height(Spacing.xs))
        Spacer(
            Modifier
                .fillMaxWidth(0.5f)
                .height(12.dp)
                .shimmer(MaterialTheme.shapes.extraSmall)
        )
    }
}

@Composable
fun TitleCardListSkeleton(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        Spacer(
            Modifier
                .size(Sizing.listThumbWidth, Sizing.listThumbHeight)
                .shimmer(AnilibrixShapeExtras.poster)
        )
        Spacer(Modifier.width(Spacing.md))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = Spacing.xs)
        ) {
            Spacer(
                Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .shimmer(MaterialTheme.shapes.extraSmall)
            )
            Spacer(Modifier.height(Spacing.sm))
            Spacer(
                Modifier
                    .fillMaxWidth(0.45f)
                    .height(12.dp)
                    .shimmer(MaterialTheme.shapes.extraSmall)
            )
            Spacer(Modifier.height(Spacing.xs))
            Spacer(
                Modifier
                    .fillMaxWidth(0.3f)
                    .height(12.dp)
                    .shimmer(MaterialTheme.shapes.extraSmall)
            )
        }
    }
}

@Composable
fun HeroSkeleton(modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal)
            .height(Sizing.heroHeight)
            .shimmer(AnilibrixShapeExtras.hero)
    )
}

/** Горизонтальная рейка скелетонов — под «Рекомендуем» и «Продолжить просмотр». */
@Composable
fun RailSkeleton(
    modifier: Modifier = Modifier,
    count: Int = 4,
    cardWidth: Dp = Sizing.railCardWidth,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Spacing.gridGutter),
        userScrollEnabled = false,
    ) {
        items(count) {
            TitleCardGridSkeleton(Modifier.width(cardWidth))
        }
    }
}

@Composable
fun EpisodeRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm)
    ) {
        Spacer(
            Modifier
                .size(120.dp, 68.dp)
                .shimmer(MaterialTheme.shapes.medium)
        )
        Spacer(Modifier.width(Spacing.md))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = Spacing.xs)
        ) {
            Spacer(
                Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .shimmer(MaterialTheme.shapes.extraSmall)
            )
            Spacer(Modifier.height(Spacing.sm))
            Spacer(
                Modifier
                    .fillMaxWidth(0.35f)
                    .height(12.dp)
                    .shimmer(MaterialTheme.shapes.extraSmall)
            )
        }
    }
}
