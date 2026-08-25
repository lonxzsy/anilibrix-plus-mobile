package com.anilibrix.plus.ui.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anilibrix.plus.core.torrent.TorrentDownloadState
import com.anilibrix.plus.core.torrent.TorrentTaskInfo
import com.anilibrix.plus.domain.model.Torrent
import com.anilibrix.plus.ui.components.EmptyKind
import com.anilibrix.plus.ui.components.EmptyState
import com.anilibrix.plus.ui.components.pressScale
import com.anilibrix.plus.ui.detail.DetailIntent
import com.anilibrix.plus.ui.detail.DetailUiState
import com.anilibrix.plus.ui.theme.Elevation
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing
import com.anilibrix.plus.ui.theme.extended
import java.util.Locale

fun LazyListScope.torrentsSection(
    state: DetailUiState,
    onIntent: (DetailIntent) -> Unit
) {
    // 1. Баннер активной загрузки торрента для текущего тайтла
    val activeTask = state.activeTorrentTasks.firstOrNull { it.state.isActive }
    if (activeTask != null) {
        item {
            ActiveTorrentBanner(task = activeTask)
            Spacer(modifier = Modifier.height(Spacing.sm))
        }
    }

    // 2. Переключатель источника (AniLibria / Nyaa.si)
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            FilterChip(
                selected = state.selectedTorrentSource == "anilibria",
                onClick = { onIntent(DetailIntent.SelectTorrentSource("anilibria")) },
                label = { Text("AniLibria (${state.torrents.size})") }
            )
            FilterChip(
                selected = state.selectedTorrentSource == "nyaa",
                onClick = { onIntent(DetailIntent.SelectTorrentSource("nyaa")) },
                label = { Text("Nyaa.si (${state.nyaaTorrents.size})") }
            )
        }
    }

    // 3. Поисковая строка ключевых слов
    item {
        OutlinedTextField(
            value = state.torrentSearchQuery,
            onValueChange = { onIntent(DetailIntent.SetTorrentSearchQuery(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.xs),
            placeholder = { Text("Поиск по группе, серии, качеству…") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (state.torrentSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { onIntent(DetailIntent.SetTorrentSearchQuery("")) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Очистить", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        )
    }

    val rawTorrents = if (state.selectedTorrentSource == "nyaa") state.nyaaTorrents else state.torrents

    // Сбор доступных серий для чип-фильтра
    val availableEpisodes = rawTorrents
        .flatMap { it.episodeNumbers }
        .distinct()
        .sorted()

    // 4. Горизонтальный селектор серий (если есть серии)
    if (availableEpisodes.isNotEmpty()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                FilterChip(
                    selected = state.selectedTorrentEpisodeFilter == null,
                    onClick = { onIntent(DetailIntent.SetTorrentEpisodeFilter(null)) },
                    label = { Text("Все серии") }
                )
                FilterChip(
                    selected = state.selectedTorrentEpisodeFilter == -1,
                    onClick = { onIntent(DetailIntent.SetTorrentEpisodeFilter(-1)) },
                    label = { Text("Пакеты / Сезон") }
                )
                availableEpisodes.forEach { epNum ->
                    FilterChip(
                        selected = state.selectedTorrentEpisodeFilter == epNum,
                        onClick = { onIntent(DetailIntent.SetTorrentEpisodeFilter(epNum)) },
                        label = { Text("Сер $epNum") }
                    )
                }
            }
        }
    }

    // 5. Фильтрация списка торрентов
    val query = state.torrentSearchQuery.trim().lowercase()
    val epFilter = state.selectedTorrentEpisodeFilter
    val qualityFilter = state.selectedTorrentQualityFilter

    val filteredTorrents = rawTorrents.filter { t ->
        val matchesQuery = if (query.isBlank()) true else {
            (t.rawTitle?.lowercase()?.contains(query) == true) ||
            (t.releaseGroup?.lowercase()?.contains(query) == true) ||
            (t.cleanTitle?.lowercase()?.contains(query) == true) ||
            (t.quality?.lowercase()?.contains(query) == true) ||
            (t.videoCodec?.lowercase()?.contains(query) == true) ||
            (t.audioInfo?.lowercase()?.contains(query) == true) ||
            (t.series?.lowercase()?.contains(query) == true)
        }

        val matchesEpisode = when (epFilter) {
            null -> true
            -1 -> t.isBatch || t.episodeNumbers.size > 1
            else -> t.episodeNumbers.contains(epFilter)
        }

        val matchesQuality = if (qualityFilter == null) true else {
            t.quality?.equals(qualityFilter, ignoreCase = true) == true
        }

        matchesQuery && matchesEpisode && matchesQuality
    }

    if (filteredTorrents.isEmpty()) {
        item {
            EmptyState(
                kind = EmptyKind.Torrents,
                title = if (rawTorrents.isEmpty()) "Раздачи не найдены" else "Ничего не найдено",
                subtitle = if (rawTorrents.isEmpty()) "Раздачи появятся после обработки" else "Попробуйте изменить параметры поиска",
                modifier = Modifier.padding(vertical = Spacing.xl)
            )
        }
        return
    }

    items(
        items = filteredTorrents,
        key = { "${state.selectedTorrentSource}_${it.id}" }
    ) { torrent ->
        EnhancedTorrentCardItem(
            torrent = torrent,
            onClick = { onIntent(DetailIntent.ClickTorrent(torrent)) },
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun ActiveTorrentBanner(task: TorrentTaskInfo) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Скачивается: ${task.torrentName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                val percent = (task.progress * 100).toInt()
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val speedMb = task.downloadSpeedBytesPerSec / (1024.0 * 1024.0)
                val speedText = if (speedMb >= 1.0) {
                    String.format(Locale.getDefault(), "%.1f МБ/с", speedMb)
                } else {
                    String.format(Locale.getDefault(), "%.0f КБ/с", task.downloadSpeedBytesPerSec / 1024.0)
                }
                Text(
                    text = "$speedText · ↑ ${task.seeds} пиров",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Встроенный загрузчик",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnhancedTorrentCardItem(
    torrent: Torrent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(),
        shape = MaterialTheme.shapes.medium,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0)
    ) {
        Column(modifier = Modifier.padding(Spacing.sm)) {
            // Верхняя строка с бейджами: Группа, Качество, Серия
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!torrent.releaseGroup.isNullOrBlank()) {
                    TagBadge(
                        text = torrent.releaseGroup,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (!torrent.quality.isNullOrBlank()) {
                    TagBadge(
                        text = torrent.quality,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                if (!torrent.series.isNullOrBlank()) {
                    TagBadge(
                        text = torrent.series,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                if (!torrent.videoCodec.isNullOrBlank()) {
                    TagBadge(
                        text = torrent.videoCodec,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!torrent.audioInfo.isNullOrBlank()) {
                    TagBadge(
                        text = torrent.audioInfo,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Название раздачи
            Text(
                text = torrent.rawTitle ?: torrent.cleanTitle ?: torrent.series ?: "Торрент",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Нижняя строка: Размер, Сиды/Личи и кнопка Скачать
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (torrent.size != null && torrent.size > 0) {
                        val sizeGb = torrent.size / (1024.0 * 1024.0 * 1024.0)
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f ГБ", sizeGb),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (torrent.seeders != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = null,
                                tint = MaterialTheme.extended.seeders,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${torrent.seeders}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.extended.seeders
                            )
                        }
                    }

                    if (torrent.leechers != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDownward,
                                contentDescription = null,
                                tint = MaterialTheme.extended.leechers,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${torrent.leechers}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.extended.leechers
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = "Скачать",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TagBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .background(color = containerColor, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}
