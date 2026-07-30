package com.anilibrix.plus.ui.trending

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.clickable
import com.anilibrix.plus.ui.components.LocalToastHostState
import com.anilibrix.plus.ui.components.pressScale
import com.anilibrix.plus.domain.model.MalAnime
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import com.anilibrix.plus.ui.components.AnilibrixLoadingIndicator
import com.anilibrix.plus.ui.components.AnilibrixLoadingIndicatorSmall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendingScreen(
    viewModel: TrendingViewModel = hiltViewModel(),
    onTitleClick: (Long) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val toastHost = LocalToastHostState.current

    // Переход делаем здесь, а не внутри ViewModel: навигация — дело экрана,
    // а ViewModel лишь сообщает, что соответствие найдено (или что его нет).
    LaunchedEffect(state.navigation) {
        when (val nav = state.navigation) {
            is TrendingNavigation.ToTitle -> {
                onTitleClick(nav.titleId)
                viewModel.handleIntent(TrendingIntent.ClearNavigation)
            }
            is TrendingNavigation.NotFound -> {
                toastHost?.showInfo("«${nav.anime.title}» пока нет в каталоге Anilibria")
                viewModel.handleIntent(TrendingIntent.ClearNavigation)
            }
            null -> Unit
        }
    }
    var showSortMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    LaunchedEffect(
        listState,
        state.viewMode,
        state.currentPage,
        state.totalPages,
        state.isLoadingMore
    ) {
        if (state.viewMode == ViewMode.LIST) {
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
                    if (!state.isLoadingMore && state.currentPage < state.totalPages) {
                        viewModel.handleIntent(TrendingIntent.LoadNextPage)
                    }
                }
        }
    }

    LaunchedEffect(
        gridState,
        state.viewMode,
        state.currentPage,
        state.totalPages,
        state.isLoadingMore
    ) {
        if (state.viewMode == ViewMode.GRID) {
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
                    if (!state.isLoadingMore && state.currentPage < state.totalPages) {
                        viewModel.handleIntent(TrendingIntent.LoadNextPage)
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Тренды") },
                actions = {
                    Box {
                        TextButton(onClick = { showSortMenu = true }) {
                            Text(state.sortBy.displayName)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortBy.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.displayName) },
                                    onClick = {
                                        viewModel.handleIntent(TrendingIntent.SetSortBy(sort))
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.handleIntent(TrendingIntent.ToggleViewMode) }) {
                        Icon(
                            imageVector = if (state.viewMode == ViewMode.GRID) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = "Сменить вид"
                        )
                    }
                    IconButton(onClick = { viewModel.handleIntent(TrendingIntent.Refresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                AnilibrixLoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.error ?: "Ошибка",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.handleIntent(TrendingIntent.Refresh) }) {
                        Text("Повторить")
                    }
                }
            } else if (state.viewMode == ViewMode.GRID) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.items, key = { it.malId }) { anime ->
                        TrendingGridCard(
                            anime = anime,
                            rank = state.items.indexOf(anime) + 1,
                            resolving = anime.malId in state.resolving,
                            onClick = { viewModel.handleIntent(TrendingIntent.OpenAnime(anime)) },
                        )
                    }
                    if (state.isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AnilibrixLoadingIndicatorSmall()
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.items, key = { it.malId }) { anime ->
                        TrendingListCard(
                            anime = anime,
                            rank = state.items.indexOf(anime) + 1,
                            resolving = anime.malId in state.resolving,
                            onClick = { viewModel.handleIntent(TrendingIntent.OpenAnime(anime)) },
                        )
                    }
                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AnilibrixLoadingIndicatorSmall()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingGridCard(
    anime: MalAnime,
    rank: Int,
    resolving: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale()
            .clickable(enabled = !resolving, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Box {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (anime.imageUrl != null) {
                        GlideImage(
                            imageModel = { anime.imageUrl },
                            modifier = Modifier.fillMaxSize(),
                            imageOptions = ImageOptions(
                                contentScale = ContentScale.Crop,
                                contentDescription = anime.title
                            )
                        )
                    } else {
                        Text(
                            text = anime.title,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = anime.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (anime.score != null) {
                        Text(
                            text = "Оценка: ${anime.score}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TrendingListCard(
    anime: MalAnime,
    rank: Int,
    resolving: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale()
            .clickable(enabled = !resolving, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (anime.imageUrl != null) {
                GlideImage(
                    imageModel = { anime.imageUrl },
                    modifier = Modifier
                        .size(width = 52.dp, height = 72.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    imageOptions = ImageOptions(
                        contentScale = ContentScale.Crop,
                        contentDescription = anime.title
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = anime.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    if (anime.score != null) {
                        Text(
                            text = "Оценка: ${anime.score}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (anime.popularity != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Поп: #${anime.popularity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (anime.rank != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Ранг: ${anime.rank}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
