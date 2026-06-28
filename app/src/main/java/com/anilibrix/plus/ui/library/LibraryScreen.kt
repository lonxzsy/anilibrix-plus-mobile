package com.anilibrix.plus.ui.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.Playlist
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onPlayEpisode: (Long, Long) -> Unit = { _, _ -> },
    onTitleClick: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val tabs = listOf("Избранное", "Буду смотреть", "История", "Плейлисты")

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.continueWatching.isNotEmpty()) {
            ContinueWatchingSection(
                items = state.continueWatching,
                onPlayEpisode = onPlayEpisode,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        ScrollableTabRow(
            selectedTabIndex = state.selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 8.dp
        ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = state.selectedTab == index,
                        onClick = { viewModel.handleIntent(LibraryIntent.SelectTab(index)) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Default.Star
                                        1 -> Icons.Default.WatchLater
                                        2 -> Icons.Default.History
                                        3 -> Icons.AutoMirrored.Filled.PlaylistPlay
                                        else -> Icons.Default.BookmarkBorder
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(title)
                            }
                        }
                    )
                }
            }

            when (state.selectedTab) {
                0 -> FavoritesTab(
                    items = state.favorites,
                    onRemove = { viewModel.handleIntent(LibraryIntent.RemoveFavorite(it)) },
                    onTitleClick = onTitleClick
                )
                1 -> WatchLaterTab(
                    items = state.watchLater,
                    onRemove = { viewModel.handleIntent(LibraryIntent.RemoveWatchLater(it)) },
                    onTitleClick = onTitleClick
                )
                2 -> HistoryTab(
                    items = state.history,
                    onRemove = { titleId, episodeId ->
                        viewModel.handleIntent(LibraryIntent.RemoveHistory(titleId, episodeId))
                    },
                    onPlayEpisode = onPlayEpisode
                )
                3 -> PlaylistsTab(
                    playlists = state.playlists,
                    expandedId = state.expandedPlaylistId,
                    onToggle = { viewModel.handleIntent(LibraryIntent.TogglePlaylist(it)) },
                    onCreate = { viewModel.handleIntent(LibraryIntent.CreatePlaylist(it)) },
                    onDelete = { viewModel.handleIntent(LibraryIntent.DeletePlaylist(it)) }
                )
            }
        }
    }

@Composable
private fun ContinueWatchingSection(
    items: List<HistoryEntry>,
    onPlayEpisode: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Продолжить просмотр",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { entry ->
                Card(
                    modifier = Modifier
                        .width(160.dp)
                        .clickable { onPlayEpisode(entry.titleId, entry.episodeId) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            if (entry.posterUrl != null) {
                                GlideImage(
                                    imageModel = { entry.posterUrl },
                                    modifier = Modifier.fillMaxSize(),
                                    imageOptions = ImageOptions(
                                        contentScale = ContentScale.Crop,
                                        contentDescription = entry.titleName
                                    )
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${entry.episodeNumber}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = entry.titleName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val progress = if (entry.duration > 0) {
                            entry.timestamp.toFloat() / entry.duration.toFloat()
                        } else 0f
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritesTab(
    items: List<FavoriteItem>,
    onRemove: (Long) -> Unit,
    onTitleClick: (Long) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState("Нет избранного", "Добавляйте аниме в избранное из карточки тайтла")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.titleId }) { item ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onRemove(item.titleId)
                            true
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.error)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTitleClick(item.titleId) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.posterUrl != null) {
                                    GlideImage(
                                        imageModel = { item.posterUrl },
                                        modifier = Modifier.fillMaxSize(),
                                        imageOptions = ImageOptions(
                                            contentScale = ContentScale.Crop,
                                            contentDescription = item.titleName
                                        )
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.titleName.ifBlank { "Title #${item.titleId}" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchLaterTab(
    items: List<FavoriteItem>,
    onRemove: (Long) -> Unit,
    onTitleClick: (Long) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState("Нет в списке", "Добавляйте аниме в «Буду смотреть»")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.titleId }) { item ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onRemove(item.titleId)
                            true
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.error)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTitleClick(item.titleId) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.posterUrl != null) {
                                    GlideImage(
                                        imageModel = { item.posterUrl },
                                        modifier = Modifier.fillMaxSize(),
                                        imageOptions = ImageOptions(
                                            contentScale = ContentScale.Crop,
                                            contentDescription = item.titleName
                                        )
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.WatchLater,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.titleName.ifBlank { "Title #${item.titleId}" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTab(
    items: List<HistoryEntry>,
    onRemove: (Long, Long) -> Unit,
    onPlayEpisode: (Long, Long) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState("История пуста", "Начните смотреть аниме, чтобы увидеть историю")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { "${it.titleId}_${it.episodeId}" }) { entry ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onRemove(entry.titleId, entry.episodeId)
                            true
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.error)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayEpisode(entry.titleId, entry.episodeId) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.titleName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Эпизод ${entry.episodeNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val progress = if (entry.duration > 0) {
                                entry.timestamp.toFloat() / entry.duration.toFloat()
                            } else 0f
                            LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<Playlist>,
    expandedId: Long?,
    onToggle: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Плейлисты",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Создать")
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Новый плейлист") },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Название") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                onCreate(newPlaylistName.trim())
                                newPlaylistName = ""
                                showCreateDialog = false
                            }
                        }
                    ) { Text("Создать") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) { Text("Отмена") }
                }
            )
        }

        if (playlists.isEmpty()) {
            EmptyState("Нет плейлистов", "Создайте плейлист, чтобы собирать аниме")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    val isExpanded = expandedId == playlist.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(playlist.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ListAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "${playlist.items.size} элементов",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDelete(playlist.id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            if (isExpanded && playlist.items.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                playlist.items.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.titleName,
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
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
