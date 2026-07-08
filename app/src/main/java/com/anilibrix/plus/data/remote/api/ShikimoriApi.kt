package com.anilibrix.plus.data.remote.api

import com.anilibrix.plus.data.remote.dto.ShikimoriAnimeDto
import com.anilibrix.plus.data.remote.dto.ShikimoriRelatedDto
import com.anilibrix.plus.data.remote.dto.ShikimoriRoleDto
import com.anilibrix.plus.data.remote.dto.ShikimoriScreenshotDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ShikimoriApi {

    @GET("api/animes")
    suspend fun searchAnime(
        @Query("search") query: String,
        @Query("limit") limit: Int = 5
    ): List<ShikimoriAnimeDto>

    @GET("api/animes/{id}")
    suspend fun getAnime(@Path("id") id: Int): ShikimoriAnimeDto

    @GET("api/animes/{id}/roles")
    suspend fun getCharacters(@Path("id") id: Int): List<ShikimoriRoleDto>

    @GET("api/animes/{id}/screenshots")
    suspend fun getScreenshots(@Path("id") id: Int): List<ShikimoriScreenshotDto>

    @GET("api/animes/{id}/related")
    suspend fun getRelated(@Path("id") id: Int): List<ShikimoriRelatedDto>
}
