package com.anilibrix.plus.ui.detail.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.RemoveDone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anilibrix.plus.core.download.DownloadItem
import com.anilibrix.plus.core.download.DownloadState
import com.anilibrix.plus.domain.model.Episode
import com.anilibrix.plus.domain.model.EpisodeProgress
import com.anilibrix.plus.ui.components.EmptyKind
import com.anilibrix.plus.ui.components.EmptyState
import com.anilibrix.plus.ui.components.pressScale
import com.anilibrix.plus.ui.components.rememberHaptics
import com.anilibrix.plus.ui.detail.DetailIntent
import com.anilibrix.plus.ui.detail.DetailUiState
import com.anilibrix.plus.ui.theme.Elevation
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing

private const val EPISODES_PER_CHUNK = 24

fun LazyListScope.episodesSection(
    state: DetailUiState,
    onIntent: (DetailIntent) -> Unit,
    onPlayEpisode: (Long, Long) -> Unit,
) {
    val episodes = state.voiceoverEpisodes ?: state.title?.episodes.orEmpty()
    val titleId = state.title?.id ?: 0L

    // Селектор озвучек и провайдеров
    item {
        VoiceoverBarItem(
            selectedVoiceover = state.selectedVoiceover,
            availableCount = state.availableVoiceovers.size,
            isLoading = state.isVoiceoverLoading,
            onClick = { onIntent(DetailIntent.ShowVoiceoverSheet) }
        )
        Spacer(Modifier.height(Spacing.xs))
    }

    if (state.isVoiceoverLoading && episodes.isEmpty()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        }
        return
    }

    if (episodes.isEmpty()) {
        item {
            EmptyState(
                kind = EmptyKind.Episodes,
                title = "Эпизоды не найдены",
                subtitle = "Попробуйте выбрать другой вариант озвучки выше",
                modifier = Modifier.padding(vertical = Spacing.xl)
            )
        }
        return
    }

    // Фильтр по диапазонам серий для длинных тайтлов
    if (episodes.size > EPISODES_PER_CHUNK) {
        item {
            EpisodeChunkSelector(
                totalEpisodes = episodes.size,
                onChunkSelected = { }
            )
            Spacer(Modifier.height(Spacing.sm))
        }
    }

    // Быстрые действия: «Скачать следующие N»
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Все серии (${episodes.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedButton(
                onClick = { onIntent(DetailIntent.DownloadNext(5)) },
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Скачать 5 серий", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(Spacing.sm))
    }

    items(
        items = episodes,
        key = { ep -> "ep_${ep.id}" }
    ) { episode ->
        val progress = state.progress.episodes[episode.id]
        val download = state.downloads[episode.id]

        EpisodeCardItem(
            episode = episode,
            progress = progress,
            download = download,
            onClick = { onPlayEpisode(titleId, episode.id) },
            onToggleWatched = { onIntent(DetailIntent.ToggleEpisodeWatched(episode)) },
            onMarkUpTo = { onIntent(DetailIntent.MarkWatchedUpTo(episode)) },
            onDownload = { onIntent(DetailIntent.DownloadEpisode(episode)) },
            onCancelDownload = { onIntent(DetailIntent.CancelDownload(episode)) },
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun EpisodeChunkSelector(
    totalEpisodes: Int,
    onChunkSelected: (Int) -> Unit
) {
    var selectedChunk by remember { mutableIntStateOf(0) }
    val chunkCount = (totalEpisodes + EPISODES_PER_CHUNK - 1) / EPISODES_PER_CHUNK
    val haptics = rememberHaptics()

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(chunkCount) { chunkIndex ->
            val start = chunkIndex * EPISODES_PER_CHUNK + 1
            val end = ((chunkIndex + 1) * EPISODES_PER_CHUNK).coerceAtMost(totalEpisodes)
            val selected = selectedChunk == chunkIndex

            FilterChip(
                selected = selected,
                onClick = {
                    haptics.tick()
                    selectedChunk = chunkIndex
                    onChunkSelected(chunkIndex)
                },
                label = { Text("$start–$end") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeCardItem(
    episode: Episode,
    progress: EpisodeProgress?,
    download: DownloadItem?,
    onClick: () -> Unit,
    onToggleWatched: () -> Unit,
    onMarkUpTo: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()
    val isWatched = progress?.isWatched == true
    val fraction = progress?.fraction ?: 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptics.longPress()
                        menuOpen = true
                    },
                )
        ) {
            Row(
                modifier = Modifier.padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Номер / галочка
                Box(
                    modifier = Modifier
                        .size(Sizing.touchTarget)
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (isWatched) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isWatched,
                        transitionSpec = {
                            (fadeIn(MotionTokens.effectsDefault()) +
                                scaleIn(MotionTokens.spatialDefault(), initialScale = 0.6f)) togetherWith
                                fadeOut(MotionTokens.effectsFast())
                        },
                        label = "episodeWatched",
                    ) { watched ->
                        if (watched) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Просмотрено",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(Sizing.iconMd),
                            )
                        } else {
                            Text(
                                text = episode.ordinal.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(Spacing.md))

                // Название серии и длительность
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Серия ${episode.ordinal}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (episode.name.isNotBlank()) {
                        Text(
                            text = episode.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (episode.duration > 0) {
                        Text(
                            text = formatEpisodeDuration(episode.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Кнопка загрузки серии
                when (download?.state) {
                    DownloadState.COMPLETED -> {
                        IconButton(onClick = onCancelDownload) {
                            Icon(
                                imageVector = Icons.Rounded.DownloadDone,
                                contentDescription = "Скачано (нажмите для удаления)",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Sizing.iconMd),
                            )
                        }
                    }
                    DownloadState.DOWNLOADING -> {
                        IconButton(onClick = onCancelDownload) {
                            CircularProgressIndicator(
                                progress = { download.progress.coerceIn(0f, 1f) },
                                modifier = Modifier.size(Sizing.iconMd),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    DownloadState.QUEUED, DownloadState.REMOVING -> {
                        IconButton(onClick = onCancelDownload) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(Sizing.iconMd),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    DownloadState.FAILED -> {
                        IconButton(onClick = onDownload) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = "Ошибка загрузки (повторить)",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(Sizing.iconMd),
                            )
                        }
                    }
                    null, DownloadState.PAUSED -> {
                        IconButton(onClick = onDownload) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Скачать",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(Sizing.iconMd),
                            )
                        }
                    }
                }

                // Контекстное меню
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(if (isWatched) "Снять отметку" else "Отметить просмотренной")
                        },
                        leadingIcon = {
                            Icon(
                                if (isWatched) Icons.Rounded.RemoveDone else Icons.Rounded.Check,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onToggleWatched()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Отметить все до этой") },
                        leadingIcon = {
                            Icon(Icons.Rounded.DoneAll, contentDescription = null)
                        },
                        onClick = {
                            menuOpen = false
                            onMarkUpTo()
                        },
                    )
                }
            }

            // Полоса прогресса просмотра
            if (fraction > 0f && !isWatched) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

private fun formatEpisodeDuration(totalSec: Int): String {
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%d:%02d", min, sec)
}

@Composable
fun VoiceoverBarItem(
    selectedVoiceover: com.anilibrix.plus.domain.model.VoiceoverOption?,
    availableCount: Int,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val name = selectedVoiceover?.name ?: "AniLibria"
    val providerName = selectedVoiceover?.provider?.displayName ?: "Официальный"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(),
        shape = MaterialTheme.shapes.medium,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.DoneAll,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(Spacing.md))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Озвучка: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "$providerName • ${if (availableCount > 0) "вариантов: $availableCount" else "поиск студий..."}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(Spacing.xs))
                }
                Text(
                    text = "Сменить",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

