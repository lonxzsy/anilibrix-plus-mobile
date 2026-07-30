package com.anilibrix.plus.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val titleId: Long,
    val titleName: String,
    val posterUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
