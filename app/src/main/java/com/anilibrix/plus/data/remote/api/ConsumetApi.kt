package com.anilibrix.plus.data.remote.api

import com.anilibrix.plus.data.remote.dto.ConsumetAnimeInfo
import com.anilibrix.plus.data.remote.dto.ConsumetSearchResponse
import com.anilibrix.plus.data.remote.dto.ConsumetWatchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ConsumetApi {

    @GET("anime/gogoanime/{query}")
    suspend fun searchGogoanime(
        @Path("query") query: String,
        @Query("page") page: Int = 1
    ): ConsumetSearchResponse

    @GET("anime/gogoanime/info/{id}")
    suspend fun getGogoanimeInfo(
        @Path("id") id: String
    ): ConsumetAnimeInfo

    @GET("anime/gogoanime/watch/{episodeId}")
    suspend fun getGogoanimeWatch(
        @Path("episodeId") episodeId: String
    ): ConsumetWatchResponse
}
