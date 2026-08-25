package com.anilibrix.plus.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConsumetSearchResponse(
    val currentPage: Int = 1,
    val hasNextPage: Boolean = false,
    val results: List<ConsumetSearchResultDto> = emptyList()
)

@Serializable
data class ConsumetSearchResultDto(
    val id: String = "",
    val title: String = "",
    val url: String? = null,
    val image: String? = null,
    val releaseDate: String? = null,
    val subOrDub: String? = null
)

@Serializable
data class ConsumetAnimeInfo(
    val id: String = "",
    val title: String = "",
    val url: String? = null,
    val image: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val totalEpisodes: Int = 0,
    val episodes: List<ConsumetEpisodeDto> = emptyList()
)

@Serializable
data class ConsumetEpisodeDto(
    val id: String = "",
    val number: Int = 0,
    val title: String? = null,
    val url: String? = null
)

@Serializable
data class ConsumetWatchResponse(
    val headers: Map<String, String>? = null,
    val sources: List<ConsumetSourceDto> = emptyList(),
    val subtitles: List<ConsumetSubtitleDto> = emptyList(),
    val download: String? = null
)

@Serializable
data class ConsumetSourceDto(
    val url: String = "",
    val isM3U8: Boolean = true,
    val quality: String? = null
)

@Serializable
data class ConsumetSubtitleDto(
    val url: String = "",
    val lang: String = ""
)
