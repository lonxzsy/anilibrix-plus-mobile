package com.anilibrix.plus.data.remote.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface NyaaApi {

    @GET("/")
    suspend fun searchRss(
        @Query("page") page: String = "rss",
        @Query("q") query: String,
        @Query("c") category: String = "1_2", // Anime - English-translated or Non-English
        @Query("f") filter: String = "0"
    ): ResponseBody
}
