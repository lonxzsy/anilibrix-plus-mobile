package com.anilibrix.plus.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anilibrix.plus.core.torrent.TorrentFileItem
import com.anilibrix.plus.core.torrent.TorrentMetadataResolver
import com.anilibrix.plus.domain.model.Torrent
import java.util.Locale

@Composable
fun TorrentDownloadDialog(
    torrent: Torrent,
    metadata: TorrentMetadataResolver.ResolvedMetadata?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onStartDownload: (Torrent, Set<Int>?) -> Unit,
    onOpenMagnet: (String) -> Unit
) {
    val initialFiles = metadata?.files ?: emptyList()
    var selectedIndices by remember(metadata) {
        mutableStateOf(
            if (initialFiles.isNotEmpty()) {
                initialFiles.filter { it.selected }.map { it.index }.toSet()
            } else {
                emptySet()
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Скачивание раздачи",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Карточка с информацией о релизе
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = torrent.rawTitle ?: torrent.series ?: "Торрент-раздача",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!torrent.releaseGroup.isNullOrBlank()) {
                                BadgeChip(text = torrent.releaseGroup)
                            }
                            if (!torrent.quality.isNullOrBlank()) {
                                BadgeChip(text = torrent.quality)
                            }
                            if (torrent.size != null && torrent.size > 0) {
                                BadgeChip(text = formatFileSize(torrent.size))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Получение списка файлов…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (initialFiles.size > 1) {
                    // Список серий / файлов для батча
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Выберите серии (${selectedIndices.size} из ${initialFiles.size}):",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = {
                                selectedIndices = if (selectedIndices.size == initialFiles.size) {
                                    emptySet()
                                } else {
                                    initialFiles.map { it.index }.toSet()
                                }
                            }
                        ) {
                            Text(if (selectedIndices.size == initialFiles.size) "Снять все" else "Выбрать все")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(initialFiles, key = { it.index }) { file ->
                            val isChecked = selectedIndices.contains(file.index)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedIndices = if (isChecked) {
                                            selectedIndices - file.index
                                        } else {
                                            selectedIndices + file.index
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedIndices = if (checked) {
                                            selectedIndices + file.index
                                        } else {
                                            selectedIndices - file.index
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val label = if (file.episodeNumber != null) {
                                        "Серия ${file.episodeNumber}"
                                    } else {
                                        file.name
                                    }
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (file.sizeBytes > 0) {
                                        Text(
                                            text = formatFileSize(file.sizeBytes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Файл будет скачан в память устройства и станет доступен для просмотра офлайн во встроенном плеере.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onStartDownload(torrent, selectedIndices.takeIf { initialFiles.size > 1 })
                    onDismiss()
                },
                enabled = !isLoading && (initialFiles.isEmpty() || selectedIndices.isNotEmpty())
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Скачать в приложении")
            }
        },
        dismissButton = {
            Row {
                if (!torrent.magnet.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            onOpenMagnet(torrent.magnet)
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Магнет")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        }
    )
}

@Composable
private fun BadgeChip(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 1.0) {
        String.format(Locale.getDefault(), "%.1f ГБ", gb)
    } else {
        val mb = bytes / (1024.0 * 1024.0)
        String.format(Locale.getDefault(), "%.0f МБ", mb)
    }
}
