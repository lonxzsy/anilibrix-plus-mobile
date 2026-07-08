package com.anilibrix.plus.data.remote.api

import com.anilibrix.plus.data.remote.dto.CatalogResponse
import com.anilibrix.plus.data.remote.dto.CollectionActionRequest
import com.anilibrix.plus.data.remote.dto.CollectionIdsResponse
import com.anilibrix.plus.data.remote.dto.DeleteTimecodeRequest
import com.anilibrix.plus.data.remote.dto.EpisodeDto
import com.anilibrix.plus.data.remote.dto.FavoriteActionRequest
import com.anilibrix.plus.data.remote.dto.FavoriteIdsResponse
import com.anilibrix.plus.data.remote.dto.FranchiseNodeDto
import com.anilibrix.plus.data.remote.dto.LoginRequest
import com.anilibrix.plus.data.remote.dto.LoginResponse
import com.anilibrix.plus.data.remote.dto.ProfileResponse
import com.anilibrix.plus.data.remote.dto.ReleaseDto
import com.anilibrix.plus.data.remote.dto.ScheduleItemDto
import com.anilibrix.plus.data.remote.dto.TimecodeDto
import com.anilibrix.plus.data.remote.dto.TimecodeRequest
import com.anilibrix.plus.data.remote.dto.TorrentDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AnilibriaApi {

    @GET("anime/catalog/releases")
    suspend fun getCatalog(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @retrofit2.http.QueryMap(encoded = true) filters: Map<String, String> = emptyMap()
    ): CatalogResponse

    @GET("anime/releases/{idOrAlias}")
    suspend fun getRelease(@Path("idOrAlias") idOrAlias: String): ReleaseDto

    @GET("anime/schedule/week")
    suspend fun getSchedule(): List<ScheduleItemDto>

    @GET("anime/franchises/release/{releaseId}")
    suspend fun getFranchise(@Path("releaseId") releaseId: Long): List<FranchiseNodeDto>

    @GET("anime/releases/recommended")
    suspend fun getRecommended(
        @Query("limit") limit: Int = 10,
        @Query("release_id") releaseId: Long? = null
    ): List<ReleaseDto>

    @GET("anime/torrents/release/{releaseId}")
    suspend fun getTorrents(@Path("releaseId") releaseId: Long): List<TorrentDto>

    @POST("accounts/users/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("accounts/users/me/profile")
    suspend fun getProfile(): ProfileResponse

    @GET("accounts/users/me/favorites/ids")
    suspend fun getFavoriteIds(): FavoriteIdsResponse

    @GET("accounts/users/me/favorites/releases")
    suspend fun getFavoriteReleases(): List<ReleaseDto>

    @POST("accounts/users/me/favorites")
    suspend fun addFavorite(@Body request: List<FavoriteActionRequest>)

    @DELETE("accounts/users/me/favorites")
    suspend fun removeFavorite(@Body request: List<FavoriteActionRequest>)

    @GET("accounts/users/me/collections/ids")
    suspend fun getCollectionIds(): CollectionIdsResponse

    @GET("accounts/users/me/collections/releases")
    suspend fun getCollectionReleases(
        @Query("type_of_collection") typeOfCollection: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): CatalogResponse

    @POST("accounts/users/me/collections")
    suspend fun addToCollection(@Body request: List<CollectionActionRequest>)

    @DELETE("accounts/users/me/collections")
    suspend fun removeFromCollection(@Body request: List<CollectionActionRequest>)

    @GET("accounts/users/me/views/history")
    suspend fun getHistory(): List<ReleaseDto>

    @GET("accounts/users/me/views/timecodes")
    suspend fun getTimecodes(): List<TimecodeDto>

    @GET("anime/releases/episodes/{releaseEpisodeId}")
    suspend fun getEpisode(@Path("releaseEpisodeId") releaseEpisodeId: String): EpisodeDto

    @POST("accounts/users/me/views/timecodes")
    suspend fun updateTimecode(@Body request: List<TimecodeRequest>)

    @HTTP(method = "DELETE", path = "accounts/users/me/views/timecodes", hasBody = true)
    suspend fun deleteTimecode(@Body request: List<DeleteTimecodeRequest>)
}
