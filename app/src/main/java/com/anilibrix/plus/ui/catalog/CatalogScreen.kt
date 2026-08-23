package com.anilibrix.plus.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.foundation.shape.CircleShape
import com.anilibrix.plus.domain.model.ShikimoriCharacterSearchResult
import com.anilibrix.plus.ui.components.AnilibrixImage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anilibrix.plus.domain.model.CatalogSort
import com.anilibrix.plus.domain.model.CatalogStatus
import com.anilibrix.plus.domain.model.ReleaseType
import com.anilibrix.plus.domain.model.SeasonName
import com.anilibrix.plus.ui.components.AnilibrixBottomSheet
import com.anilibrix.plus.ui.components.EmptyKind
import com.anilibrix.plus.ui.components.EmptyState
import com.anilibrix.plus.ui.components.ErrorView
import com.anilibrix.plus.ui.components.FilterChipsRow
import com.anilibrix.plus.ui.components.FilterOption
import com.anilibrix.plus.ui.components.ScreenState
import com.anilibrix.plus.ui.components.ScreenStateHost
import com.anilibrix.plus.ui.components.ShimmerHost
import com.anilibrix.plus.ui.components.TitleCardGrid
import com.anilibrix.plus.ui.components.TitleCardGridSkeleton
import com.anilibrix.plus.ui.components.TitleCardList
import com.anilibrix.plus.ui.components.TitleCardListSkeleton
import com.anilibrix.plus.ui.components.rememberHaptics
import com.anilibrix.plus.ui.components.rememberSheetDismiss
import com.anilibrix.plus.ui.navigation.LocalBottomBarHeight
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.time.Year
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.rounded.History
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.zIndex
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.anilibrix.plus.ui.components.shimmer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel = hiltViewModel(),
    onTitleClick: (Long) -> Unit = {},
    onNavigateToStudioSearch: (String) -> Unit = {},
    onCharacterClick: (Long) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val haptics = rememberHaptics()

    var searchText by remember { mutableStateOf(state.filter.search) }
    var searchExpanded by remember { mutableStateOf(false) }
    var showFiltersSheet by remember { mutableStateOf(false) }
    var contextMenuTitle by remember { mutableStateOf<com.anilibrix.plus.domain.model.Title?>(null) }

    LaunchedEffect(state.filter.search) {
        if (searchText != state.filter.search) searchText = state.filter.search
    }

    PaginationEffect(
        active = state.filter.viewMode == ViewMode.GRID,
        totalItems = { gridState.layoutInfo.totalItemsCount },
        lastVisibleIndex = { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index },
        canLoadMore = { state.hasMore && !state.loadingMore },
        onLoadMore = { viewModel.onIntent(CatalogIntent.LoadMore) },
    )
    PaginationEffect(
        active = state.filter.viewMode == ViewMode.LIST,
        totalItems = { listState.layoutInfo.totalItemsCount },
        lastVisibleIndex = { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index },
        canLoadMore = { state.hasMore && !state.loadingMore },
        onLoadMore = { viewModel.onIntent(CatalogIntent.LoadMore) },
    )

    val screenState = when {
        state.loading && state.titles.isEmpty() -> ScreenState.Loading
        state.error != null && state.titles.isEmpty() -> ScreenState.Error
        state.titles.isEmpty() -> ScreenState.Empty
        else -> ScreenState.Content
    }

    val updateFilter = { filter: CatalogFilter ->
        viewModel.onIntent(CatalogIntent.UpdateFilter(filter))
    }

    val animatedSearchPadding by animateDpAsState(
        targetValue = if (searchExpanded) 0.dp else Spacing.screenHorizontal,
        animationSpec = MotionTokens.effectsDefault(),
        label = "searchBarPadding",
    )
    val searchHorizontalPadding = animatedSearchPadding.coerceAtLeast(0.dp)
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            SearchBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(1f)
                    .padding(horizontal = searchHorizontalPadding),
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchText,
                        onQueryChange = {
                            searchText = it
                            viewModel.onIntent(CatalogIntent.Search(it))
                        },
                        onSearch = {
                            viewModel.onIntent(CatalogIntent.SubmitSearch(it))
                            searchExpanded = false
                        },
                        expanded = searchExpanded,
                        onExpandedChange = { searchExpanded = it },
                        placeholder = { Text("Поиск аниме…") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            AnimatedVisibility(
                                visible = searchText.isNotEmpty(),
                                enter = scaleIn(MotionTokens.spatialDefault(), initialScale = 0.6f) +
                                    fadeIn(MotionTokens.effectsDefault()),
                                exit = scaleOut(MotionTokens.spatialFast(), targetScale = 0.6f) +
                                    fadeOut(MotionTokens.effectsFast()),
                            ) {
                                IconButton(onClick = {
                                    searchText = ""
                                    viewModel.onIntent(CatalogIntent.ClearSearch)
                                }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Очистить")
                                }
                            }
                        },
                    )
                },
                expanded = searchExpanded,
                onExpandedChange = { searchExpanded = it },
            ) {
                SearchOverlay(
                    query = searchText,
                    suggestions = state.suggestions,
                    suggestionsLoading = state.suggestionsLoading,
                    characters = state.characterSuggestions,
                    onPickCharacter = onCharacterClick,
                    history = state.searchHistory,
                    onPick = {
                        searchText = it
                        viewModel.onIntent(CatalogIntent.SubmitSearch(it))
                        searchExpanded = false
                    },
                    onFillField = {
                        searchText = it
                        viewModel.onIntent(CatalogIntent.Search(it))
                    },
                    onPickGenre = { genre ->
                        updateFilter(state.filter.copy(genres = setOf(genre)))
                        searchExpanded = false
                    },
                    onClearHistory = { viewModel.onIntent(CatalogIntent.ClearSearchHistory) },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = statusBarPadding + SEARCH_BAR_SLOT_HEIGHT)
            ) {
                CatalogControls(
                    viewMode = state.filter.viewMode,
                    activeFilterCount = state.filter.activeCount,
                    onViewModeChange = {
                        haptics.tick()
                        viewModel.onIntent(CatalogIntent.ToggleViewMode(it))
                    },
                    onOpenFilters = { showFiltersSheet = true },
                    onOpenStudios = { onNavigateToStudioSearch(searchText) },
                    onRandomPick = {
                        val available = state.titles
                        if (available.isNotEmpty()) {
                            haptics.confirm()
                            val picked = available.random()
                            onTitleClick(picked.id)
                        }
                    },
                )

                ActiveFilterChips(
                    filter = state.filter,
                    onFilterChange = updateFilter,
                )

                ScreenStateHost(
                state = screenState,
                modifier = Modifier.fillMaxSize(),
                onRetry = { viewModel.onIntent(CatalogIntent.Refresh) },
                loading = { CatalogSkeleton(state.filter.viewMode) },
                error = {
                    ErrorView(
                        message = state.error ?: "",
                        onRetry = { viewModel.onIntent(CatalogIntent.Refresh) },
                    )
                },
                empty = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            kind = EmptyKind.SearchResult,
                            action = if (state.filter.hasActiveFilters) {
                                {
                                    TextButton(onClick = { updateFilter(CatalogFilter()) }) {
                                        Text("Сбросить фильтры")
                                    }
                                }
                            } else {
                                null
                            },
                        )
                    }
                },
            ) {
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = { viewModel.onIntent(CatalogIntent.Refresh) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val bottomPadding = LocalBottomBarHeight.current + Spacing.lg
                    if (state.filter.viewMode == ViewMode.GRID) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = Spacing.screenHorizontal,
                                end = Spacing.screenHorizontal,
                                top = Spacing.sm,
                                bottom = bottomPadding,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.gridGutter),
                            verticalArrangement = Arrangement.spacedBy(Spacing.gridGutter),
                        ) {
                            items(items = state.titles, key = { it.id }) { title ->
                                TitleCardGrid(
                                    title = title,
                                    onClick = { onTitleClick(title.id) },
                                    onLongClick = { contextMenuTitle = title },
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = MotionTokens.effectsDefault(),
                                        placementSpec = MotionTokens.spatialDefault(),
                                        fadeOutSpec = MotionTokens.effectsFast(),
                                    ),
                                )
                            }
                            if (state.loadingMore) {
                                items(2) { TitleCardGridSkeleton() }
                            }
                            if (!state.hasMore && !state.loadingMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) { EndOfList() }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = Spacing.screenHorizontal,
                                end = Spacing.screenHorizontal,
                                top = Spacing.sm,
                                bottom = bottomPadding,
                            ),
                            verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap),
                        ) {
                            items(items = state.titles, key = { it.id }) { title ->
                                TitleCardList(
                                    title = title,
                                    onClick = { onTitleClick(title.id) },
                                    onLongClick = { contextMenuTitle = title },
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = MotionTokens.effectsDefault(),
                                        placementSpec = MotionTokens.spatialDefault(),
                                        fadeOutSpec = MotionTokens.effectsFast(),
                                    ),
                                )
                            }
                            if (state.loadingMore) {
                                items(2) { TitleCardListSkeleton() }
                            }
                            if (!state.hasMore && !state.loadingMore) {
                                item { EndOfList() }
                            }
                        }
                    }
                }
                }
            }
        }
    }

    contextMenuTitle?.let { title ->
        com.anilibrix.plus.ui.components.TitleContextMenuSheet(
            title = title,
            onPlay = { onTitleClick(title.id) },
            onOpenDetails = { onTitleClick(title.id) },
            onToggleFavorite = {},
            onSetCollectionStatus = {},
            onClearCollectionStatus = {},
            onDismiss = { contextMenuTitle = null }
        )
    }

    if (showFiltersSheet) {
        val sheetState = rememberModalBottomSheetState()
        // Закрытие ждёт анимацию: раньше sheetState вообще не передавался,
        // и шторка пропадала мгновенно.
        val dismiss = rememberSheetDismiss(sheetState) { showFiltersSheet = false }

        AnilibrixBottomSheet(onDismiss = { showFiltersSheet = false }, sheetState = sheetState) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Фильтры",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = { updateFilter(CatalogFilter()) }) {
                    Text("Сбросить")
                }
            }
            CatalogFiltersPanel(
                filter = state.filter,
                onFilterChange = updateFilter,
                modifier = Modifier
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm)
                    .verticalScroll(rememberScrollState()),
            )
            TextButton(
                onClick = dismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.screenHorizontal),
            ) {
                Text("Показать ${state.titles.size} результатов")
            }
        }
    }
}

