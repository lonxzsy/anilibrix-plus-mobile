package com.anilibrix.plus.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanResponse<T>(
    val data: List<T>? = null
)

@Serializable
data class JikanSingleResponse<T>(
    val data: T? = null
)

@Serializable
data class JikanAnimeDto(
    @SerialName("mal_id") val malId: Long = 0,
    val title: String = "",
    val score: Double? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val images: JikanImagesDto? = null,
    val type: String? = null,
    val synopsis: String? = null
)

@Serializable
data class JikanImagesDto(
    val jpg: JikanImageDto? = null
)

@Serializable
data class JikanImageDto(
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class JikanCharacterDto(
    val character: JikanCharacterBriefDto? = null,
    val role: String? = null,
    @SerialName("voice_actors") val voiceActors: List<JikanVoiceActorDto>? = null
)

@Serializable
data class JikanCharacterBriefDto(
    @SerialName("mal_id") val malId: Long = 0,
    val name: String = "",
    val images: JikanImagesDto? = null
)

@Serializable
data class JikanVoiceActorDto(
    val person: JikanPersonDto? = null,
    val language: String? = null
)

@Serializable
data class JikanPersonDto(
    @SerialName("mal_id") val malId: Long = 0,
    val name: String = "",
    val images: JikanImagesDto? = null
)

@Serializable
data class JikanStatisticsDto(
    val data: JikanStatisticsDataDto? = null
)

@Serializable
data class JikanStatisticsDataDto(
    val scores: Map<String, JikanScoreDto>? = null
)

@Serializable
data class JikanScoreDto(
    val percentage: Double = 0.0,
    val votes: Int = 0
)

@Serializable
data class JikanRecommendationDto(
    val entry: List<JikanRecommendationEntryDto>? = null
)

@Serializable
data class JikanRecommendationEntryDto(
    @SerialName("mal_id") val malId: Long = 0,
    val title: String = "",
    val images: JikanImagesDto? = null
)
