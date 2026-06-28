package com.anilibrix.plus.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "titleId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId")]
)
data class PlaylistItemEntity(
    val playlistId: Long,
    val titleId: Long,
    val titleName: String
)