/**
 * Контролы каталога, разведённые по визуальному весу.
 *
 * Раньше все четыре были одинаковыми `AssistChip`: переключатель режима,
 * открытие фильтров, переход к студиям и **деструктивный сброс** выглядели
 * неразличимо. Теперь режим — сегментированный переключатель, счётчик активных
 * фильтров — бейдж вместо звёздочки в тексте, а сброс убран отсюда совсем
 * (он есть в шапке шторки и на строке активных фильтров).
 */
@Composable
private fun CatalogControls(
    viewMode: ViewMode,
    activeFilterCount: Int,
    onViewModeChange: (ViewMode) -> Unit,
    onOpenFilters: () -> Unit,
    onOpenStudios: () -> Unit,
    onRandomPick: () -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = viewMode == ViewMode.GRID,
                    onClick = { onViewModeChange(ViewMode.GRID) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = {},
                ) {
                    Icon(
                        Icons.Rounded.GridView,
                        contentDescription = "Сетка",
                        modifier = Modifier.size(Sizing.iconSm),
                    )
                }
                SegmentedButton(
                    selected = viewMode == ViewMode.LIST,
                    onClick = { onViewModeChange(ViewMode.LIST) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = {},
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.List,
                        contentDescription = "Список",
                        modifier = Modifier.size(Sizing.iconSm),
                    )
                }
            }
        }

        item {
            BadgedBox(
                badge = {
                    if (activeFilterCount > 0) {
                        Badge { Text(activeFilterCount.toString()) }
                    }
                }
            ) {
                AssistChip(
                    onClick = onOpenFilters,
                    label = { Text("Фильтры") },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(Sizing.iconSm),
                        )
                    },
                )
            }
        }

        item {
            AssistChip(
                onClick = onOpenStudios,
                label = { Text("Студии") },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(Sizing.iconSm),
                    )
                },
            )
        }

        item {
            AssistChip(
                onClick = onRandomPick,
                label = { Text("Случайно") },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Rounded.CallMade,
                        contentDescription = null,
                        modifier = Modifier.size(Sizing.iconSm),
                    )
                },
            )
        }
    }
}

