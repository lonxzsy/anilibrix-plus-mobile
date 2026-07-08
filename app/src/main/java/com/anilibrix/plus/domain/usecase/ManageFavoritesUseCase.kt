package com.anilibrix.plus.domain.usecase

import com.anilibrix.plus.domain.model.FavoriteTitle
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.domain.repository.LocalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class ManageFavoritesUseCase @Inject constructor(
    private val localRepository: LocalRepository,
    private val anilibriaRepository: AnilibriaRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getFavorites(): Flow<List<FavoriteTitle>> {
        return localRepository.getFavorites()
    }

    fun addFavorite(titleId: Long, titleName: String, posterUrl: String?) {
        scope.launch {
            localRepository.addFavorite(titleId, titleName, posterUrl)
            anilibriaRepository.addFavorite(titleId).collect {}
        }
    }

    fun removeFavorite(titleId: Long) {
        scope.launch {
            localRepository.removeFavorite(titleId)
            anilibriaRepository.removeFavorite(titleId).collect {}
        }
    }

    fun isFavorite(titleId: Long): Boolean {
        return runBlocking {
            localRepository.isFavorite(titleId)
        }
    }

    suspend fun isFavoriteSuspend(titleId: Long): Boolean {
        return localRepository.isFavorite(titleId)
    }

    suspend fun syncFromApi(token: String?) {
        if (token.isNullOrBlank()) return
        scope.launch {
            anilibriaRepository.getFavoriteReleases().collect { result ->
                if (result is com.anilibrix.plus.domain.model.NetworkResult.Success) {
                    result.data.forEach { title ->
                        localRepository.addFavorite(
                            title.id,
                            title.name.main,
                            title.poster?.medium ?: title.poster?.small
                        )
                    }
                }
            }
        }
    }
}
