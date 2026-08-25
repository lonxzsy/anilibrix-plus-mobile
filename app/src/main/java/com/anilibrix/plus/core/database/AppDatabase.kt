package com.anilibrix.plus.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.anilibrix.plus.core.database.dao.CollectionDao
import com.anilibrix.plus.core.database.dao.DownloadDao
import com.anilibrix.plus.core.database.dao.FavoriteDao
import com.anilibrix.plus.core.database.dao.HistoryDao
import com.anilibrix.plus.core.database.dao.PlaylistDao
import com.anilibrix.plus.core.database.dao.PlaylistItemDao
import com.anilibrix.plus.core.database.dao.RatingDao
import com.anilibrix.plus.core.database.dao.SyncOperationDao
import com.anilibrix.plus.core.database.entity.CollectionEntity
import com.anilibrix.plus.core.database.entity.DownloadEntity
import com.anilibrix.plus.core.database.entity.FavoriteEntity
import com.anilibrix.plus.core.database.entity.HistoryEntity
import com.anilibrix.plus.core.database.entity.PlaylistEntity
import com.anilibrix.plus.core.database.entity.PlaylistItemEntity
import com.anilibrix.plus.core.database.entity.RatingEntity
import com.anilibrix.plus.core.database.entity.SyncOperationEntity

/**
 * Версия 5: `watch_later` слита в `collections`, у истории появился серверный
 * ключ серии, добавлены таблицы загрузок и очереди синхронизации.
 * См. [MIGRATION_4_5].
 */
@Database(
    entities = [
        FavoriteEntity::class,
        HistoryEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        RatingEntity::class,
        CollectionEntity::class,
        DownloadEntity::class,
        SyncOperationEntity::class,
        com.anilibrix.plus.core.database.entity.TorrentDownloadEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AnilibrixDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistItemDao(): PlaylistItemDao
    abstract fun ratingDao(): RatingDao
    abstract fun collectionDao(): CollectionDao
    abstract fun downloadDao(): DownloadDao
    abstract fun syncOperationDao(): SyncOperationDao
    abstract fun torrentDownloadDao(): com.anilibrix.plus.core.database.dao.TorrentDownloadDao
}
