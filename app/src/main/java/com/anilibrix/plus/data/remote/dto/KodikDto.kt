package com.anilibrix.plus.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KodikSearchResponse(
    val time: String? = null,
    val total: Int = 0,
    val results: List<KodikMaterialDto> = emptyList()
)

@Serializable
data class KodikMaterialDto(
    val id: String? = null,
    val type: String? = null,
    val link: String? = null,
    val title: String? = null,
    @SerialName("title_orig") val titleOrig: String? = null,
    @SerialName("other_title") val otherTitle: String? = null,
    val year: Int? = null,
    @SerialName("last_season") val lastSeason: Int? = null,
    @SerialName("last_episode") val lastEpisode: Int? = null,
    @SerialName("episodes_count") val episodesCount: Int? = null,
    @SerialName("shikimori_id") val shikimoriId: String? = null,
    val quality: String? = null,
    val translation: KodikTranslationDto? = null,
    val seasons: Map<String, KodikSeasonDto>? = null,
    val screenshots: List<String>? = null
)

@Serializable
data class KodikTranslationDto(
    val id: Long = 0L,
    val title: String = "",
    val type: String = "voice" // "voice" | "subtitles"
)

@Serializable
data class KodikSeasonDto(
    val link: String? = null,
    val episodes: Map<String, String>? = null // "1" -> "//kodik.cc/video/..."
)
