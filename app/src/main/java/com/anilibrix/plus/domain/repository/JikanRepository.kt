package com.anilibrix.plus.domain.repository

import com.anilibrix.plus.domain.model.MalAnime
import com.anilibrix.plus.domain.model.MalCharacter
import com.anilibrix.plus.domain.model.NetworkResult
import kotlinx.coroutines.flow.Flow

interface JikanRepository {
    fun search(query: String, page: Int = 1): Flow<NetworkResult<List<MalAnime>>>
    fun getTop(page: Int = 1): Flow<NetworkResult<List<MalAnime>>>
    fun getDetail(malId: Long): Flow<NetworkResult<MalAnime>>
    fun getCharacters(malId: Long): Flow<NetworkResult<List<MalCharacter>>>
    fun getStatistics(malId: Long): Flow<NetworkResult<Map<Int, Int>>>
    fun getRecommendations(malId: Long): Flow<NetworkResult<List<MalAnime>>>
}
