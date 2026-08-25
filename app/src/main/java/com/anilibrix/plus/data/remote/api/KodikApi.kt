package com.anilibrix.plus.data.remote.api

import com.anilibrix.plus.data.remote.dto.KodikSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface KodikApi {

    @GET("search")
    suspend fun search(
        @Query("token") token: String,
        @Query("shikimori_id") shikimoriId: String? = null,
        @Query("mal_id") malId: String? = null,
        @Query("title") title: String? = null,
        @Query("with_episodes") withEpisodes: Boolean = true,
        @Query("with_material_data") withMaterialData: Boolean = true,
        @Query("limit") limit: Int = 100
    ): KodikSearchResponse

    @GET("list")
    suspend fun list(
        @Query("token") token: String,
        @Query("types") types: String = "anime,anime-serial",
        @Query("with_episodes") withEpisodes: Boolean = true,
        @Query("limit") limit: Int = 50
    ): KodikSearchResponse
}
