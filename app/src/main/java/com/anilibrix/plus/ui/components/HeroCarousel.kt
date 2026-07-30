package com.anilibrix.plus.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.ui.theme.AnilibrixBrushes
import com.anilibrix.plus.ui.theme.AnilibrixShapeExtras
import com.anilibrix.plus.ui.theme.AnilibrixTypeExtras
import com.anilibrix.plus.ui.theme.Elevation
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing
import com.anilibrix.plus.ui.theme.extended
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import com.anilibrix.plus.ui.theme.LocalReducedMotion

private const val AUTO_ADVANCE_MS = 8000L

/**
 * Hero-карусель на главной.
 *
 * Главное изменение — **масштаб и параллакс следуют за пальцем**.
 * Раньше было `animateFloatAsState(if (pagerState.currentPage == page) 1f else 0.96f)`,
 * то есть анимация висела на *устоявшейся* странице: во время перетаскивания
 * ничего не происходило, а в момент фиксации масштаб щёлкал. Теперь обе
 * величины считаются из `currentPageOffsetFraction` внутри `graphicsLayer`,
 * то есть читаются на этапе ОТРИСОВКИ — за кадр не происходит ни одной
 * рекомпозиции.
 *
 * Картинка внутри карточки едет медленнее самой карточки (0.35 от смещения) —
 * это и даёт ощущение глубины.
 */
@Composable
fun HeroCarousel(
    items: List<Title>,
    onItemClick: (Title) -> Unit,
    modifier: Modifier = Modifier,
    /** Ноль, если боковые поля уже заданы контейнером-родителем. */
    horizontalPadding: Dp = Spacing.screenHorizontal,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })
    val currentItems by rememberUpdatedState(items)

    // Автопрокрутка не должна мешать пользователю и не должна работать вхолостую:
    // пауза на время жеста, пауза когда экран не на переднем плане,
    // и полное отключение при включённом «уменьшении анимации».
    val lifecycleOwner = LocalLifecycleOwner.current
    val reduceMotion = LocalReducedMotion.current

    LaunchedEffect(pagerState, items.size, reduceMotion) {
        if (items.size <= 1 || reduceMotion) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(AUTO_ADVANCE_MS)
                if (pagerState.isScrollInProgress) continue
                val next = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(
                    page = next,
                    animationSpec = MotionTokens.spatialSlow(),
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizing.heroHeight),
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            pageSpacing = Spacing.md,
            beyondViewportPageCount = 1,
        ) { page ->
            val item = currentItems[page]

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val offset = pageOffset(pagerState.currentPage, page, pagerState.currentPageOffsetFraction)
                        val proximity = 1f - offset.absoluteValue
                        val scale = lerp(0.90f, 1f, proximity)
                        scaleX = scale
                        scaleY = scale
                        alpha = lerp(0.55f, 1f, proximity)
                    }
                    .pressScale(0.98f),
                shape = AnilibrixShapeExtras.hero,
                onClick = { onItemClick(item) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0),
            ) {
                Box {
                    AnilibrixImage(
                        model = item.poster?.cardUrl,
                        contentDescription = item.name.main,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val offset = pageOffset(
                                    pagerState.currentPage,
                                    page,
                                    pagerState.currentPageOffsetFraction,
                                )
                                // Картинка отстаёт от карточки — эффект глубины.
                                translationX = offset * size.width * 0.35f
                            },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AnilibrixBrushes.heroOverlay)
                    )
                    HeroMeta(
                        item = item,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(Spacing.lg),
                    )
                }
            }
        }

        PagerDots(
            count = items.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.sm),
        )
    }
}

/** Смещение страницы относительно текущей, в диапазоне [-1, 1]. */
private fun pageOffset(currentPage: Int, page: Int, fraction: Float): Float =
    ((currentPage - page) + fraction).coerceIn(-1f, 1f)

@Composable
private fun HeroMeta(item: Title, modifier: Modifier = Modifier) {
    val onScrim = MaterialTheme.extended.onMediaScrim
    Column(modifier = modifier) {
        Text(
            text = item.name.main,
            style = AnilibrixTypeExtras.titleLargeEmphasized,
            color = onScrim,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (item.type != null) {
                Text(
                    text = item.type.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = onScrim.copy(alpha = 0.8f),
                )
            }
            if (item.year > 0) {
                Text(
                    text = item.year.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = onScrim.copy(alpha = 0.8f),
                )
            }
            if (item.score != null) {
                Text(
                    text = String.format("%.1f", item.score),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.extended.rating,
                )
            }
        }
    }
}

/**
 * Индикатор страниц.
 *
 * Активная точка вытягивается в пилюлю, ширина и цвет анимируются. Раньше
 * точки мгновенно переключались между двумя размерами `size()`.
 */
@Composable
private fun PagerDots(
    count: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (selected) 20.dp else 6.dp,
                animationSpec = MotionTokens.spatialDefault(),
                label = "dotWidth",
            )
            val color by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.extended.onMediaScrim.copy(alpha = 0.4f)
                },
                animationSpec = MotionTokens.effectsDefault(),
                label = "dotColor",
            )
            Box(
                modifier = Modifier
                    // coerce обязателен: spatial-пружина проскакивает мимо цели,
                    // а Modifier.size на отрицательном значении падает.
                    .size(width = width.coerceAtLeast(0.dp), height = 6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/** Скелетон карусели — форма и радиус совпадают с настоящей карточкой. */
@Composable
fun HeroCarouselShimmer(modifier: Modifier = Modifier) {
    HeroSkeleton(modifier)
}
