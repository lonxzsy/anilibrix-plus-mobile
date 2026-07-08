package com.anilibrix.plus.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_later")
data class WatchLaterEntity(
    @PrimaryKey val titleId: Long,
    val titleName: String = "",
    val posterUrl: String? = null
)
