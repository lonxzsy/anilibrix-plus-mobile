package com.anilibrix.plus.domain.usecase

import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.FavoriteTitle
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.domain.repository.LocalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

class ManageCollectionsUseCase @Inject constructor(
    private val localRepository: LocalRepository,
    private val anilibriaRepository: AnilibriaRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getCollections(collectionType: CollectionType): Flow<List<FavoriteTitle>> {
        return localRepository.getCollections(collectionType)
    }

    fun addToCollection(titleId: Long, collectionType: CollectionType, titleName: String, posterUrl: String?) {
        scope.launch {
            localRepository.addToCollection(titleId, collectionType, titleName, posterUrl)
            anilibriaRepository.addToCollection(titleId, collectionType).collect {}
        }
    }

    fun removeFromCollection(titleId: Long, collectionType: CollectionType) {
        scope.launch {
            localRepository.removeFromCollection(titleId, collectionType)
            anilibriaRepository.removeFromCollection(titleId, collectionType).collect {}
        }
    }

    suspend fun isInCollection(titleId: Long, collectionType: CollectionType): Boolean {
        return localRepository.isInCollection(titleId, collectionType)
    }

    suspend fun getCollectionTypesForTitle(titleId: Long): List<CollectionType> {
        return localRepository.getCollectionTypesForTitle(titleId)
    }

    suspend fun syncFromApi(token: String?) {
        if (token.isNullOrBlank()) return
        scope.launch {
            anilibriaRepository.getCollectionIds().collect { result ->
                if (result is NetworkResult.Success) {
                    val collectionsByType = result.data.groupBy { it.collectionType }
                    
                    collectionsByType.forEach { (collectionType, items) ->
                        launch {
                            anilibriaRepository.getCollectionReleases(collectionType).collect { releasesResult ->
                                if (releasesResult is NetworkResult.Success) {
                                    releasesResult.data.forEach { title ->
                                        localRepository.addToCollection(
                                            title.id,
                                            collectionType,
                                            title.name.main,
                                            title.poster?.medium ?: title.poster?.small
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
