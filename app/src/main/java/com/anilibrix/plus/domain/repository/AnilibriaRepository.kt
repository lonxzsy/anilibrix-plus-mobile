package com.anilibrix.plus.domain.repository

import com.anilibrix.plus.domain.model.CollectionItem
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.CatalogQuery
import com.anilibrix.plus.domain.model.FranchiseItem
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.ScheduleDay
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.domain.model.Torrent
import com.anilibrix.plus.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AnilibriaRepository {
    fun getCatalog(query: CatalogQuery): Flow<NetworkResult<List<Title>>>
    fun getCatalog(page: Int, limit: Int, search: String?): Flow<NetworkResult<List<Title>>> {
        return getCatalog(CatalogQuery(page = page, limit = limit, search = search))
    }
    fun getRelease(idOrAlias: String): Flow<NetworkResult<Title>>
    fun getSchedule(): Flow<NetworkResult<List<ScheduleDay>>>
    fun getFranchise(releaseId: Long): Flow<NetworkResult<List<FranchiseItem>>>
    fun getRecommended(limit: Int, releaseId: Long?): Flow<NetworkResult<List<Title>>>
    fun getTorrents(releaseId: Long): Flow<NetworkResult<List<Torrent>>>
    fun login(login: String, password: String): Flow<NetworkResult<String>>
    fun getProfile(): Flow<NetworkResult<User>>
    fun getFavoriteIds(): Flow<NetworkResult<List<Long>>>
    fun getFavoriteReleases(): Flow<NetworkResult<List<Title>>>
    fun addFavorite(releaseId: Long): Flow<NetworkResult<Unit>>
    fun removeFavorite(releaseId: Long): Flow<NetworkResult<Unit>>
    fun getCollectionIds(): Flow<NetworkResult<List<CollectionItem>>>
    fun getCollectionReleases(collectionType: CollectionType, page: Int = 1, limit: Int = 20): Flow<NetworkResult<List<Title>>>
    fun addToCollection(releaseId: Long, collectionType: CollectionType): Flow<NetworkResult<Unit>>
    fun removeFromCollection(releaseId: Long, collectionType: CollectionType): Flow<NetworkResult<Unit>>
    fun getHistory(): Flow<NetworkResult<List<HistoryEntry>>>
    fun getTimecodes(): Flow<NetworkResult<List<HistoryEntry>>>
    fun updateTimecode(releaseEpisodeId: String, timestamp: Long, duration: Long): Flow<NetworkResult<Unit>>
    fun deleteTimecode(releaseEpisodeId: String): Flow<NetworkResult<Unit>>
}
