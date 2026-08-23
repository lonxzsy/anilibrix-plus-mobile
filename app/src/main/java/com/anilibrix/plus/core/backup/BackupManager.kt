package com.anilibrix.plus.core.backup

import com.anilibrix.plus.core.database.dao.CollectionDao
import com.anilibrix.plus.core.database.dao.FavoriteDao
import com.anilibrix.plus.core.database.dao.HistoryDao
import com.anilibrix.plus.core.database.dao.PlaylistDao
import com.anilibrix.plus.core.database.dao.PlaylistItemDao
import com.anilibrix.plus.core.database.dao.RatingDao
import com.anilibrix.plus.core.database.entity.CollectionEntity
import com.anilibrix.plus.core.database.entity.FavoriteEntity
import com.anilibrix.plus.core.database.entity.HistoryEntity
import com.anilibrix.plus.core.database.entity.PlaylistEntity
import com.anilibrix.plus.core.database.entity.PlaylistItemEntity
import com.anilibrix.plus.core.database.entity.RatingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val history: List<HistoryBackupDto> = emptyList(),
    val favorites: List<FavoriteBackupDto> = emptyList(),
    val collections: List<CollectionBackupDto> = emptyList(),
    val playlists: List<PlaylistBackupDto> = emptyList(),
    val ratings: List<RatingBackupDto> = emptyList(),
)

@Serializable
data class HistoryBackupDto(
    val titleId: Long,
    val titleName: String,
    val posterUrl: String?,
    val episodeId: Long,
    val episodeNumber: Int,
    val timestamp: Long,
    val duration: Long,
    val watchedAt: Long,
    val releaseEpisodeId: String = "",
)

@Serializable
data class FavoriteBackupDto(
    val titleId: Long,
    val titleName: String,
    val posterUrl: String?,
    val addedAt: Long,
)

@Serializable
data class CollectionBackupDto(
    val titleId: Long,
    val titleName: String,
    val posterUrl: String?,
    val collectionType: String,
    val progressEpisode: Int,
    val addedAt: Long,
    val updatedAt: Long,
    val shikimoriId: Int? = null,
)

@Serializable
data class PlaylistBackupDto(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val items: List<PlaylistItemBackupDto> = emptyList(),
)

@Serializable
data class PlaylistItemBackupDto(
    val playlistId: Long,
    val titleId: Long,
    val titleName: String,
)

@Serializable
data class RatingBackupDto(
    val titleId: Long,
    val rating: Float,
)

data class RestoreResult(
    val historyCount: Int,
    val favoritesCount: Int,
    val collectionsCount: Int,
    val playlistsCount: Int,
    val ratingsCount: Int,
)

@Singleton
class BackupManager @Inject constructor(
    private val historyDao: HistoryDao,
    private val favoriteDao: FavoriteDao,
    private val collectionDao: CollectionDao,
    private val playlistDao: PlaylistDao,
    private val playlistItemDao: PlaylistItemDao,
    private val ratingDao: RatingDao,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun createBackup(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val history = historyDao.getAll().first().map {
                HistoryBackupDto(
                    titleId = it.titleId,
                    titleName = it.titleName,
                    posterUrl = it.posterUrl,
                    episodeId = it.episodeId,
                    episodeNumber = it.episodeNumber,
                    timestamp = it.timestamp,
                    duration = it.duration,
                    watchedAt = it.watchedAt,
                    releaseEpisodeId = it.releaseEpisodeId,
                )
            }

            val favorites = favoriteDao.getAll().first().map {
                FavoriteBackupDto(
                    titleId = it.titleId,
                    titleName = it.titleName,
                    posterUrl = it.posterUrl,
                    addedAt = it.addedAt,
                )
            }

            val collections = collectionDao.getAll().first().map {
                CollectionBackupDto(
                    titleId = it.titleId,
                    titleName = it.titleName,
                    posterUrl = it.posterUrl,
                    collectionType = it.collectionType,
                    progressEpisode = it.progressEpisode,
                    addedAt = it.addedAt,
                    updatedAt = it.updatedAt,
                    shikimoriId = it.shikimoriId,
                )
            }

            val playlists = playlistDao.getAll().first().map { pl ->
                val items = playlistItemDao.getByPlaylistId(pl.id).map { item ->
                    PlaylistItemBackupDto(
                        playlistId = item.playlistId,
                        titleId = item.titleId,
                        titleName = item.titleName,
                    )
                }
                PlaylistBackupDto(
                    id = pl.id,
                    name = pl.name,
                    createdAt = pl.createdAt,
                    items = items,
                )
            }

            val ratings = ratingDao.getAll().first().map {
                RatingBackupDto(
                    titleId = it.titleId,
                    rating = it.rating,
                )
            }

            val backup = BackupData(
                history = history,
                favorites = favorites,
                collections = collections,
                playlists = playlists,
                ratings = ratings,
            )

            val jsonString = json.encodeToString(backup)
            outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
            outputStream.flush()

            history.size + favorites.size + collections.size + playlists.size + ratings.size
        }
    }

    suspend fun restoreBackup(inputStream: InputStream): Result<RestoreResult> = withContext(Dispatchers.IO) {
        runCatching {
            val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val backup = json.decodeFromString<BackupData>(content)

            // Восстановление истории
            val historyEntities = backup.history.map {
                HistoryEntity(
                    titleId = it.titleId,
                    titleName = it.titleName,
                    posterUrl = it.posterUrl,
                    episodeId = it.episodeId,
                    episodeNumber = it.episodeNumber,
                    timestamp = it.timestamp,
                    duration = it.duration,
                    watchedAt = it.watchedAt,
                    releaseEpisodeId = it.releaseEpisodeId,
                )
            }
            if (historyEntities.isNotEmpty()) {
                historyDao.insertAll(historyEntities)
            }

            // Восстановление избранного
            backup.favorites.forEach {
                favoriteDao.insert(
                    FavoriteEntity(
                        titleId = it.titleId,
                        titleName = it.titleName,
                        posterUrl = it.posterUrl,
                        addedAt = it.addedAt,
                    )
                )
            }

            // Восстановление коллекций
            val collectionEntities = backup.collections.map {
                CollectionEntity(
                    titleId = it.titleId,
                    titleName = it.titleName,
                    posterUrl = it.posterUrl,
                    collectionType = it.collectionType,
                    progressEpisode = it.progressEpisode,
                    addedAt = it.addedAt,
                    updatedAt = it.updatedAt,
                    shikimoriId = it.shikimoriId,
                )
            }
            if (collectionEntities.isNotEmpty()) {
                collectionDao.insertAll(collectionEntities)
            }

            // Восстановление плейлистов
            backup.playlists.forEach { pl ->
                val plId = playlistDao.insert(
                    PlaylistEntity(
                        id = pl.id,
                        name = pl.name,
                        createdAt = pl.createdAt,
                    )
                )
                pl.items.forEach { item ->
                    playlistItemDao.insert(
                        PlaylistItemEntity(
                            playlistId = plId,
                            titleId = item.titleId,
                            titleName = item.titleName,
                        )
                    )
                }
            }

            // Восстановление оценок
            backup.ratings.forEach {
                ratingDao.insert(
                    RatingEntity(
                        titleId = it.titleId,
                        rating = it.rating,
                    )
                )
            }

            RestoreResult(
                historyCount = backup.history.size,
                favoritesCount = backup.favorites.size,
                collectionsCount = backup.collections.size,
                playlistsCount = backup.playlists.size,
                ratingsCount = backup.ratings.size,
            )
        }
    }
}
