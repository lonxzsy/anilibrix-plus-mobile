package com.anilibrix.plus.data.remote.api

import com.anilibrix.plus.data.remote.dto.AniSkipResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AniSkipApi {

    @GET("v2/skip-times/{malId}/{episodeNumber}")
    suspend fun getSkipTimes(
        @Path("malId") malId: Long,
        @Path("episodeNumber") episodeNumber: Int,
        @Query("types") types: List<String> = listOf("op", "ed"),
        @Query("episodeLength") episodeLength: Double = 0.0
    ): AniSkipResponseDto
}
