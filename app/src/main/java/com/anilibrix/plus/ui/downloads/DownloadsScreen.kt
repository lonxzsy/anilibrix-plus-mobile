package com.anilibrix.plus.ui.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.anilibrix.plus.core.download.DownloadItem
import com.anilibrix.plus.core.download.DownloadState
import com.anilibrix.plus.ui.components.AnilibrixImage
import com.anilibrix.plus.ui.components.EmptyKind
import com.anilibrix.plus.ui.components.EmptyState
import com.anilibrix.plus.ui.components.SectionHeader
import com.anilibrix.plus.ui.components.pressScale
import com.anilibrix.plus.ui.navigation.screenContentPadding
import com.anilibrix.plus.ui.theme.AnilibrixShapeExtras
import com.anilibrix.plus.ui.theme.Elevation
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing
import java.util.Locale

/**
 * Скачанные и качающиеся серии.
 *
 * Отдельный экран, а не вкладка библиотеки: у загрузок своя логика управления
 * (пауза, удаление, занятое место), и мешать её со списками просмотра значило
 * бы перегрузить и то, и другое.
 */
@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onPlayEpisode: (Long, Long) -> Unit = { _, _ -> },
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val paused by viewModel.paused.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Удалить все загрузки?") },
            text = { Text("Скачанные серии будут стёрты с устройства. Прогресс просмотра сохранится.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onIntent(DownloadsIntent.RemoveAll)
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
                    if (summary.items.isNotEmpty()) {
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
        if (summary.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    kind = EmptyKind.History,
                    title = "Ничего не скачано",
                    subtitle = "Нажмите на стрелку загрузки рядом с серией, чтобы посмотреть её без сети",
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = screenContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap),
        ) {
            item(key = "storage") {
                Text(
                    text = "Занято на устройстве: ${formatBytes(summary.usedBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
            }

            val active = summary.active
            if (active.isNotEmpty()) {
                item(key = "active-header") {
                    SectionHeader(title = "Скачиваются", horizontalPadding = Spacing.none)
                }
                items(active, key = { it.requestId }) { item ->
                    DownloadRow(
                        item = item,
                        onRemove = { viewModel.onIntent(DownloadsIntent.Remove(item.requestId)) },
                        onResume = { viewModel.onIntent(DownloadsIntent.Resume(item.requestId)) },
                        onPlay = null,
                        modifier = Modifier.animateItem(
                            fadeInSpec = MotionTokens.effectsDefault(),
                            placementSpec = MotionTokens.spatialDefault(),
                            fadeOutSpec = MotionTokens.effectsFast(),
                        ),
                    )
                }
            }

            val groups = summary.completed.groupByTitle()
            groups.forEach { group ->
                item(key = "group-${group.titleId}") {
                    SectionHeader(title = group.titleName, horizontalPadding = Spacing.none)
                }
                items(group.items, key = { it.requestId }) { item ->
                    DownloadRow(
                        item = item,
                        onRemove = { viewModel.onIntent(DownloadsIntent.Remove(item.requestId)) },
                        onResume = null,
                        onPlay = { onPlayEpisode(item.titleId, item.episodeId) },
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

@Composable
private fun DownloadRow(
    item: DownloadItem,
    onRemove: () -> Unit,
    onResume: (() -> Unit)?,
    onPlay: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onPlay != null) Modifier.pressScale().clickable(onClick = onPlay) else Modifier),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnilibrixImage(
                model = item.posterUrl,
                contentDescription = item.titleName,
                modifier = Modifier.size(width = Sizing.avatarSm, height = Sizing.avatarMd),
                shape = AnilibrixShapeExtras.poster,
            )
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Серия ${item.episodeNumber}",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.statusLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = when (item.state) {
                        DownloadState.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (item.state.isActive) {
                    Spacer(Modifier.height(Spacing.xs))
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Spacing.xs)
                            .clip(AnilibrixShapeExtras.pill),
                    )
                }
            }

            if (item.state == DownloadState.PAUSED && onResume != null) {
                IconButton(onClick = onResume) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Возобновить")
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun DownloadItem.statusLabel(): String = when (state) {
    DownloadState.QUEUED -> "В очереди · ${quality}p"
    DownloadState.DOWNLOADING -> {
        val percent = (progress * 100).toInt()
        val size = totalBytes?.let { " из ${formatBytes(it)}" }.orEmpty()
        "$percent% · ${formatBytes(downloadedBytes)}$size"
    }
    DownloadState.PAUSED -> "Приостановлено · ожидает сети"
    DownloadState.COMPLETED -> "${quality}p · ${formatBytes(downloadedBytes)}"
    DownloadState.FAILED -> "Не удалось скачать — нажмите, чтобы повторить"
    DownloadState.REMOVING -> "Удаляется…"
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 МБ"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) {
        String.format(Locale.getDefault(), "%.1f ГБ", mb / 1024)
    } else {
        String.format(Locale.getDefault(), "%.0f МБ", mb)
    }
}