/**
 * Активные фильтры отдельными съёмными чипами.
 *
 * Раньше это была строка текста «Активно: …», и чтобы снять один фильтр,
 * надо было открыть шторку, найти его и сбросить. Теперь — один тап.
 */
@Composable
private fun ActiveFilterChips(
    filter: CatalogFilter,
    onFilterChange: (CatalogFilter) -> Unit,
) {
    val chips = filter.activeChips(onFilterChange)
    if (chips.isEmpty()) return

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(items = chips, key = { it.label }) { chip ->
            InputChip(
                selected = true,
                onClick = chip.onRemove,
                label = { Text(chip.label, maxLines = 1) },
                trailingIcon = {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Убрать фильтр",
                        modifier = Modifier.size(Sizing.iconSm),
                    )
                },
                modifier = Modifier.animateItem(placementSpec = MotionTokens.spatialDefault()),
            )
        }
    }
}

@Composable
private fun CatalogSkeleton(viewMode: ViewMode) {
    ShimmerHost {
        if (viewMode == ViewMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = Spacing.screenHorizontal,
                    vertical = Spacing.sm,
                ),
                horizontalArrangement = Arrangement.spacedBy(Spacing.gridGutter),
                verticalArrangement = Arrangement.spacedBy(Spacing.gridGutter),
                userScrollEnabled = false,
            ) {
                items(6) { TitleCardGridSkeleton() }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = Spacing.screenHorizontal,
                    vertical = Spacing.sm,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap),
                userScrollEnabled = false,
            ) {
                items(6) { TitleCardListSkeleton() }
            }
        }
    }
}

