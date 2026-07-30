package com.anilibrix.plus.domain.repository

import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.FavoriteTitle
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.Playlist
import com.anilibrix.plus.domain.model.PlaylistItem
import com.anilibrix.plus.domain.model.TitleProgress
import kotlinx.coroutines.flow.Flow

interface LocalRepository {
    fun getFavorites(): Flow<List<FavoriteTitle>>
    suspend fun isFavorite(titleId: Long): Boolean
    suspend fun addFavorite(titleId: Long, titleName: String, posterUrl: String?)
    suspend fun removeFavorite(titleId: Long)

    fun getHistory(): Flow<List<HistoryEntry>>

    /** Последняя запись по каждому тайтлу — для «Продолжить просмотр». */
    fun getContinueWatching(): Flow<List<HistoryEntry>>
    suspend fun addHistory(entry: HistoryEntry)
    suspend fun deleteHistory(titleId: Long, episodeId: Long)
    suspend fun clearHistory()
    suspend fun getHistoryEntry(titleId: Long, episodeId: Long): HistoryEntry?

    /**
     * Суммарное время просмотра в миллисекундах: досмотренные серии целиком,
     * недосмотренные — по позиции остановки.
     */
    suspend fun getTotalWatchTimeMs(): Long
    suspend fun getHistoryCount(): Int

    /** Прогресс по тайтлу: что просмотрено, где остановились. */
    fun getTitleProgress(titleId: Long, totalEpisodes: Int): Flow<TitleProgress>
    suspend fun getTitleProgressOnce(titleId: Long, totalEpisodes: Int): TitleProgress

    /**
     * Отмечает серию просмотренной без её открытия.
     *
     * Пишет позицию, равную длительности, — так отметка не расходится с
     * подсчётом прогресса, который смотрит именно на позицию.
     */
    suspend fun markEpisodeWatched(entry: HistoryEntry)

    /** Отмечает просмотренными все серии до указанной включительно. */
    suspend fun markEpisodesWatchedUpTo(entries: List<HistoryEntry>)

    /**
     * Стирает **всё** локальное: избранное, историю, коллекции, оценки, плейлисты.
     *
     * Раньше это называлось `clearAccountData()` и вызывалось при выходе из
     * аккаунта — прямо вопреки тексту диалога выхода, который обещает, что
     * локальные данные останутся. Теперь у операции честное имя, и вызывает её
     * только явный пункт «Удалить локальные данные» с подтверждением.
     */
    suspend fun clearAllUserData()

    fun getPlaylists(): Flow<List<Playlist>>
    suspend fun createPlaylist(name: String): Long
    suspend fun renamePlaylist(playlistId: Long, name: String)
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun getPlaylistItems(playlistId: Long): List<PlaylistItem>
    suspend fun addPlaylistItem(playlistId: Long, titleId: Long, titleName: String)
    suspend fun removePlaylistItem(playlistId: Long, titleId: Long)

    fun getWatchLater(): Flow<List<FavoriteTitle>>
    suspend fun isInWatchLater(titleId: Long): Boolean
    suspend fun addWatchLater(titleId: Long, titleName: String = "", posterUrl: String? = null)
    suspend fun removeWatchLater(titleId: Long)

    fun getCollections(collectionType: CollectionType): Flow<List<FavoriteTitle>>
    fun getCollectionCounts(): Flow<Map<CollectionType, Int>>
    suspend fun isInCollection(titleId: Long, collectionType: CollectionType): Boolean

    /** Текущий статус тайтла, `null` — если он ни в одном списке. */
    suspend fun getCollectionType(titleId: Long): CollectionType?
    fun observeCollectionType(titleId: Long): Flow<CollectionType?>

    /**
     * Ставит тайтлу единственный статус, снимая предыдущий.
     *
     * Статусы взаимоисключающие: тайтл не может быть одновременно «смотрю» и
     * «брошено».
     */
    suspend fun setCollectionType(
        titleId: Long,
        collectionType: CollectionType,
        titleName: String = "",
        posterUrl: String? = null,
    )

    suspend fun removeFromCollections(titleId: Long)
    suspend fun getCollectionTypesForTitle(titleId: Long): List<CollectionType>

    /** Номер последней просмотренной серии, каким его видит внешний трекер. */
    suspend fun updateCollectionProgress(titleId: Long, episode: Int)

    suspend fun getRating(titleId: Long): Float?
    fun getAllRatings(): Flow<Map<Long, Float>>
    suspend fun setRating(titleId: Long, rating: Float)
    suspend fun deleteRating(titleId: Long)
}
