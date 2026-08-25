package com.anilibrix.plus.ui.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.anilibrix.plus.core.download.DownloadItem
import com.anilibrix.plus.core.download.DownloadState
import com.anilibrix.plus.core.torrent.TorrentDownloadState
import com.anilibrix.plus.core.torrent.TorrentFileItem
import com.anilibrix.plus.core.torrent.TorrentTaskInfo
import com.anilibrix.plus.ui.components.AnilibrixImage
import com.anilibrix.plus.ui.components.EmptyKind
import com.anilibrix.plus.ui.components.EmptyState
import com.anilibrix.plus.ui.components.SectionHeader
import com.anilibrix.plus.ui.components.pressScale
import com.anilibrix.plus.ui.navigation.screenContentPadding
import com.anilibrix.plus.ui.theme.AnilibrixShapeExtras
import com.anilibrix.plus.ui.theme.Elevation
import com.anilibrix.plus.ui.theme.Spacing
import com.anilibrix.plus.ui.theme.extended
import java.util.Locale

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onPlayEpisode: (Long, Long) -> Unit = { _, _ -> },
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val torrentTasks by viewModel.torrentTasks.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val paused by viewModel.paused.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showClearDialog by remember { mutableStateOf(false) }

    val hasAnyItems = summary.items.isNotEmpty() || torrentTasks.isNotEmpty()

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Удалить все загрузки?") },
            text = { Text("Скачанные серии и торренты будут стёрты с устройства.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onIntent(DownloadsIntent.RemoveAll)
                    torrentTasks.forEach {
                        viewModel.onIntent(DownloadsIntent.RemoveTorrent(it.id, deleteFiles = true))
                    }
                    showClearDialog = false
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Отмена") }
            },
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Загрузки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (summary.hasActive) {
                        IconButton(onClick = { viewModel.onIntent(DownloadsIntent.TogglePauseAll) }) {
                            Icon(
                                imageVector = if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = if (paused) "Возобновить все" else "Приостановить все",
                            )
                        }
                    }
                    if (hasAnyItems) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Удалить все")
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { padding ->
        if (!hasAnyItems) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    kind = EmptyKind.History,
                    title = "Ничего не скачано",
                    subtitle = "Серии и торренты появятся здесь после начала загрузки",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = screenContentPadding(
                    scaffoldPadding = padding,
                    extraBottom = Spacing.xxl
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Вкладки переключения категорий загрузок
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        val totalCount = summary.items.size + torrentTasks.size
                        FilterChip(
                            selected = selectedTab == DownloadsTab.ALL,
                            onClick = { viewModel.onIntent(DownloadsIntent.SelectTab(DownloadsTab.ALL)) },
                            label = { Text("Все ($totalCount)") }
                        )
                        FilterChip(
                            selected = selectedTab == DownloadsTab.TORRENTS,
                            onClick = { viewModel.onIntent(DownloadsIntent.SelectTab(DownloadsTab.TORRENTS)) },
                            label = { Text("Торренты (${torrentTasks.size})") }
                        )
                        FilterChip(
                            selected = selectedTab == DownloadsTab.EPISODES,
                            onClick = { viewModel.onIntent(DownloadsIntent.SelectTab(DownloadsTab.EPISODES)) },
                            label = { Text("Серии (${summary.items.size})") }
                        )
                    }
                }

                // Секция 1: Торренты (если выбраны вкладки ALL или TORRENTS)
                if (selectedTab != DownloadsTab.EPISODES && torrentTasks.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Торрент-загрузки (${torrentTasks.size})",
                            modifier = Modifier.padding(horizontal = Spacing.md)
                        )
                    }

                    items(torrentTasks, key = { "torrent_${it.id}" }) { task ->
                        TorrentTaskCard(
                            task = task,
                            onPause = { viewModel.onIntent(DownloadsIntent.PauseTorrent(task.id)) },
                            onResume = { viewModel.onIntent(DownloadsIntent.ResumeTorrent(task.id)) },
                            onDelete = { viewModel.onIntent(DownloadsIntent.RemoveTorrent(task.id, deleteFiles = true)) },
                            onPlay = { epId -> onPlayEpisode(task.titleId, epId) },
                            modifier = Modifier.padding(horizontal = Spacing.md)
                        )
                    }
                }

                // Секция 2: HLS Серии (если выбраны вкладки ALL или EPISODES)
                if (selectedTab != DownloadsTab.TORRENTS && summary.items.isNotEmpty()) {
                    val groups = summary.items.groupByTitle()
                    item {
                        SectionHeader(
                            title = "Серии (${summary.items.size})",
                            modifier = Modifier.padding(horizontal = Spacing.md)
                        )
                    }

                    groups.forEach { group ->
                        item(key = "title_${group.titleId}") {
                            DownloadTitleHeader(group = group, modifier = Modifier.padding(horizontal = Spacing.md))
                        }
                        items(group.items, key = { it.requestId }) { item ->
                            DownloadItemRow(
                                item = item,
                                onPlay = { onPlayEpisode(item.titleId, item.episodeId) },
                                onResume = { viewModel.onIntent(DownloadsIntent.Resume(item.requestId)) },
                                onRemove = { viewModel.onIntent(DownloadsIntent.Remove(item.requestId)) },
                                modifier = Modifier.padding(horizontal = Spacing.md)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TorrentTaskCard(
    task: TorrentTaskInfo,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    onPlay: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            // Верхняя часть: Постер, Название, Статус и Кнопки управления
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                if (task.posterUrl != null) {
                    AnilibrixImage(
                        model = task.posterUrl,
                        contentDescription = task.titleName,
                        shape = AnilibrixShapeExtras.poster,
                        modifier = Modifier.size(width = 56.dp, height = 80.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.titleName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = task.torrentName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Теги: Группа, Качество, Статус
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!task.releaseGroup.isNullOrBlank()) {
                            StatusTag(text = task.releaseGroup, color = MaterialTheme.colorScheme.primaryContainer)
                        }
                        if (!task.quality.isNullOrBlank()) {
                            StatusTag(text = task.quality, color = MaterialTheme.colorScheme.tertiaryContainer)
                        }
                        TorrentStateBadge(state = task.state)
                    }
                }

                // Кнопки управления (Пауза/Возобновить, Удалить)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.state == TorrentDownloadState.DOWNLOADING) {
                        IconButton(onClick = onPause) {
                            Icon(Icons.Rounded.Pause, contentDescription = "Пауза", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else if (task.state == TorrentDownloadState.PAUSED) {
                        IconButton(onClick = onResume) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Возобновить", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Прогресс-бар
            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Информация о скорости, пирах и объёме
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val downloadedStr = formatBytes(task.downloadedBytes)
                val totalStr = formatBytes(task.totalBytes)
                val percent = (task.progress * 100).toInt()

                Text(
                    text = "$percent% ($downloadedStr из $totalStr)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (task.state == TorrentDownloadState.DOWNLOADING) {
                    val speedStr = formatSpeed(task.downloadSpeedBytesPerSec)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ArrowDownward, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(text = speedStr, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.extended.seeders)
                        Text(text = "${task.seeds}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.extended.seeders)
                    }
                }
            }

            // Раскрывающийся список файлов
            if (task.files.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { expanded = !expanded }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Файлы серий (${task.files.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        task.files.forEach { file ->
                            TorrentFileRow(file = file, onPlay = { onPlay(file.index.toLong()) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TorrentFileRow(
    file: TorrentFileItem,
    onPlay: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val name = if (file.episodeNumber != null) "Серия ${file.episodeNumber}" else file.name
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (file.sizeBytes > 0) {
                    Text(
                        text = formatBytes(file.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (file.isCompleted) {
                IconButton(onClick = onPlay, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Смотреть", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun TorrentStateBadge(state: TorrentDownloadState) {
    val (label, bg, fg) = when (state) {
        TorrentDownloadState.DOWNLOADING -> Triple("Скачивается", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        TorrentDownloadState.FETCHING_METADATA -> Triple("Метаданные…", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        TorrentDownloadState.PAUSED -> Triple("Пауза", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        TorrentDownloadState.COMPLETED -> Triple("Завершено", MaterialTheme.extended.seeders.copy(alpha = 0.2f), MaterialTheme.extended.seeders)
        TorrentDownloadState.ERROR -> Triple("Ошибка", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        TorrentDownloadState.QUEUED -> Triple("В очереди", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Box(
        modifier = Modifier
            .background(color = bg, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun StatusTag(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color = color, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DownloadTitleHeader(group: DownloadGroup, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (group.posterUrl != null) {
            AnilibrixImage(
                model = group.posterUrl,
                contentDescription = group.titleName,
                shape = AnilibrixShapeExtras.poster,
                modifier = Modifier.size(width = 24.dp, height = 34.dp),
            )
        }
        Text(
            text = group.titleName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DownloadItemRow(
    item: DownloadItem,
    onPlay: () -> Unit,
    onResume: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canPlay = item.state == DownloadState.COMPLETED
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (canPlay) Modifier.clickable(onClick = onPlay) else Modifier),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Серия ${item.episodeNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (item.episodeName.isNotBlank()) {
                    Text(
                        text = item.episodeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item.state == DownloadState.DOWNLOADING && item.progress >= 0f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                    )
                }
            }
            if (canPlay) {
                IconButton(onClick = onPlay) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Смотреть", tint = MaterialTheme.colorScheme.primary)
                }
            } else if (item.state == DownloadState.PAUSED || item.state == DownloadState.FAILED) {
                IconButton(onClick = onResume) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Возобновить")
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 1.0) {
        String.format(Locale.getDefault(), "%.1f ГБ", gb)
    } else {
        val mb = bytes / (1024.0 * 1024.0)
        String.format(Locale.getDefault(), "%.0f МБ", mb)
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    val mb = bytesPerSec / (1024.0 * 1024.0)
    return if (mb >= 1.0) {
        String.format(Locale.getDefault(), "%.1f МБ/с", mb)
    } else {
        val kb = bytesPerSec / 1024.0
        String.format(Locale.getDefault(), "%.0f КБ/с", kb)
    }
}