@Composable
private fun EndOfList() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Больше результатов нет",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Содержимое раскрытого поиска.
 *
 * Раньше здесь был плоский список подсказок: при пяти результатах ниже
 * оставалась большая пустая плашка, а совпадение с запросом никак не
 * выделялось. Теперь — разделы с заголовками, подсветка совпавшей части
 * и быстрые жанры, когда вводить ещё нечего.
 */
@Composable
private fun SearchOverlay(
    query: String,
    suggestions: List<String>,
    suggestionsLoading: Boolean,
    characters: List<ShikimoriCharacterSearchResult>,
    history: List<String>,
    onPickCharacter: (Long) -> Unit,
    onPick: (String) -> Unit,
    onFillField: (String) -> Unit,
    onPickGenre: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.xxl),
    ) {
        if (suggestions.isNotEmpty()) {
            item(key = "h_sug") {
                SearchSectionHeader(title = "Подсказки")
            }
            items(items = suggestions, key = { "s_$it" }) { item ->
                SearchRow(
                    text = item,
                    query = query,
                    icon = Icons.Rounded.Search,
                    onClick = { onPick(item) },
                    onFill = { onFillField(item) },
                    modifier = Modifier.animateItem(
                        fadeInSpec = MotionTokens.effectsDefault(),
                        placementSpec = MotionTokens.spatialDefault(),
                        fadeOutSpec = MotionTokens.effectsFast(),
                    ),
                )
            }
        }

        // Персонажи — отдельным разделом ниже тайтлов: искать по ним люди
        // начинают реже, но когда начинают, ищут именно персонажа.
        if (characters.isNotEmpty()) {
            item(key = "h_chars") {
                SearchSectionHeader(title = "Персонажи")
            }
            items(items = characters, key = { "c_${it.id}" }) { character ->
                ListItem(
                    headlineContent = { Text(character.displayName, maxLines = 1) },
                    supportingContent = character.russian
                        ?.takeIf { it.isNotBlank() && it != character.name }
                        ?.let { { Text(character.name, maxLines = 1) } },
                    leadingContent = {
                        AnilibrixImage(
                            model = character.imageUrl,
                            contentDescription = character.displayName,
                            modifier = Modifier.size(Sizing.avatarSm),
                            shape = CircleShape,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable { onPickCharacter(character.id.toLong()) }
                        .animateItem(
                            fadeInSpec = MotionTokens.effectsDefault(),
                            placementSpec = MotionTokens.spatialDefault(),
                            fadeOutSpec = MotionTokens.effectsFast(),
                        ),
                )
            }
        }

        if (history.isNotEmpty()) {
            item(key = "h_hist") {
                SearchSectionHeader(
                    title = "Недавние запросы",
                    action = {
                        TextButton(onClick = onClearHistory) { Text("Очистить") }
                    },
                )
            }
            items(items = history, key = { "h_$it" }) { item ->
                SearchRow(
                    text = item,
                    query = query,
                    icon = Icons.Rounded.History,
                    onClick = { onPick(item) },
                    onFill = { onFillField(item) },
                    modifier = Modifier.animateItem(
                        fadeInSpec = MotionTokens.effectsDefault(),
                        placementSpec = MotionTokens.spatialDefault(),
                        fadeOutSpec = MotionTokens.effectsFast(),
                    ),
                )
            }
        }

        // Пока запрос набирается, а подсказки ещё не пришли, показываем
        // скелетон строк — иначе панель схлопывается в пустоту и дёргается,
        // когда ответ наконец приходит.
        if (query.isNotBlank() && suggestions.isEmpty() && suggestionsLoading) {
            item(key = "sug_skeleton") {
                ShimmerHost {
                    // fillMaxWidth обязателен: без него Column сжимается по
                    // содержимому и полосы скелетона уезжают к центру.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.sm)
                    ) {
                        repeat(3) {
                            Spacer(
                                Modifier
                                    .padding(
                                        horizontal = Spacing.screenHorizontal,
                                        vertical = Spacing.md,
                                    )
                                    .fillMaxWidth(0.7f)
                                    .height(16.dp)
                                    .shimmer(MaterialTheme.shapes.extraSmall)
                            )
                        }
                    }
                }
            }
        }

        // Запрос есть, загрузка закончилась, совпадений нет — так и говорим,
        // вместо бесконечного скелетона.
        if (query.isNotBlank() && suggestions.isEmpty() && !suggestionsLoading) {
            item(key = "sug_none") {
                Text(
                    text = "Совпадений нет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = Spacing.screenHorizontal,
                        vertical = Spacing.lg,
                    ),
                )
            }
        }

        // Жанры — только когда вводить ещё нечего. Показывать их поверх
        // набранного запроса было бы просто неверно.
        if (query.isBlank() && history.isEmpty()) {
            item(key = "h_genres") {
                SearchSectionHeader(title = "Популярные жанры")
            }
            item(key = "genres") {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    catalogGenreOptions.take(12).forEach { option ->
                        AssistChip(
                            onClick = { onPickGenre(option.id) },
                            label = { Text(option.label) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Spacing.screenHorizontal,
                end = Spacing.sm,
                top = Spacing.md,
                bottom = Spacing.xs,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        action?.invoke()
    }
}

/**
 * Строка подсказки: совпавшая с запросом часть выделяется, а стрелка справа
 * подставляет текст в поле, не запуская поиск, — стандартное поведение,
 * которого раньше не было вовсе.
 */
@Composable
private fun SearchRow(
    text: String,
    query: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    onFill: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        // Прозрачный контейнер: у ListItem фон `surface`, а у раскрытой панели
        // поиска — `surfaceContainerHigh`, из-за чего под заголовком раздела
        // проступала полоса другого оттенка.
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(highlightMatch(text, query)) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Sizing.iconMd),
            )
        },
        trailingContent = {
            IconButton(onClick = onFill) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.CallMade,
                    contentDescription = "Подставить в поле",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(Sizing.iconSm)
                        .rotate(-90f),
                )
            }
        },
    )
}

