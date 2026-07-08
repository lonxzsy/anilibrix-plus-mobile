package com.anilibrix.plus.data.repository

import com.anilibrix.plus.data.remote.api.DecoderApi
import com.anilibrix.plus.data.remote.dto.AnimeRequest
import com.anilibrix.plus.data.remote.dto.EpisodeVideosRequest
import com.anilibrix.plus.data.remote.dto.SearchRequest
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.StudioEpisode
import com.anilibrix.plus.domain.model.StudioResult
import com.anilibrix.plus.domain.model.StudioVideo
import com.anilibrix.plus.domain.repository.DecoderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DecoderRepositoryImpl @Inject constructor(
    private val api: DecoderApi
) : DecoderRepository {

    override fun search(source: String, query: String): Flow<NetworkResult<List<StudioResult>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.search(SearchRequest(source, query))
            emit(NetworkResult.Success(response.results.map { result ->
                StudioResult(
                    source = result.source,
                    id = result.id,
                    title = result.title,
                    url = result.url,
                    image = result.image
                )
            }))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getAnime(source: String, id: String): Flow<NetworkResult<List<StudioEpisode>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.getAnime(AnimeRequest(source, id))
            emit(NetworkResult.Success(response.episodes.map { episode ->
                StudioEpisode(
                    id = episode.id,
                    number = episode.number,
                    title = episode.title
                )
            }))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getEpisodeVideos(source: String, id: String): Flow<NetworkResult<List<StudioVideo>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.getEpisodeVideos(EpisodeVideosRequest(source, id))
            emit(NetworkResult.Success(response.videos.map { video ->
                StudioVideo(
                    url = video.url,
                    quality = video.quality
                )
            }))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }
}
