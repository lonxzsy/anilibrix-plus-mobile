package com.anilibrix.plus.domain.repository

import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.ShikimoriAnime
import com.anilibrix.plus.domain.model.ShikimoriCharacter
import com.anilibrix.plus.domain.model.ShikimoriRelated
import kotlinx.coroutines.flow.Flow

interface ShikimoriRepository {
    fun search(query: String, limit: Int = 5): Flow<NetworkResult<List<ShikimoriAnime>>>
    fun getAnime(id: Int): Flow<NetworkResult<ShikimoriAnime>>
    fun getCharacters(id: Int): Flow<NetworkResult<List<ShikimoriCharacter>>>
    fun getRelated(id: Int): Flow<NetworkResult<List<ShikimoriRelated>>>
}
