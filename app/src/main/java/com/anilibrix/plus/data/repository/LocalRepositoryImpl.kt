package com.anilibrix.plus.data.repository

import com.anilibrix.plus.core.database.dao.CollectionDao
import com.anilibrix.plus.core.database.dao.FavoriteDao
import com.anilibrix.plus.core.database.dao.HistoryDao
import com.anilibrix.plus.core.database.dao.PlaylistDao
import com.anilibrix.plus.core.database.dao.PlaylistItemDao
import com.anilibrix.plus.core.database.dao.RatingDao
import com.anilibrix.plus.core.database.dao.WatchLaterDao
import com.anilibrix.plus.core.database.entity.CollectionEntity
import com.anilibrix.plus.core.database.entity.FavoriteEntity
import com.anilibrix.plus.core.database.entity.PlaylistEntity
import com.anilibrix.plus.core.database.entity.RatingEntity
import com.anilibrix.plus.core.database.entity.WatchLaterEntity
import com.anilibrix.plus.data.local.mapper.toDomain
import com.anilibrix.plus.data.local.mapper.toEntity
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.FavoriteTitle
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.Playlist
import com.anilibrix.plus.domain.model.PlaylistItem
import com.anilibrix.plus.domain.repository.LocalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao,
    private val playlistDao: PlaylistDao,
    private val playlistItemDao: PlaylistItemDao,
    private val watchLaterDao: WatchLaterDao,
    private val ratingDao: RatingDao,
    private val collectionDao: CollectionDao
) : LocalRepository {

    override fun getFavorites(): Flow<List<FavoriteTitle>> {
        return favoriteDao.getAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun isFavorite(titleId: Long): Boolean {
        return favoriteDao.isFavorite(titleId)
    }

    override suspend fun addFavorite(titleId: Long, titleName: String, posterUrl: String?) {
        favoriteDao.insert(FavoriteEntity(titleId = titleId, titleName = titleName, posterUrl = posterUrl))
    }

    override suspend fun removeFavorite(titleId: Long) {
        favoriteDao.deleteByTitleId(titleId)
    }

    override fun getHistory(): Flow<List<HistoryEntry>> {
        return historyDao.getAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addHistory(entry: HistoryEntry) {
        historyDao.insert(entry.toEntity())
    }

    override suspend fun deleteHistory(titleId: Long, episodeId: Long) {
        historyDao.delete(titleId, episodeId)
    }

    override suspend fun clearHistory() {
        historyDao.deleteAll()
    }

    override suspend fun clearAccountData() {
        favoriteDao.deleteAll()
        watchLaterDao.deleteAll()
        collectionDao.deleteAll()
        ratingDao.deleteAll()
        historyDao.deleteAll()
        playlistItemDao.deleteAll()
        playlistDao.deleteAll()
    }

    override fun getPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAll().map { entities ->
            entities.map { entity ->
                val items = playlistItemDao.getByPlaylistId(entity.id).map { it.toDomain() }
                entity.toDomain(items)
            }
        }
    }

    override suspend fun createPlaylist(name: String): Long {
        val entity = PlaylistEntity(name = name)
        return playlistDao.insert(entity)
    }

    override suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.update(PlaylistEntity(id = playlist.id, name = playlist.name, createdAt = playlist.createdAt))
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.getById(playlistId)?.let { playlistDao.delete(it) }
    }

    override suspend fun getPlaylistItems(playlistId: Long): List<PlaylistItem> {
        return playlistItemDao.getByPlaylistId(playlistId).map { it.toDomain() }
    }

    override suspend fun addPlaylistItem(playlistId: Long, titleId: Long, titleName: String) {
        playlistItemDao.insert(
            com.anilibrix.plus.core.database.entity.PlaylistItemEntity(
                playlistId = playlistId,
                titleId = titleId,
                titleName = titleName
            )
        )
    }

    override suspend fun removePlaylistItem(playlistId: Long, titleId: Long) {
        playlistItemDao.delete(playlistId, titleId)
    }

    override fun getWatchLater(): Flow<List<FavoriteTitle>> {
        return watchLaterDao.getAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun isInWatchLater(titleId: Long): Boolean {
        return watchLaterDao.isInWatchLater(titleId)
    }

    override suspend fun addWatchLater(titleId: Long, titleName: String, posterUrl: String?) {
        watchLaterDao.insert(WatchLaterEntity(titleId = titleId, titleName = titleName, posterUrl = posterUrl))
    }

    override suspend fun removeWatchLater(titleId: Long) {
        watchLaterDao.delete(titleId)
    }

    override fun getCollections(collectionType: CollectionType): Flow<List<FavoriteTitle>> {
        return collectionDao.getByType(collectionType.value).map { list ->
            list.map {
                FavoriteTitle(
                    titleId = it.titleId,
                    titleName = it.titleName,
                    posterUrl = it.posterUrl
                )
            }
        }
    }

    override suspend fun isInCollection(titleId: Long, collectionType: CollectionType): Boolean {
        return collectionDao.exists(titleId, collectionType.value)
    }

    override suspend fun addToCollection(titleId: Long, collectionType: CollectionType, titleName: String, posterUrl: String?) {
        collectionDao.insert(
            CollectionEntity(
                titleId = titleId,
                collectionType = collectionType.value,
                titleName = titleName,
                posterUrl = posterUrl
            )
        )
    }

    override suspend fun removeFromCollection(titleId: Long, collectionType: CollectionType) {
        collectionDao.delete(titleId, collectionType.value)
    }

    override suspend fun getCollectionTypesForTitle(titleId: Long): List<CollectionType> {
        return collectionDao.getTypesForTitle(titleId).mapNotNull { typeString ->
            CollectionType.fromValue(typeString)
        }
    }

    override suspend fun getRating(titleId: Long): Float? {
        return ratingDao.getByTitleId(titleId)?.rating
    }

    override fun getAllRatings(): Flow<Map<Long, Float>> {
        return ratingDao.getAll().map { list ->
            list.associate { it.titleId to it.rating }
        }
    }

    override suspend fun setRating(titleId: Long, rating: Float) {
        ratingDao.insert(RatingEntity(titleId = titleId, rating = rating))
    }

    override suspend fun deleteRating(titleId: Long) {
        ratingDao.delete(titleId)
    }
}
