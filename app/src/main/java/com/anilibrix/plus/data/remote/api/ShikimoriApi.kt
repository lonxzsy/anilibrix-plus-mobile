package com.anilibrix.plus.data.remote.api

import com.anilibrix.plus.data.remote.dto.ShikimoriAnimeDto
import com.anilibrix.plus.data.remote.dto.ShikimoriPersonDto
import com.anilibrix.plus.data.remote.dto.ShikimoriRelatedDto
import com.anilibrix.plus.data.remote.dto.ShikimoriRoleDto
import com.anilibrix.plus.data.remote.dto.ShikimoriScreenshotDto
import com.anilibrix.plus.data.remote.dto.ShikimoriTokenDto
import com.anilibrix.plus.data.remote.dto.ShikimoriUserDto
import com.anilibrix.plus.data.remote.dto.UserRateDto
import com.anilibrix.plus.data.remote.dto.UserRateRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ShikimoriApi {

    @GET("api/animes")
    suspend fun searchAnime(
        @Query("search") query: String,
        @Query("limit") limit: Int = 5
    ): List<ShikimoriAnimeDto>

    /**
     * Поиск персонажей по имени.
     *
     * Отдельный эндпоинт: `api/animes/{id}/roles` возвращает персонажей
     * конкретного аниме, а здесь нужен поиск по всей базе.
     */
    @GET("api/characters/search")
    suspend fun searchCharacters(
        @Query("search") query: String,
        @Query("limit") limit: Int = 5,
    ): List<ShikimoriPersonDto>

    @GET("api/animes/{id}")
    suspend fun getAnime(@Path("id") id: Int): ShikimoriAnimeDto

    @GET("api/animes/{id}/roles")
    suspend fun getCharacters(@Path("id") id: Int): List<ShikimoriRoleDto>

    @GET("api/animes/{id}/screenshots")
    suspend fun getScreenshots(@Path("id") id: Int): List<ShikimoriScreenshotDto>

    @GET("api/animes/{id}/related")
    suspend fun getRelated(@Path("id") id: Int): List<ShikimoriRelatedDto>

    // --- OAuth -------------------------------------------------------------

    /**
     * Обмен кода авторизации на токен и обновление протухшего токена.
     *
     * Один метод на обе операции, потому что у Shikimori это один эндпоинт с
     * разным `grant_type`. Поля, не нужные конкретному типу, передаются `null`
     * и в тело не попадают.
     */
    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun exchangeToken(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String? = null,
        @Field("redirect_uri") redirectUri: String? = null,
        @Field("refresh_token") refreshToken: String? = null,
    ): ShikimoriTokenDto

    @GET("api/users/whoami")
    suspend fun whoami(): ShikimoriUserDto

    // --- Пользовательские списки -------------------------------------------

    @GET("api/v2/user_rates")
    suspend fun getUserRates(
        @Query("user_id") userId: Int,
        @Query("target_type") targetType: String = "Anime",
        @Query("limit") limit: Int = 1000,
        @Query("page") page: Int = 1,
    ): List<UserRateDto>

    @POST("api/v2/user_rates")
    suspend fun createRate(@Body body: UserRateRequest): UserRateDto

    @PATCH("api/v2/user_rates/{id}")
    suspend fun updateRate(@Path("id") id: Long, @Body body: UserRateRequest): UserRateDto

    @DELETE("api/v2/user_rates/{id}")
    suspend fun deleteRate(@Path("id") id: Long)
}
