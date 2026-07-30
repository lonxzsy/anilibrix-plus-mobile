package com.anilibrix.plus.app.di

import android.content.Context
import androidx.room.Room
import com.anilibrix.plus.core.database.ANILIBRIX_MIGRATIONS
import com.anilibrix.plus.core.database.AnilibrixDatabase
import com.anilibrix.plus.core.database.dao.CollectionDao
import com.anilibrix.plus.core.database.dao.DownloadDao
import com.anilibrix.plus.core.database.dao.FavoriteDao
import com.anilibrix.plus.core.database.dao.HistoryDao
import com.anilibrix.plus.core.database.dao.PlaylistDao
import com.anilibrix.plus.core.database.dao.PlaylistItemDao
import com.anilibrix.plus.core.database.dao.RatingDao
import com.anilibrix.plus.core.database.dao.SyncOperationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AnilibrixDatabase {
        return Room.databaseBuilder(
            context,
            AnilibrixDatabase::class.java,
            "anilibrix.db"
        )
            .addMigrations(*ANILIBRIX_MIGRATIONS)
            .build()
    }

    @Provides
    fun provideFavoriteDao(db: AnilibrixDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideHistoryDao(db: AnilibrixDatabase): HistoryDao = db.historyDao()

    @Provides
    fun providePlaylistDao(db: AnilibrixDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun providePlaylistItemDao(db: AnilibrixDatabase): PlaylistItemDao = db.playlistItemDao()

    @Provides
    fun provideRatingDao(db: AnilibrixDatabase): RatingDao = db.ratingDao()

    @Provides
    fun provideCollectionDao(db: AnilibrixDatabase): CollectionDao = db.collectionDao()

    @Provides
    fun provideDownloadDao(db: AnilibrixDatabase): DownloadDao = db.downloadDao()

    @Provides
    fun provideSyncOperationDao(db: AnilibrixDatabase): SyncOperationDao = db.syncOperationDao()
}
