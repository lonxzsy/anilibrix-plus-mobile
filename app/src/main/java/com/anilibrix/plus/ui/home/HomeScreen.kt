package com.anilibrix.plus.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.ui.components.AnilibrixImage
import com.anilibrix.plus.ui.components.EmptyKind
import com.anilibrix.plus.ui.components.EmptyState
import com.anilibrix.plus.ui.components.ErrorView
import com.anilibrix.plus.ui.components.HeroCarousel
import com.anilibrix.plus.ui.components.HeroSkeleton
import com.anilibrix.plus.ui.components.RailSkeleton
import com.anilibrix.plus.ui.components.ScreenState
import com.anilibrix.plus.ui.components.ScreenStateHost
import com.anilibrix.plus.ui.components.SectionHeader
import com.anilibrix.plus.ui.components.ShimmerHost
import com.anilibrix.plus.ui.components.TitleCardGrid
import com.anilibrix.plus.ui.components.TitleCardGridSkeleton
import com.anilibrix.plus.ui.navigation.screenContentPadding
import com.anilibrix.plus.ui.theme.AnilibrixShapeExtras
import com.anilibrix.plus.ui.theme.Elevation
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing
import com.anilibrix.plus.ui.theme.extended

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onTitleClick: (Long) -> Unit = {},
    onSearchClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    // enterAlways, а не Large: 240dp hero — визуальный якорь экрана,
    // крупный топбар утопил бы его под сгиб.
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val screenState = when {
        state.loading -> ScreenState.Loading
        state.error != null && state.heroItems.isEmpty() -> ScreenState.Error
        state.heroItems.isEmpty() && state.recentUpdates.isEmpty() &&
            state.recommended.isEmpty() -> ScreenState.Empty
        else -> ScreenState.Content
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Anilibrix+") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Rounded.Search, contentDescription = "Поиск")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    // Прозрачный в покое — hero виден целиком; поверхность
                    // появляется только когда под баром реально есть контент.
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        // Экран сам владеет верхним и боковыми инсетами — см. контракт в AppInsets.kt.
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        ScreenStateHost(
            state = screenState,
            modifier = Modifier.fillMaxSize(),
            onRetry = { viewModel.onIntent(HomeIntent.Load) },
            loading = { HomeLoading(contentPadding = innerPadding) },
            error = {
                ErrorView(
                    message = state.error ?: "Ошибка загрузки",
                    onRetry = { viewModel.onIntent(HomeIntent.Load) },
                )
            },
            empty = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(kind = EmptyKind.Home)
                }
            },
        ) {
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.onIntent(HomeIntent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyVerticalGrid(
                    // Настоящая сетка вместо `recentUpdates.take(6).chunked(2)`
                    // с ручными Row: возвращает переиспользование ячеек, снимает
                    // жёсткий лимит в 6 элементов и хрупкий составной key.
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = screenContentPadding(innerPadding),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.gridGutter),
                    verticalArrangement = Arrangement.spacedBy(Spacing.gridGutter),
                ) {
                    // Боковые поля уже заданы contentPadding сетки, поэтому
                    // вложенным секциям передаём ноль — иначе отступ удвоится.
                    fullWidth {
                        HeroCarousel(
                            items = state.heroItems,
                            onItemClick = { onTitleClick(it.id) },
                            horizontalPadding = 0.dp,
                        )
                    }

                    if (state.continueWatching.isNotEmpty()) {
                        fullWidth {
                            SectionHeader(
                                title = "Продолжить просмотр",
                                horizontalPadding = 0.dp,
                            )
                        }
                        fullWidth {
                            ContinueWatchingRail(
                                items = state.continueWatching,
                                onItemClick = onTitleClick,
                            )
                        }
                    }

                    if (state.recommended.isNotEmpty()) {
                        fullWidth {
                            SectionHeader(title = "Рекомендуем", horizontalPadding = 0.dp)
                        }
                        fullWidth {
                            RecommendedRail(
                                items = state.recommended,
                                onItemClick = onTitleClick,
                            )
                        }
                    }

                    // Подборка по локальной истории. Подпись объясняет, откуда
                    // она взялась: непрозрачные рекомендации люди игнорируют,
                    // а объяснённые — проверяют и кликают.
                    if (!state.personal.isEmpty) {
                        fullWidth {
                            SectionHeader(
                                title = "Вам может понравиться",
                                subtitle = state.personal.reason().takeIf { it.isNotBlank() },
                                horizontalPadding = 0.dp,
                            )
                        }
                        fullWidth {
                            RecommendedRail(
                                items = state.personal.titles,
                                onItemClick = onTitleClick,
                            )
                        }
                    }

                    if (state.recentUpdates.isNotEmpty()) {
                        fullWidth {
                            SectionHeader(title = "Недавние обновления", horizontalPadding = 0.dp)
                        }
                        items(
                            items = state.recentUpdates,
                            key = { it.id },
                        ) { title ->
                            TitleCardGrid(
                                title = title,
                                isFavorite = title.id in state.favoriteIds,
                                onClick = { onTitleClick(title.id) },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = MotionTokens.effectsDefault(),
                                    placementSpec = MotionTokens.spatialDefault(),
                                    fadeOutSpec = MotionTokens.effectsFast(),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Скелетон главной внутри настоящей обвязки экрана: топбар уже отрисован,
 * поэтому подмена на контент не даёт скачка. Раньше здесь была полноэкранная
 * подмена всего экрана.
 */
@Composable
private fun HomeLoading(contentPadding: PaddingValues) {
    ShimmerHost {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            HeroSkeleton()
            SectionHeader(title = "Рекомендуем")
            RailSkeleton()
            SectionHeader(title = "Недавние обновления")
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(Spacing.gridGutter),
                userScrollEnabled = false,
            ) {
                items(2) {
                    TitleCardGridSkeleton(Modifier.width(160.dp))
                }
            }
        }
    }
}

@Composable
private fun RecommendedRail(
    items: List<com.anilibrix.plus.domain.model.Title>,
    onItemClick: (Long) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.gridGutter),
    ) {
        items(items = items, key = { it.id }) { title ->
            TitleCardGrid(
                title = title,
                onClick = { onItemClick(title.id) },
                modifier = Modifier
                    .width(Sizing.railCardWidth)
                    .animateItem(
                        fadeInSpec = MotionTokens.effectsDefault(),
                        placementSpec = MotionTokens.spatialDefault(),
                        fadeOutSpec = MotionTokens.effectsFast(),
                    ),
            )
        }
    }
}

@Composable
private fun ContinueWatchingRail(
    items: List<HistoryEntry>,
    onItemClick: (Long) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.gridGutter),
    ) {
        items(items = items, key = { it.titleId }) { entry ->
            ContinueWatchingCard(
                entry = entry,
                onClick = { onItemClick(entry.titleId) },
                modifier = Modifier
                    .width(Sizing.railCardWidth)
                    .animateItem(
                        fadeInSpec = MotionTokens.effectsDefault(),
                        placementSpec = MotionTokens.spatialDefault(),
                        fadeOutSpec = MotionTokens.effectsFast(),
                    ),
            )
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    entry: HistoryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (entry.duration > 0) {
        entry.timestamp.toFloat() / entry.duration
    } else {
        0f
    }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0),
    ) {
        Column {
            Box {
                AnilibrixImage(
                    model = entry.posterUrl,
                    contentDescription = entry.titleName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Sizing.railCardWidth * 1.4f),
                    shape = AnilibrixShapeExtras.topOnlyMedium,
                )
                LinearProgressIndicator(
                    progress = { if (progress.isFinite() && progress > 0f) progress else 0f },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.extended.onMediaScrim.copy(alpha = 0.3f),
                )
            }
            Column(modifier = Modifier.padding(Spacing.sm)) {
                Text(
                    text = entry.titleName,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Серия ${entry.episodeNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Секция во всю ширину сетки — чтобы не плодить `span = { GridItemSpan(maxLineSpan) }`. */
private fun androidx.compose.foundation.lazy.grid.LazyGridScope.fullWidth(
    content: @Composable () -> Unit,
) = item(span = { GridItemSpan(maxLineSpan) }) { content() }
