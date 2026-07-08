package com.anilibrix.plus.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ratings")
data class RatingEntity(
    @PrimaryKey val titleId: Long,
    val rating: Float
)
