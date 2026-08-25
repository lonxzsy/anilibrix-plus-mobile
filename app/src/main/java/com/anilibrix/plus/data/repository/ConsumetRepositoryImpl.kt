package com.anilibrix.plus.data.repository

import com.anilibrix.plus.data.remote.api.ConsumetApi
import com.anilibrix.plus.domain.model.Episode
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.StreamSourceInfo
import com.anilibrix.plus.domain.model.VoiceoverOption
import com.anilibrix.plus.domain.model.VoiceoverProvider
import com.anilibrix.plus.domain.model.VoiceoverType
import com.anilibrix.plus.domain.repository.ConsumetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsumetRepositoryImpl @Inject constructor(
    private val api: ConsumetApi
) : ConsumetRepository {

    override fun searchAndGetVoiceovers(query: String): Flow<NetworkResult<List<VoiceoverOption>>> = flow {
        emit(NetworkResult.Loading)
        try {
            android.util.Log.d("ConsumetRepo", "searchAndGetVoiceovers: query='$query'")
            val response = api.searchGogoanime(query)
            val options = response.results.map { item ->
                val isDub = item.subOrDub?.contains("dub", ignoreCase = true) == true || item.id.endsWith("-dub")
                VoiceoverOption(
                    id = "consumet_${item.id}",
                    name = if (isDub) "Gogoanime (EN Dub)" else "Gogoanime (EN Sub)",
                    provider = VoiceoverProvider.CONSUMET,
                    type = if (isDub) VoiceoverType.VOICE else VoiceoverType.SUBTITLES,
                    link = item.id
                )
            }
            android.util.Log.d("ConsumetRepo", "Parsed ${options.size} voiceovers from Consumet")
            emit(NetworkResult.Success(options))
        } catch (e: Exception) {
            android.util.Log.e("ConsumetRepo", "searchAndGetVoiceovers failed: ${e.message}", e)
            emit(NetworkResult.Error(e.message ?: "Ошибка поиска Consumet", e))
        }
    }

    override fun getEpisodes(animeId: String): Flow<NetworkResult<List<Episode>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val cleanId = animeId.removePrefix("consumet_")
            android.util.Log.d("ConsumetRepo", "getEpisodes: animeId='$animeId' (clean='$cleanId')")
            val info = api.getGogoanimeInfo(cleanId)
            val episodes = info.episodes.map { ep ->
                val epOrdinal = ep.number
                val uniqueId = 8_000_000_000L + (cleanId.hashCode().toLong().let { if (it < 0) -it else it } % 1_000_000L) * 1000L + epOrdinal
                Episode(
                    id = uniqueId,
                    releaseEpisodeId = ep.id,
                    name = ep.title ?: "Серия $epOrdinal",
                    ordinal = epOrdinal,
                    duration = 1440,
                    hls480 = null,
                    hls720 = null,
                    hls1080 = null,
                    opening = null,
                    ending = null
                )
            }
            android.util.Log.d("ConsumetRepo", "Parsed ${episodes.size} episodes from Consumet")
            emit(NetworkResult.Success(episodes))
        } catch (e: Exception) {
            android.util.Log.e("ConsumetRepo", "getEpisodes failed: ${e.message}", e)
            emit(NetworkResult.Error(e.message ?: "Ошибка получения серий Consumet", e))
        }
    }

    override suspend fun getStreamSource(episodeId: String): StreamSourceInfo? {
        return try {
            val cleanEpId = episodeId.removePrefix("consumet_")
            val watch = api.getGogoanimeWatch(cleanEpId)
            val bestSource = watch.sources.find { it.quality == "1080p" }
                ?: watch.sources.find { it.quality == "720p" }
                ?: watch.sources.find { it.quality == "default" }
                ?: watch.sources.firstOrNull()

            bestSource?.let { src ->
                StreamSourceInfo(
                    url = src.url,
                    quality = src.quality,
                    headers = watch.headers ?: emptyMap(),
                    subtitlesUrl = watch.subtitles.firstOrNull { it.lang.contains("English", ignoreCase = true) }?.url
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}
