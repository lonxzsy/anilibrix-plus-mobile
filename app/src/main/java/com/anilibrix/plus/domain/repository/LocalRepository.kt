package com.anilibrix.plus.domain.repository

import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.FavoriteTitle
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.Playlist
import com.anilibrix.plus.domain.model.PlaylistItem
import kotlinx.coroutines.flow.Flow

interface LocalRepository {
    fun getFavorites(): Flow<List<FavoriteTitle>>
    suspend fun isFavorite(titleId: Long): Boolean
    suspend fun addFavorite(titleId: Long, titleName: String, posterUrl: String?)
    suspend fun removeFavorite(titleId: Long)
    fun getHistory(): Flow<List<HistoryEntry>>
    suspend fun addHistory(entry: HistoryEntry)
    suspend fun deleteHistory(titleId: Long, episodeId: Long)
    suspend fun clearHistory()
    fun getPlaylists(): Flow<List<Playlist>>
    suspend fun createPlaylist(name: String): Long
    suspend fun updatePlaylist(playlist: Playlist)
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun getPlaylistItems(playlistId: Long): List<PlaylistItem>
    suspend fun addPlaylistItem(playlistId: Long, titleId: Long, titleName: String)
    suspend fun removePlaylistItem(playlistId: Long, titleId: Long)
    fun getWatchLater(): Flow<List<FavoriteTitle>>
    suspend fun isInWatchLater(titleId: Long): Boolean
    suspend fun addWatchLater(titleId: Long, titleName: String = "", posterUrl: String? = null)
    suspend fun removeWatchLater(titleId: Long)
    fun getCollections(collectionType: CollectionType): Flow<List<FavoriteTitle>>
    suspend fun isInCollection(titleId: Long, collectionType: CollectionType): Boolean
    suspend fun addToCollection(titleId: Long, collectionType: CollectionType, titleName: String = "", posterUrl: String? = null)
    suspend fun removeFromCollection(titleId: Long, collectionType: CollectionType)
    suspend fun getCollectionTypesForTitle(titleId: Long): List<CollectionType>
    suspend fun getRating(titleId: Long): Float?
    fun getAllRatings(): Flow<Map<Long, Float>>
    suspend fun setRating(titleId: Long, rating: Float)
    suspend fun deleteRating(titleId: Long)
}
