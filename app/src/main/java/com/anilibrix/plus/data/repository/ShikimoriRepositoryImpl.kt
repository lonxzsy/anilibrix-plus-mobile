package com.anilibrix.plus.data.repository

import com.anilibrix.plus.data.remote.api.ShikimoriApi
import com.anilibrix.plus.data.remote.dto.ShikimoriAnimeDto
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.ShikimoriAnime
import com.anilibrix.plus.domain.model.ShikimoriCharacter
import com.anilibrix.plus.domain.model.ShikimoriRelated
import com.anilibrix.plus.domain.model.ShikimoriSeiyu
import com.anilibrix.plus.domain.repository.ShikimoriRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShikimoriRepositoryImpl @Inject constructor(
    private val api: ShikimoriApi
) : ShikimoriRepository {

    private val throttleMs = 400L

    override fun search(query: String, limit: Int): Flow<NetworkResult<List<ShikimoriAnime>>> = flow {
        emit(NetworkResult.Loading)
        try {
            delay(throttleMs)
            val response = api.searchAnime(query, limit)
            emit(NetworkResult.Success(response.map { it.toDomain() }))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getAnime(id: Int): Flow<NetworkResult<ShikimoriAnime>> = flow {
        emit(NetworkResult.Loading)
        try {
            delay(throttleMs)
            val response = api.getAnime(id)
            emit(NetworkResult.Success(response.toDomain()))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getCharacters(id: Int): Flow<NetworkResult<List<ShikimoriCharacter>>> = flow {
        emit(NetworkResult.Loading)
        try {
            delay(throttleMs)
            val response = api.getCharacters(id)
            emit(NetworkResult.Success(response.mapNotNull { dto ->
                val character = dto.character ?: return@mapNotNull null
                val seiyu = dto.person
                ShikimoriCharacter(
                    id = character.id,
                    name = character.name,
                    russian = character.russian,
                    imageUrl = character.image?.original ?: character.image?.preview,
                    role = dto.rolesRussian.firstOrNull() ?: dto.roles.firstOrNull(),
                    seiyus = if (seiyu != null) {
                        listOf(
                            ShikimoriSeiyu(
                                id = seiyu.id,
                                name = seiyu.name,
                                russian = seiyu.russian,
                                imageUrl = seiyu.image?.original ?: seiyu.image?.preview
                            )
                        )
                    } else {
                        emptyList()
                    }
                )
            }))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getRelated(id: Int): Flow<NetworkResult<List<ShikimoriRelated>>> = flow {
        emit(NetworkResult.Loading)
        try {
            delay(throttleMs)
            val response = api.getRelated(id)
            emit(NetworkResult.Success(response.map { dto ->
                ShikimoriRelated(
                    id = dto.id,
                    name = dto.name,
                    russian = dto.russian,
                    imageUrl = dto.image?.original ?: dto.image?.preview,
                    relation = dto.relation
                )
            }))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    private fun ShikimoriAnimeDto.toDomain(): ShikimoriAnime = ShikimoriAnime(
        id = id,
        name = name,
        russian = russian,
        score = score?.toDoubleOrNull(),
        status = status,
        kind = kind,
        episodes = episodes,
        episodesAired = episodesAired,
        airedOn = airedOn,
        posterOriginal = poster?.original,
        posterPreview = poster?.preview,
        description = description,
        url = url
    )
}
