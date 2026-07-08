package com.anilibrix.plus.data.remote.api

import com.anilibrix.plus.data.remote.dto.GitHubCreateIssueRequest
import com.anilibrix.plus.data.remote.dto.GitHubIssueDto
import com.anilibrix.plus.data.remote.dto.GitHubReleaseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface GitHubApi {

    @GET("repos/lonxzsy/anilibrix-plus-web/releases")
    suspend fun getReleases(): List<GitHubReleaseDto>

    @GET("repos/lonxzsy/anilibrix-plus-web/issues")
    suspend fun getIssues(): List<GitHubIssueDto>

    @POST("repos/lonxzsy/anilibrix-plus-web/issues")
    suspend fun createIssue(@Body issue: GitHubCreateIssueRequest): GitHubIssueDto
}
