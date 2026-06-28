package com.anilibrix.plus.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel = hiltViewModel(),
    onTitleClick: (Long) -> Unit = {},
    onNavigateToStudioSearch: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    var searchText by remember { mutableStateOf(state.filter.search) }

    LaunchedEffect(state.filter.search) {
        if (searchText != state.filter.search) {
            searchText = state.filter.search
        }
    }

    LaunchedEffect(listState) {
        val layoutInfo = listState.layoutInfo
        if (layoutInfo.totalItemsCount > 0) {
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastVisibleItem != null && lastVisibleItem.index >= layoutInfo.totalItemsCount - 3) {
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
            searchHistory = emptyList()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }

        FilterChipsRow(
            options = listOf(
                FilterOption("Экшен", "Экшен"),
                FilterOption("Комедия", "Комедия"),
                FilterOption("Драма", "Драма"),
                FilterOption("Фэнтези", "Фэнтези"),
                FilterOption("Романтика", "Романтика"),
                FilterOption("Фантастика", "Фантастика")
            ),
            selectedIds = state.filter.genres,
            onSelectionChanged = { ids ->
                viewModel.onIntent(
                    CatalogIntent.UpdateFilter(
                        state.filter.copy(genres = ids)
                    )
                )
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        when {
            state.loading && state.titles.isEmpty() -> {
                LoadingIndicator()
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
                        }
                    }
                }
            }
        }
    }
}
