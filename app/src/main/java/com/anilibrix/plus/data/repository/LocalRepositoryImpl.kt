package com.anilibrix.plus.data.repository

import com.anilibrix.plus.core.database.dao.CollectionDao
import com.anilibrix.plus.core.database.dao.FavoriteDao
import com.anilibrix.plus.core.database.dao.HistoryDao
import com.anilibrix.plus.core.database.dao.PlaylistDao
import com.anilibrix.plus.core.database.dao.PlaylistItemDao
import com.anilibrix.plus.core.database.dao.RatingDao
import com.anilibrix.plus.core.database.entity.CollectionEntity
import com.anilibrix.plus.core.database.entity.FavoriteEntity
import com.anilibrix.plus.core.database.entity.PlaylistEntity
import com.anilibrix.plus.core.database.entity.PlaylistItemEntity
import com.anilibrix.plus.core.database.entity.RatingEntity
import com.anilibrix.plus.data.local.mapper.toDomain
import com.anilibrix.plus.data.local.mapper.toEntity
import com.anilibrix.plus.data.local.mapper.toProgress
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.FavoriteTitle
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.Playlist
import com.anilibrix.plus.domain.model.PlaylistItem
import com.anilibrix.plus.domain.model.TitleProgress
import com.anilibrix.plus.domain.repository.LocalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao,
    private val playlistDao: PlaylistDao,
    private val playlistItemDao: PlaylistItemDao,
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

    override fun getContinueWatching(): Flow<List<HistoryEntry>> {
        return historyDao.getLatestPerTitle().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addHistory(entry: HistoryEntry) {
        historyDao.insert(entry.toEntity())
        // Просмотр — сам по себе сигнал «я это смотрю». Если тайтл ещё ни в
        // одном списке, он попадает в «Смотрю» автоматически: заставлять
        // человека отмечать это руками — лишний шаг, который он всё равно
        // пропустит, и библиотека останется пустой.
        promoteToWatching(entry)
    }

    override suspend fun deleteHistory(titleId: Long, episodeId: Long) {
        historyDao.delete(titleId, episodeId)
    }

    override suspend fun clearHistory() {
        historyDao.deleteAll()
    }

    override suspend fun getHistoryEntry(titleId: Long, episodeId: Long): HistoryEntry? {
        return historyDao.getEntry(titleId, episodeId)?.toDomain()
    }

    override suspend fun getTotalWatchTimeMs(): Long = historyDao.getTotalWatchTimeMs()

    override suspend fun getHistoryCount(): Int = historyDao.getCount()

    override fun getTitleProgress(titleId: Long, totalEpisodes: Int): Flow<TitleProgress> {
        return historyDao.getByTitleId(titleId).map { entities ->
            TitleProgress(
                titleId = titleId,
                episodes = entities.associate { it.episodeId to it.toProgress() },
                totalEpisodes = totalEpisodes,
            )
        }
    }

    override suspend fun getTitleProgressOnce(titleId: Long, totalEpisodes: Int): TitleProgress {
        val entities = historyDao.getByTitleIdOnce(titleId)
        return TitleProgress(
            titleId = titleId,
            episodes = entities.associate { it.episodeId to it.toProgress() },
            totalEpisodes = totalEpisodes,
        )
    }

    override suspend fun markEpisodeWatched(entry: HistoryEntry) {
        historyDao.insert(entry.copy(timestamp = entry.duration).toEntity())
        promoteToWatching(entry)
        syncCollectionProgress(entry.titleId)
    }

    override suspend fun markEpisodesWatchedUpTo(entries: List<HistoryEntry>) {
        if (entries.isEmpty()) return
        historyDao.insertAll(entries.map { it.copy(timestamp = it.duration).toEntity() })
        promoteToWatching(entries.last())
        syncCollectionProgress(entries.first().titleId)
    }

    override suspend fun clearAllUserData() {
        favoriteDao.deleteAll()
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
        return playlistDao.insert(PlaylistEntity(name = name))
    }

    override suspend fun renamePlaylist(playlistId: Long, name: String) {
        // Читаем существующую запись, а не собираем новую из аргументов:
        // иначе createdAt затирался бы текущим временем и порядок плейлистов
        // менялся бы от простого переименования.
        val existing = playlistDao.getById(playlistId) ?: return
        playlistDao.update(existing.copy(name = name))
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.getById(playlistId)?.let { playlistDao.delete(it) }
    }

    override suspend fun getPlaylistItems(playlistId: Long): List<PlaylistItem> {
        return playlistItemDao.getByPlaylistId(playlistId).map { it.toDomain() }
    }

    override suspend fun addPlaylistItem(playlistId: Long, titleId: Long, titleName: String) {
        playlistItemDao.insert(
            PlaylistItemEntity(playlistId = playlistId, titleId = titleId, titleName = titleName)
        )
    }

    override suspend fun removePlaylistItem(playlistId: Long, titleId: Long) {
        playlistItemDao.delete(playlistId, titleId)
    }

    // «Буду смотреть» больше не отдельная таблица — это один из статусов
    // коллекции. Раньше локальный `watch_later` и серверная коллекция
    // WATCH_LATER существовали параллельно и не пересекались: добавленное в
    // приложении не появлялось на сайте и наоборот.

    override fun getWatchLater(): Flow<List<FavoriteTitle>> =
        getCollections(CollectionType.WATCH_LATER)

    override suspend fun isInWatchLater(titleId: Long): Boolean =
        isInCollection(titleId, CollectionType.WATCH_LATER)

    override suspend fun addWatchLater(titleId: Long, titleName: String, posterUrl: String?) =
        setCollectionType(titleId, CollectionType.WATCH_LATER, titleName, posterUrl)

    override suspend fun removeWatchLater(titleId: Long) =
        collectionDao.delete(titleId, CollectionType.WATCH_LATER.value)

    override fun getCollections(collectionType: CollectionType): Flow<List<FavoriteTitle>> {
        return collectionDao.getByType(collectionType.value).map { list -> list.map { it.toDomain() } }
    }

    override fun getCollectionCounts(): Flow<Map<CollectionType, Int>> {
        return collectionDao.getCounts().map { rows ->
            rows.mapNotNull { row ->
                CollectionType.fromValue(row.collectionType)?.let { it to row.count }
            }.toMap()
        }
    }

    override suspend fun isInCollection(titleId: Long, collectionType: CollectionType): Boolean {
        return collectionDao.exists(titleId, collectionType.value)
    }

    override suspend fun getCollectionType(titleId: Long): CollectionType? {
        return collectionDao.getForTitle(titleId)?.let { CollectionType.fromValue(it.collectionType) }
    }

    override fun observeCollectionType(titleId: Long): Flow<CollectionType?> {
        return collectionDao.getForTitleFlow(titleId).map { entity ->
            entity?.let { CollectionType.fromValue(it.collectionType) }
        }
    }

    override suspend fun setCollectionType(
        titleId: Long,
        collectionType: CollectionType,
        titleName: String,
        posterUrl: String?,
    ) {
        // Имя и постер могут прийти пустыми (например, из уведомления, где
        // известен только id). Тогда сохраняем то, что уже лежит в базе, —
        // иначе карточка в библиотеке превратится в безымянный прямоугольник.
        val existing = collectionDao.getForTitle(titleId)
        val now = System.currentTimeMillis()
        collectionDao.setExclusiveStatus(
            CollectionEntity(
                titleId = titleId,
                collectionType = collectionType.value,
                titleName = titleName.ifBlank { existing?.titleName.orEmpty() },
                posterUrl = posterUrl ?: existing?.posterUrl,
                addedAt = existing?.addedAt ?: now,
                updatedAt = now,
                shikimoriId = existing?.shikimoriId,
                progressEpisode = existing?.progressEpisode ?: 0,
            )
        )
    }

    override suspend fun removeFromCollections(titleId: Long) {
        collectionDao.deleteAllForTitle(titleId)
    }

    override suspend fun getCollectionTypesForTitle(titleId: Long): List<CollectionType> {
        return collectionDao.getTypesForTitle(titleId).mapNotNull { CollectionType.fromValue(it) }
    }

    override suspend fun updateCollectionProgress(titleId: Long, episode: Int) {
        collectionDao.updateProgress(titleId, episode, System.currentTimeMillis())
    }

    override suspend fun getRating(titleId: Long): Float? {
        return ratingDao.getByTitleId(titleId)?.rating
    }

    override fun getAllRatings(): Flow<Map<Long, Float>> {
        return ratingDao.getAll().map { list -> list.associate { it.titleId to it.rating } }
    }

    override suspend fun setRating(titleId: Long, rating: Float) {
        ratingDao.insert(RatingEntity(titleId = titleId, rating = rating))
    }

    override suspend fun deleteRating(titleId: Long) {
        ratingDao.delete(titleId)
    }

    /**
     * Переводит тайтл в «Смотрю», если он ещё нигде не отмечен.
     *
     * Осознанно не трогает уже проставленный статус: «Брошено» не должно
     * само превращаться в «Смотрю» от того, что человек открыл серию, чтобы
     * вспомнить, почему бросил.
     */
    private suspend fun promoteToWatching(entry: HistoryEntry) {
        if (entry.titleId == 0L) return
        if (collectionDao.getForTitle(entry.titleId) != null) return
        setCollectionType(
            titleId = entry.titleId,
            collectionType = CollectionType.WATCHING,
            titleName = entry.titleName,
            posterUrl = entry.posterUrl,
        )
    }

    /** Подтягивает счётчик серий для внешнего трекера под фактический прогресс. */
    private suspend fun syncCollectionProgress(titleId: Long) {
        val progress = getTitleProgress(titleId, totalEpisodes = 0).first()
        collectionDao.updateProgress(titleId, progress.consecutiveWatched, System.currentTimeMillis())
    }
}
