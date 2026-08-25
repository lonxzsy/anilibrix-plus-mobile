package com.anilibrix.plus.domain.repository

import com.anilibrix.plus.domain.model.Episode
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.StreamSourceInfo
import com.anilibrix.plus.domain.model.VoiceoverOption
import kotlinx.coroutines.flow.Flow

interface ConsumetRepository {

    fun searchAndGetVoiceovers(query: String): Flow<NetworkResult<List<VoiceoverOption>>>

    fun getEpisodes(animeId: String): Flow<NetworkResult<List<Episode>>>

    suspend fun getStreamSource(episodeId: String): StreamSourceInfo?
}
