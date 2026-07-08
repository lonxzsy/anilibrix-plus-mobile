package com.anilibrix.plus.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.anilibrix.plus.core.database.dao.CollectionDao
import com.anilibrix.plus.core.database.dao.FavoriteDao
import com.anilibrix.plus.core.database.dao.HistoryDao
import com.anilibrix.plus.core.database.dao.PlaylistDao
import com.anilibrix.plus.core.database.dao.PlaylistItemDao
import com.anilibrix.plus.core.database.dao.RatingDao
import com.anilibrix.plus.core.database.dao.WatchLaterDao
import com.anilibrix.plus.core.database.entity.CollectionEntity
import com.anilibrix.plus.core.database.entity.FavoriteEntity
import com.anilibrix.plus.core.database.entity.HistoryEntity
import com.anilibrix.plus.core.database.entity.PlaylistEntity
import com.anilibrix.plus.core.database.entity.PlaylistItemEntity
import com.anilibrix.plus.core.database.entity.RatingEntity
import com.anilibrix.plus.core.database.entity.WatchLaterEntity

@Database(
    entities = [
        FavoriteEntity::class,
        HistoryEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        WatchLaterEntity::class,
        RatingEntity::class,
        CollectionEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AnilibrixDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistItemDao(): PlaylistItemDao
    abstract fun watchLaterDao(): WatchLaterDao
    abstract fun ratingDao(): RatingDao
    abstract fun collectionDao(): CollectionDao
}
