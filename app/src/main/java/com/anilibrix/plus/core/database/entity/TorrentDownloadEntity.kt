package com.anilibrix.plus.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "torrent_downloads", indices = [Index("titleId")])
data class TorrentDownloadEntity(
    @PrimaryKey val id: String,
    val titleId: Long,
    val titleName: String = "",
    val posterUrl: String? = null,
    val torrentName: String = "",
    val releaseGroup: String? = null,
    val quality: String? = null,
    val magnetOrUrl: String = "",
    val state: String = "QUEUED",
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val filesJson: String = "[]",
    val saveDirectory: String = "",
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
