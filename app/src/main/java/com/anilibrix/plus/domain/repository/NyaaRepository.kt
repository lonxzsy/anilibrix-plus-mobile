package com.anilibrix.plus.domain.repository

import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.Torrent
import kotlinx.coroutines.flow.Flow

interface NyaaRepository {

    fun searchTorrents(query: String): Flow<NetworkResult<List<Torrent>>>
}
