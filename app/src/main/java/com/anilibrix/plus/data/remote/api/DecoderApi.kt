package com.anilibrix.plus.data.remote.api

import com.anilibrix.plus.data.remote.dto.AnimeRequest
import com.anilibrix.plus.data.remote.dto.AnimeResponse
import com.anilibrix.plus.data.remote.dto.EpisodeVideosRequest
import com.anilibrix.plus.data.remote.dto.EpisodeVideosResponse
import com.anilibrix.plus.data.remote.dto.SearchRequest
import com.anilibrix.plus.data.remote.dto.SearchResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface DecoderApi {

    @POST("api/search")
    suspend fun search(@Body request: SearchRequest): SearchResponse

    @POST("api/anime")
    suspend fun getAnime(@Body request: AnimeRequest): AnimeResponse

    @POST("api/episode/videos")
    suspend fun getEpisodeVideos(@Body request: EpisodeVideosRequest): EpisodeVideosResponse
}
