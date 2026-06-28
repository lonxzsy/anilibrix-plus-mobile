package com.anilibrix.plus.data.repository

import com.anilibrix.plus.data.remote.api.JikanApi
import com.anilibrix.plus.domain.model.MalAnime
import com.anilibrix.plus.domain.model.MalCharacter
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.Seiyuu
import com.anilibrix.plus.domain.repository.JikanRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JikanRepositoryImpl @Inject constructor(
    private val api: JikanApi
) : JikanRepository {

    private val throttleMs = 500L

    override fun search(query: String, page: Int): Flow<NetworkResult<List<MalAnime>>> = flow {
        emit(NetworkResult.Loading)
        try {
            delay(throttleMs)
            val response = api.search(query, page)
            val list = response.data?.map { it.toDomain() } ?: emptyList()
            emit(NetworkResult.Success(list))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getTop(page: Int): Flow<NetworkResult<List<MalAnime>>> = flow {
        emit(NetworkResult.Loading)
        try {
            delay(throttleMs)
            val response = api.getTop(page)
            val list = response.data?.map { it.toDomain() } ?: emptyList()
            emit(NetworkResult.Success(list))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getDetail(malId: Long): Flow<NetworkResult<MalAnime>> = flow {
        emit(NetworkResult.Loading)
        try {
            delay(throttleMs)
            val response = api.getDetail(malId)
            val anime = response.data?.toDomain()
            if (anime != null) {
                emit(NetworkResult.Success(anime))
            } else {
                emit(NetworkResult.Error("Anime not found"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getCharacters(malId: Long): Flow<NetworkResult<List<MalCharacter>>> = flow {
        emit(NetworkResult.Loading)
        try {
            delay(throttleMs)
            val response = api.getCharacters(malId)
            val list = response.data?.map { it.toDomain() } ?: emptyList()
            emit(NetworkResult.Success(list))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getStatistics(malId: Long): Flow<NetworkResult<Map<Int, Int>>> = flow {
        emit(NetworkResult.Loading)
        try {
            delay(throttleMs)
            val response = api.getStatistics(malId)
            val scores = response.data?.scores?.mapKeys { it.key.toIntOrNull() ?: 0 }
                ?.mapValues { it.value.votes } ?: emptyMap()
            emit(NetworkResult.Success(scores))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getRecommendations(malId: Long): Flow<NetworkResult<List<MalAnime>>> = flow {
        emit(NetworkResult.Loading)
        try {
            delay(throttleMs)
            val response = api.getRecommendations(malId)
            val list = response.data?.mapNotNull { it.entry?.toDomain() } ?: emptyList()
            emit(NetworkResult.Success(list))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    private fun com.anilibrix.plus.data.remote.dto.JikanAnimeDto.toDomain(): MalAnime = MalAnime(
        malId = malId,
        title = title,
        score = score,
        rank = rank,
        popularity = popularity,
        imageUrl = images?.jpg?.imageUrl,
        type = type,
        synopsis = synopsis
    )

    private fun com.anilibrix.plus.data.remote.dto.JikanCharacterDto.toDomain(): MalCharacter = MalCharacter(
        malId = character?.malId ?: 0,
        name = character?.name.orEmpty(),
        role = role,
        imageUrl = character?.images?.jpg?.imageUrl,
        seiyuu = voiceActors?.firstOrNull()?.let { actor ->
            actor.person?.let { person ->
                Seiyuu(
                    malId = person.malId,
                    name = person.name,
                    role = actor.language,
                    imageUrl = person.images?.jpg?.imageUrl
                )
            }
        }
    )

    private fun com.anilibrix.plus.data.remote.dto.JikanRecommendationEntryDto.toDomain(): MalAnime = MalAnime(
        malId = malId,
        title = title,
        score = null,
        rank = null,
        popularity = null,
        imageUrl = images?.jpg?.imageUrl,
        type = null,
        synopsis = null
    )
}