/** Выделяет совпавшую с запросом часть подсказки. */
@Composable
private fun highlightMatch(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val index = text.indexOf(query, ignoreCase = true)
    if (index < 0) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, index))
        withStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        ) {
            append(text.substring(index, index + query.length))
        }
        append(text.substring(index + query.length))
    }
}

/** Общая логика догрузки для сетки и списка — раньше была продублирована. */
@Composable
private fun PaginationEffect(
    active: Boolean,
    totalItems: () -> Int,
    lastVisibleIndex: () -> Int?,
    canLoadMore: () -> Boolean,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(active, canLoadMore()) {
        if (!active) return@LaunchedEffect
        snapshotFlow {
            val total = totalItems()
            val last = lastVisibleIndex()
            total > 0 && last != null && last >= total - 3
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { if (canLoadMore()) onLoadMore() }
    }
}

@Composable
private fun CatalogFiltersPanel(
    filter: CatalogFilter,
    onFilterChange: (CatalogFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        FilterChipsRow(
            options = catalogGenreOptions,
            selectedIds = filter.genres,
            onSelectionChanged = { ids -> onFilterChange(filter.copy(genres = ids)) },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            FilterDropdownChip(
                label = "Год",
                selectedLabel = filter.year?.toString() ?: ANY_LABEL,
                selected = filter.year != null,
                options = yearOptions,
                onSelect = { year -> onFilterChange(filter.copy(year = year)) },
                modifier = Modifier.weight(1f),
            )
            FilterDropdownChip(
                label = "Тип",
                selectedLabel = filter.type?.displayName ?: ANY_LABEL,
                selected = filter.type != null,
                options = typeOptions,
                onSelect = { type -> onFilterChange(filter.copy(type = type)) },
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            FilterDropdownChip(
                label = "Сезон",
                selectedLabel = filter.season?.displayNameRu() ?: ANY_LABEL,
                selected = filter.season != null,
                options = seasonOptions,
                onSelect = { season -> onFilterChange(filter.copy(season = season)) },
                modifier = Modifier.weight(1f),
            )
            FilterDropdownChip(
                label = "Статус",
                selectedLabel = filter.status?.displayName ?: ANY_LABEL,
                selected = filter.status != null,
                options = statusOptions,
                onSelect = { status -> onFilterChange(filter.copy(status = status)) },
                modifier = Modifier.weight(1f),
            )
        }

        FilterDropdownChip(
            label = "Сортировка",
            selectedLabel = filter.sort.displayName,
            selected = filter.sort != CatalogSort.UPDATED,
            options = sortOptions,
            onSelect = { sort -> onFilterChange(filter.copy(sort = sort ?: CatalogSort.UPDATED)) },
        )
    }
}

@Composable
private fun <T> FilterDropdownChip(
    label: String,
    selectedLabel: String,
    selected: Boolean,
    options: List<Pair<T?, String>>,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FilterChip(
            // Раньше выбранность выводилась сравнением подписи с магической
            // строкой «Все» — ломалось от любой правки текста.
            selected = selected,
            onClick = { expanded = true },
            label = { Text("$label: $selectedLabel", maxLines = 1) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

private const val ANY_LABEL = "Все"

private data class ActiveFilterChipData(val label: String, val onRemove: () -> Unit)

private val CatalogFilter.activeCount: Int
    get() = genres.size +
        listOfNotNull(year, type, season, status).size +
        (if (sort != CatalogSort.UPDATED) 1 else 0)

private fun CatalogFilter.activeChips(
    onChange: (CatalogFilter) -> Unit,
): List<ActiveFilterChipData> = buildList {
    genres.forEach { genre ->
        add(ActiveFilterChipData(genre) { onChange(copy(genres = genres - genre)) })
    }
    year?.let { add(ActiveFilterChipData(it.toString()) { onChange(copy(year = null)) }) }
    type?.let { add(ActiveFilterChipData(it.displayName) { onChange(copy(type = null)) }) }
    season?.let { add(ActiveFilterChipData(it.displayNameRu()) { onChange(copy(season = null)) }) }
    status?.let { add(ActiveFilterChipData(it.displayName) { onChange(copy(status = null)) }) }
    if (sort != CatalogSort.UPDATED) {
        add(ActiveFilterChipData(sort.displayName) { onChange(copy(sort = CatalogSort.UPDATED)) })
    }
}

private fun SeasonName.displayNameRu(): String = when (this) {
    SeasonName.WINTER -> "Зима"
    SeasonName.SPRING -> "Весна"
    SeasonName.SUMMER -> "Лето"
    SeasonName.FALL -> "Осень"
    SeasonName.UNKNOWN -> "Неизвестно"
}

private val catalogGenreOptions = listOf(
    "Экшен", "Приключения", "Комедия", "Драма", "Фэнтези", "Романтика",
    "Фантастика", "Повседневность", "Спорт", "Мистика", "Ужасы", "Триллер",
    "Детектив", "Психология", "Музыка", "Исторический", "Меха",
    "Сверхъестественное", "Сёнен", "Сэйнэн",
).map { FilterOption(it, it) }

/** Верхняя граница вычисляется, а не зашита числом 2026, которое устареет. */
private val yearOptions: List<Pair<Int?, String>> =
    listOf(null to ANY_LABEL) + (Year.now().value downTo 1990).map { it to it.toString() }

private val typeOptions: List<Pair<ReleaseType?, String>> =
    listOf(null to ANY_LABEL) + ReleaseType.entries
        .filter { it != ReleaseType.UNKNOWN }
        .map { it to it.displayName }

private val seasonOptions: List<Pair<SeasonName?, String>> =
    listOf(null to ANY_LABEL) + SeasonName.entries
        .filter { it != SeasonName.UNKNOWN }
        .map { it to it.displayNameRu() }

private val statusOptions: List<Pair<CatalogStatus?, String>> =
    listOf(null to ANY_LABEL) + CatalogStatus.entries.map { it to it.displayName }

private val sortOptions: List<Pair<CatalogSort?, String>> =
    CatalogSort.entries.map { it to it.displayName }

/**
 * Место, которое свёрнутый поиск занимает сверху: он лежит слоем НАД
 * контентом, поэтому высоту приходится резервировать вручную.
 */
private val SEARCH_BAR_SLOT_HEIGHT = 72.dp
