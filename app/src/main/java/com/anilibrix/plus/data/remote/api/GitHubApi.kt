package com.anilibrix.plus.data.remote.api

import com.anilibrix.plus.data.remote.dto.GitHubReleaseDto
import retrofit2.http.GET

interface GitHubApi {

    @GET("repos/lonxzsy/anilibrix-plus-web/releases")
    suspend fun getReleases(): List<GitHubReleaseDto>
}
