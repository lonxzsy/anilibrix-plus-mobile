package com.anilibrix.plus.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collections", primaryKeys = ["titleId", "collectionType"])
data class CollectionEntity(
    val titleId: Long,
    val collectionType: String,
    val titleName: String = "",
    val posterUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
