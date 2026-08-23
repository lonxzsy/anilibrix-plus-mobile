package com.anilibrix.plus.ui.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

fun LazyListScope.torrentsSection(
    state: DetailUiState,
    onIntent: (DetailIntent) -> Unit
) {
    if (state.torrents.isEmpty()) {
        item {
            EmptyState(
                kind = EmptyKind.Torrents,
                title = "Торренты не найдены",
                subtitle = "Раздачи появятся после обработки релиза",
                modifier = Modifier.padding(vertical = Spacing.xl)
            )
        }
        return
    }

    items(
        items = state.torrents,
        key = { it.id }
    ) { torrent ->
        TorrentCardItem(
            torrent = torrent,
            onClick = {
                torrent.magnet?.let { magnet ->
                    onIntent(DetailIntent.OpenMagnet(magnet))
                }
            },
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
fun TorrentCardItem(
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
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    if (torrent.quality != null) {
                        Text(
                            text = torrent.quality,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (torrent.series != null) {
                        Text(
                            text = torrent.series,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (torrent.size != null && torrent.size > 0) {
                    val sizeGb = torrent.size / (1024.0 * 1024.0 * 1024.0)
                    Text(
                        text = String.format("%.1f ГБ", sizeGb),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    if (torrent.seeders != null) {
                        Text(
                            text = "↑ ${torrent.seeders}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.extended.seeders
                        )
                    }
                    if (torrent.leechers != null) {
                        Text(
                            text = "↓ ${torrent.leechers}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.extended.leechers
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = "Скачать торрент",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Sizing.iconMd)
            )
        }
    }
}
