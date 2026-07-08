package com.anilibrix.plus.ui.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.ui.components.ErrorView
import com.anilibrix.plus.ui.components.HeroCarousel
import com.anilibrix.plus.ui.components.HomeShimmer
import com.anilibrix.plus.ui.components.TitleCardGrid
import com.anilibrix.plus.ui.components.TitleCardShimmer
import com.anilibrix.plus.ui.theme.shimmerBase
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onTitleClick: (Long) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    when {
        state.loading -> {
            HomeShimmer(modifier = Modifier.fillMaxSize())
        }
        state.error != null && state.heroItems.isEmpty() -> {
            ErrorView(
                message = state.error ?: "Ошибка загрузки",
                onRetry = { viewModel.onIntent(HomeIntent.Load) }
            )
        }
        else -> {
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.onIntent(HomeIntent.Refresh) },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        HeroCarousel(
                            items = state.heroItems,
                            onItemClick = { onTitleClick(it.id) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    if (state.continueWatching.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Продолжить просмотр")
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = state.continueWatching,
                                    key = { it.titleId }
                                ) { entry ->
                                    ContinueWatchingCard(
                                        entry = entry,
                                        onClick = { onTitleClick(entry.titleId) },
                                        modifier = Modifier.width(160.dp)
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }

                    item {
                        SectionHeader(title = "Рекомендуем")
                        Spacer(modifier = Modifier.height(8.dp))
                        RecommendedRow(
                            items = state.recommended,
                            onItemClick = onTitleClick
                        )
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    item {
                        SectionHeader(title = "Недавние обновления")
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(
                        items = state.recentUpdates.take(6).chunked(2),
                        key = { chunk -> chunk.map { it.id }.joinToString() }
                    ) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { title ->
                                TitleCardGrid(
                                    title = title,
                                    onClick = { onTitleClick(title.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun RecommendedRow(
    items: List<Title>,
    onItemClick: (Long) -> Unit
) {
    if (items.isEmpty()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(4) {
                TitleCardShimmer(modifier = Modifier.width(140.dp))
            }
        }
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = items, key = { it.id }) { title ->
                TitleCardGrid(
                    title = title,
                    onClick = { onItemClick(title.id) },
                    modifier = Modifier.width(140.dp)
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    entry: HistoryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (entry.duration > 0) entry.timestamp.toFloat() / entry.duration else 0f

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            Box {
                GlideImage(
                    imageModel = { entry.posterUrl },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    imageOptions = ImageOptions(
                        contentScale = ContentScale.Crop,
                        contentDescription = entry.titleName
                    ),
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(shimmerBase)
                        )
                    }
                )
                LinearProgressIndicator(
                    progress = { if (progress.isFinite() && progress > 0f) progress else 0f },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = entry.titleName,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Серия ${entry.episodeNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
