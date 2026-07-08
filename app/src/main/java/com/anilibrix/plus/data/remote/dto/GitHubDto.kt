package com.anilibrix.plus.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String? = null,
    @SerialName("published_at") val publishedAt: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val author: GitHubAuthorDto? = null,
    val assets: List<GitHubAssetDto> = emptyList(),
    val draft: Boolean = false,
    val prerelease: Boolean = false
)

@Serializable
data class GitHubAuthorDto(
    val login: String = "",
    @SerialName("avatar_url") val avatarUrl: String = ""
)

@Serializable
data class GitHubAssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    val size: Long = 0
)

@Serializable
data class GitHubIssueDto(
    val number: Int = 0,
    val title: String = "",
    val body: String? = null,
    val state: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val user: GitHubAuthorDto? = null
)

@Serializable
data class GitHubCreateIssueRequest(
    val title: String,
    val body: String? = null
)
