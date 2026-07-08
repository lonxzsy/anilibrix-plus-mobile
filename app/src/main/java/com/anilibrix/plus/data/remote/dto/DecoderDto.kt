package com.anilibrix.plus.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SearchRequest(
    val source: String,
    val query: String
)

@Serializable
data class SearchResponse(
    val results: List<SearchResultDto> = emptyList()
)

@Serializable
data class SearchResultDto(
    val source: String = "",
    val id: String = "",
    val title: String = "",
    val url: String? = null,
    val image: String? = null
)

@Serializable
data class AnimeRequest(
    val source: String,
    val id: String
)

@Serializable
data class AnimeResponse(
    val episodes: List<AnimeEpisodeDto> = emptyList()
)

@Serializable
data class AnimeEpisodeDto(
    val id: String = "",
    val number: Int = 0,
    val title: String? = null
)

@Serializable
data class EpisodeVideosRequest(
    val source: String,
    val id: String
)

@Serializable
data class EpisodeVideosResponse(
    val videos: List<EpisodeVideoDto> = emptyList()
)

@Serializable
data class EpisodeVideoDto(
    val url: String = "",
    val quality: String? = null
)
