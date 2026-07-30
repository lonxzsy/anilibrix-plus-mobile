package com.anilibrix.plus.domain.repository

import com.anilibrix.plus.domain.model.MalAnime
import com.anilibrix.plus.domain.model.MalCharacter
import com.anilibrix.plus.domain.model.MalCharacterDetail
import com.anilibrix.plus.domain.model.NetworkResult
import kotlinx.coroutines.flow.Flow

interface JikanRepository {
    fun search(query: String, page: Int = 1): Flow<NetworkResult<List<MalAnime>>>
    fun getTop(page: Int = 1): Flow<NetworkResult<List<MalAnime>>>
    fun getDetail(malId: Long): Flow<NetworkResult<MalAnime>>
    fun getCharacters(malId: Long): Flow<NetworkResult<List<MalCharacter>>>

    /**
     * Карточка одного персонажа по его собственному id.
     *
     * Экран персонажа раньше вызывал [getCharacters], передавая туда id
     * персонажа вместо id аниме, — запрос уходил не по тому адресу.
     */
    fun getCharacter(characterId: Long): Flow<NetworkResult<MalCharacterDetail>>
    fun getStatistics(malId: Long): Flow<NetworkResult<Map<Int, Int>>>
    fun getRecommendations(malId: Long): Flow<NetworkResult<List<MalAnime>>>
}
