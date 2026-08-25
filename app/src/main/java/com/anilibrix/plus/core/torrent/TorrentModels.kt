package com.anilibrix.plus.core.torrent

import kotlinx.serialization.Serializable

enum class TorrentDownloadState {
    QUEUED,
    FETCHING_METADATA,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    ERROR;

    val isActive: Boolean get() = this == QUEUED || this == FETCHING_METADATA || this == DOWNLOADING
}

@Serializable
data class TorrentFileItem(
    val index: Int,
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val downloadedBytes: Long = 0L,
    val selected: Boolean = true,
    val isCompleted: Boolean = false,
    val episodeNumber: Int? = null
)

data class TorrentTaskInfo(
    val id: String,
    val titleId: Long,
    val titleName: String,
    val posterUrl: String?,
    val torrentName: String,
    val releaseGroup: String?,
    val quality: String?,
    val magnetOrUrl: String,
    val state: TorrentDownloadState,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadSpeedBytesPerSec: Long = 0L,
    val uploadSpeedBytesPerSec: Long = 0L,
    val seeds: Int = 0,
    val peers: Int = 0,
    val files: List<TorrentFileItem> = emptyList(),
    val errorMessage: String? = null,
    val saveDirectory: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
