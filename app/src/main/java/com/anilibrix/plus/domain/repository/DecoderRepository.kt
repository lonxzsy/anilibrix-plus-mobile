package com.anilibrix.plus.domain.repository

import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.StudioEpisode
import com.anilibrix.plus.domain.model.StudioResult
import com.anilibrix.plus.domain.model.StudioVideo
import kotlinx.coroutines.flow.Flow

interface DecoderRepository {
    fun search(source: String, query: String): Flow<NetworkResult<List<StudioResult>>>
    fun getAnime(source: String, id: String): Flow<NetworkResult<List<StudioEpisode>>>
    fun getEpisodeVideos(source: String, id: String): Flow<NetworkResult<List<StudioVideo>>>
}
