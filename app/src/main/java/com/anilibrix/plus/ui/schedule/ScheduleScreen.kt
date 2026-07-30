package com.anilibrix.plus.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import com.anilibrix.plus.ui.theme.Sizing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anilibrix.plus.ui.components.EmptyKind
import com.anilibrix.plus.ui.components.EmptyState
import com.anilibrix.plus.ui.components.ErrorView
import com.anilibrix.plus.ui.components.ScreenState
import com.anilibrix.plus.ui.components.ScreenStateHost
import com.anilibrix.plus.ui.components.ShimmerHost
import com.anilibrix.plus.ui.components.TitleCardGrid
import com.anilibrix.plus.ui.components.TitleCardGridSkeleton
import com.anilibrix.plus.ui.components.rememberHaptics
import com.anilibrix.plus.ui.navigation.screenContentPadding
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.theme.Spacing
import kotlinx.coroutines.launch

/** «Вышла серия 7 · дальше 8-я» — одна строка вместо двух неочевидных чисел. */
private fun com.anilibrix.plus.domain.model.ScheduleEntry.episodeLabel(): String? {
    val published = publishedEpisode
    val next = nextEpisode
    return when {
        published != null && next != null -> "Вышла серия $published · дальше $next-я"
        published != null -> "Вышла серия $published"
        next != null -> "Ожидается серия $next"
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
    onTitleClick: (Long) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    val days = state.days
    val pagerState = rememberPagerState(
        initialPage = state.selectedDayIndex.coerceAtLeast(0),
        pageCount = { days.size },
    )

    // Свой скролл на каждый день: раньше все дни делили одно состояние
    // и позиция прыгала при переключении.
    val dayGridStates = remember(days.size) { List(days.size) { LazyGridState() } }

    // Дни приходят асинхронно, поэтому initialPage на первой композиции ещё
    // не знает сегодняшнего индекса — доезжаем до него, когда данные пришли.
    var jumpedToToday by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(days.size, state.selectedDayIndex) {
        if (!jumpedToToday && days.isNotEmpty()) {
            jumpedToToday = true
            pagerState.scrollToPage(state.selectedDayIndex.coerceIn(days.indices))
        }
    }

    // Пейджер — источник правды о выбранном дне; ViewModel синхронизируется следом.
    LaunchedEffect(pagerState, days.size) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page in days.indices && page != state.selectedDayIndex) {
                viewModel.onIntent(ScheduleIntent.SelectDay(page))
                haptics.tick()
            }
        }
    }

    val screenState = when {
        state.loading && days.isEmpty() -> ScreenState.Loading
        state.error != null && days.isEmpty() -> ScreenState.Error
        days.isEmpty() -> ScreenState.Empty
        else -> ScreenState.Content
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Расписание") },
                    actions = {
                        // Расписание Anilibria — весь сезон; без фильтра свои
                        // тайтлы приходится искать глазами среди сорока чужих.
                        FilterChip(
                            selected = state.onlyTracked,
                            onClick = { viewModel.onIntent(ScheduleIntent.ToggleOnlyTracked) },
                            label = { Text("Только моё") },
                            leadingIcon = if (state.onlyTracked) {
                                {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(Sizing.iconSm),
                                    )
                                }
                            } else {
                                null
                            },
                            modifier = Modifier.padding(end = Spacing.sm),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
                if (days.isNotEmpty()) {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage
                            .coerceIn(0, maxOf(0, days.size - 1)),
                        edgePadding = Spacing.screenHorizontal,
                    ) {
                        days.forEachIndexed { index, day ->
                            Tab(
                                selected = index == pagerState.currentPage,
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(
                                            page = index,
                                            animationSpec = MotionTokens.spatialDefault(),
                                        )
                                    }
                                },
                                text = {
                                    Text(
                                        text = day.day,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        ScreenStateHost(
            state = screenState,
            modifier = Modifier.fillMaxSize(),
            onRetry = { viewModel.onIntent(ScheduleIntent.Load) },
            loading = {
                ShimmerHost {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = screenContentPadding(innerPadding),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.gridGutter),
                        verticalArrangement = Arrangement.spacedBy(Spacing.gridGutter),
                        userScrollEnabled = false,
                    ) {
                        items(6) { TitleCardGridSkeleton() }
                    }
                }
            },
            error = {
                ErrorView(
                    message = state.error ?: "Ошибка загрузки",
                    onRetry = { viewModel.onIntent(ScheduleIntent.Load) },
                )
            },
            empty = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(kind = EmptyKind.Schedule)
                }
            },
        ) {
            // Свайп между днями — раньше день можно было сменить только тапом по табу.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val day = days[page]
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = { viewModel.onIntent(ScheduleIntent.Refresh) },
                    // Раньше modifier не передавался, и контейнер не занимал экран.
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val entries = state.entriesFor(page)
                    if (entries.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(
                                kind = EmptyKind.Schedule,
                                title = if (state.onlyTracked) {
                                    "Из ваших ничего не выходит"
                                } else {
                                    EmptyKind.Schedule.defaultTitle
                                },
                                subtitle = if (state.onlyTracked) {
                                    "Снимите фильтр «Только моё», чтобы увидеть весь день"
                                } else {
                                    EmptyKind.Schedule.defaultSubtitle
                                },
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = dayGridStates[page],
                            modifier = Modifier.fillMaxSize(),
                            // Все поля — в contentPadding. Раньше горизонтальная
                            // часть стояла в Modifier.padding и подрезала
                            // скроллящийся контент вместо того, чтобы дать ему
                            // уходить под край.
                            contentPadding = screenContentPadding(innerPadding),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.gridGutter),
                            verticalArrangement = Arrangement.spacedBy(Spacing.gridGutter),
                        ) {
                            items(items = entries, key = { it.title.id }) { entry ->
                                Column(
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = MotionTokens.effectsDefault(),
                                        placementSpec = MotionTokens.spatialDefault(),
                                        fadeOutSpec = MotionTokens.effectsFast(),
                                    ),
                                ) {
                                    TitleCardGrid(
                                        title = entry.title,
                                        isFavorite = entry.title.id in state.trackedIds,
                                        onClick = { onTitleClick(entry.title.id) },
                                    )
                                    // Номера серий приезжали из API с самого
                                    // начала, но не отрисовывались нигде:
                                    // расписание показывало только «что-то
                                    // сегодня выходит».
                                    entry.episodeLabel()?.let { label ->
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = Spacing.xs),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
