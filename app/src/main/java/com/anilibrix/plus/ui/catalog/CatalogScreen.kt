package com.anilibrix.plus.ui.catalog

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.verticalScroll

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anilibrix.plus.ui.components.AnilibrixSearchBar
import com.anilibrix.plus.ui.components.ErrorView
import com.anilibrix.plus.ui.components.FilterOption
import com.anilibrix.plus.ui.components.FilterChipsRow
import com.anilibrix.plus.ui.components.LoadingIndicator
import com.anilibrix.plus.ui.components.TitleCardGrid
import com.anilibrix.plus.ui.components.TitleCardList
import com.anilibrix.plus.domain.model.CatalogSort
import com.anilibrix.plus.domain.model.CatalogStatus
import com.anilibrix.plus.domain.model.ReleaseType
import com.anilibrix.plus.domain.model.SeasonName
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel = hiltViewModel(),
    onTitleClick: (Long) -> Unit = {},
    onNavigateToStudioSearch: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    var searchText by remember { mutableStateOf(state.filter.search) }
    var showFiltersSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.filter.search) {
        if (searchText != state.filter.search) {
            searchText = state.filter.search
        }
    }

    LaunchedEffect(gridState, state.filter.viewMode, state.hasMore, state.loadingMore) {
        if (state.filter.viewMode == ViewMode.GRID) {
            snapshotFlow {
                val layoutInfo = gridState.layoutInfo
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                layoutInfo.totalItemsCount > 0 &&
                    lastVisibleItem != null &&
                    lastVisibleItem.index >= layoutInfo.totalItemsCount - 3
            }
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    if (state.hasMore && !state.loadingMore) {
                        viewModel.onIntent(CatalogIntent.LoadMore)
                    }
                }
        }
    }

    LaunchedEffect(listState, state.filter.viewMode, state.hasMore, state.loadingMore) {
        if (state.filter.viewMode == ViewMode.LIST) {
            snapshotFlow {
                val layoutInfo = listState.layoutInfo
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                layoutInfo.totalItemsCount > 0 &&
                    lastVisibleItem != null &&
                    lastVisibleItem.index >= layoutInfo.totalItemsCount - 3
            }
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    if (state.hasMore && !state.loadingMore) {
                        viewModel.onIntent(CatalogIntent.LoadMore)
                    }
                }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AnilibrixSearchBar(
            query = searchText,
            onQueryChange = {
                searchText = it
                viewModel.onIntent(CatalogIntent.Search(it))
            },
            onSearch = {
                searchText = it
                viewModel.onIntent(CatalogIntent.SubmitSearch(it))
            },
            suggestions = state.suggestions,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            searchHistory = state.searchHistory,
            onClearHistory = { viewModel.onIntent(CatalogIntent.ClearSearchHistory) }
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = {
                    viewModel.onIntent(
                        CatalogIntent.ToggleViewMode(
                            if (state.filter.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
                        )
                    )
                },
                label = {
                    Text(if (state.filter.viewMode == ViewMode.GRID) "Сетка" else "Список")
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (state.filter.viewMode == ViewMode.GRID) Icons.Default.GridView
                        else Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            )
            AssistChip(
                onClick = { showFiltersSheet = true },
                label = { Text(if (state.filter.hasActiveFilters) "Фильтры*" else "Фильтры") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            )
            AssistChip(
                onClick = { onNavigateToStudioSearch(searchText) },
                label = { Text("Студии") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            )
            if (state.filter.hasActiveFilters) {
                AssistChip(
                    onClick = { viewModel.onIntent(CatalogIntent.UpdateFilter(CatalogFilter())) },
                    label = { Text("Сбросить") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                )
            }
        }

        if (state.filter.hasActiveFilters) {
            Text(
                text = "Активно: ${state.filter.summary()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        when {
            state.loading && state.titles.isEmpty() -> {
                LoadingIndicator(message = "Загружаем каталог…")
            }
            state.error != null && state.titles.isEmpty() -> {
                val errorMsg = state.error
                ErrorView(
                    message = errorMsg ?: "",
                    onRetry = { viewModel.onIntent(CatalogIntent.Refresh) }
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = { viewModel.onIntent(CatalogIntent.Refresh) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (state.filter.viewMode == ViewMode.GRID) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = state.titles,
                                key = { it.id }
                            ) { title ->
                                TitleCardGrid(
                                    title = title,
                                    onClick = { onTitleClick(title.id) }
                                )
                            }
                            if (state.loadingMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            if (!state.hasMore && !state.loadingMore && state.titles.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Больше результатов нет",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = state.titles,
                                key = { it.id }
                            ) { title ->
                                TitleCardList(
                                    title = title,
                                    onClick = { onTitleClick(title.id) }
                                )
                            }
                            if (state.loadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            if (!state.hasMore && !state.loadingMore && state.titles.isNotEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Больше результатов нет",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

    if (showFiltersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFiltersSheet = false }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Фильтры",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = {
                        viewModel.onIntent(CatalogIntent.UpdateFilter(CatalogFilter()))
                    }
                ) {
                    Text("Сбросить")
                }
            }
            CatalogFiltersPanel(
                filter = state.filter,
                onFilterChange = { filter ->
                    viewModel.onIntent(CatalogIntent.UpdateFilter(filter))
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun CatalogFiltersPanel(
    filter: CatalogFilter,
    onFilterChange: (CatalogFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChipsRow(
            options = catalogGenreOptions,
            selectedIds = filter.genres,
            onSelectionChanged = { ids ->
                onFilterChange(filter.copy(genres = ids))
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterDropdownChip(
                label = "Год",
                selectedLabel = filter.year?.toString() ?: "Все",
                options = yearOptions,
                onSelect = { year -> onFilterChange(filter.copy(year = year)) },
                modifier = Modifier.weight(1f)
            )
            FilterDropdownChip(
                label = "Тип",
                selectedLabel = filter.type?.displayName ?: "Все",
                options = typeOptions,
                onSelect = { type -> onFilterChange(filter.copy(type = type)) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterDropdownChip(
                label = "Сезон",
                selectedLabel = filter.season?.displayNameRu() ?: "Все",
                options = seasonOptions,
                onSelect = { season -> onFilterChange(filter.copy(season = season)) },
                modifier = Modifier.weight(1f)
            )
            FilterDropdownChip(
                label = "Статус",
                selectedLabel = filter.status?.displayName ?: "Все",
                options = statusOptions,
                onSelect = { status -> onFilterChange(filter.copy(status = status)) },
                modifier = Modifier.weight(1f)
            )
        }

        FilterDropdownChip(
            label = "Сортировка",
            selectedLabel = filter.sort.displayName,
            options = sortOptions,
            onSelect = { sort -> onFilterChange(filter.copy(sort = sort ?: CatalogSort.UPDATED)) }
        )
    }
}

@Composable
private fun <T> FilterDropdownChip(
    label: String,
    selectedLabel: String,
    options: List<Pair<T?, String>>,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FilterChip(
            selected = selectedLabel != "Все" && selectedLabel != CatalogSort.UPDATED.displayName,
            onClick = { expanded = true },
            label = { Text("$label: $selectedLabel", maxLines = 1) }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun CatalogFilter.summary(): String {
    val parts = buildList {
        if (search.isNotBlank()) add("поиск «$search»")
        if (genres.isNotEmpty()) add(genres.joinToString(", "))
        year?.let { add(it.toString()) }
        type?.let { add(it.displayName) }
        season?.let { add(it.displayNameRu()) }
        status?.let { add(it.displayName) }
        if (sort != CatalogSort.UPDATED) add(sort.displayName)
    }
    return parts.joinToString(" · ")
}

private fun SeasonName.displayNameRu(): String {
    return when (this) {
        SeasonName.WINTER -> "Зима"
        SeasonName.SPRING -> "Весна"
        SeasonName.SUMMER -> "Лето"
        SeasonName.FALL -> "Осень"
        SeasonName.UNKNOWN -> "Неизвестно"
    }
}

private val catalogGenreOptions = listOf(
    "Экшен",
    "Приключения",
    "Комедия",
    "Драма",
    "Фэнтези",
    "Романтика",
    "Фантастика",
    "Повседневность",
    "Спорт",
    "Мистика",
    "Ужасы",
    "Триллер",
    "Детектив",
    "Психология",
    "Музыка",
    "Исторический",
    "Меха",
    "Сверхъестественное",
    "Сёнен",
    "Сэйнэн"
).map { FilterOption(it, it) }

private val yearOptions: List<Pair<Int?, String>> =
    listOf(null to "Все") + (2026 downTo 1990).map { it to it.toString() }

private val typeOptions: List<Pair<ReleaseType?, String>> =
    listOf(null to "Все") + ReleaseType.entries
        .filter { it != ReleaseType.UNKNOWN }
        .map { it to it.displayName }

private val seasonOptions: List<Pair<SeasonName?, String>> =
    listOf(null to "Все") + SeasonName.entries
        .filter { it != SeasonName.UNKNOWN }
        .map { it to it.displayNameRu() }

private val statusOptions: List<Pair<CatalogStatus?, String>> =
    listOf(null to "Все") + CatalogStatus.entries.map { it to it.displayName }

private val sortOptions: List<Pair<CatalogSort?, String>> =
    CatalogSort.entries.map { it to it.displayName }
