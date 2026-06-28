package com.anilibrix.plus.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GitHubReleaseDto(
    val tagName: String = "",
    val name: String = "",
    val body: String? = null,
    val publishedAt: String = "",
    val htmlUrl: String = ""
)
