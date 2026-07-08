package com.anilibrix.plus.data.remote.api

import com.anilibrix.plus.data.remote.dto.JikanAnimeDto
import com.anilibrix.plus.data.remote.dto.JikanCharacterDto
import com.anilibrix.plus.data.remote.dto.JikanRecommendationDto
import com.anilibrix.plus.data.remote.dto.JikanResponse
import com.anilibrix.plus.data.remote.dto.JikanSingleResponse
import com.anilibrix.plus.data.remote.dto.JikanStatisticsDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JikanApi {

    @GET("anime")
    suspend fun search(@Query("q") query: String, @Query("page") page: Int = 1): JikanResponse<JikanAnimeDto>

    @GET("top/anime")
    suspend fun getTop(@Query("page") page: Int = 1): JikanResponse<JikanAnimeDto>

    @GET("anime/{malId}/full")
    suspend fun getDetail(@Path("malId") malId: Long): JikanSingleResponse<JikanAnimeDto>

    @GET("anime/{malId}/characters")
    suspend fun getCharacters(@Path("malId") malId: Long): JikanResponse<JikanCharacterDto>

    @GET("anime/{malId}/statistics")
    suspend fun getStatistics(@Path("malId") malId: Long): JikanStatisticsDto

    @GET("anime/{malId}/recommendations")
    suspend fun getRecommendations(@Path("malId") malId: Long): JikanResponse<JikanRecommendationDto>
}
