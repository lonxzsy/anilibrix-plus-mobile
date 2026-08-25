package com.anilibrix.plus.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AniSkipResponseDto(
    val found: Boolean = false,
    val results: List<AniSkipResultDto> = emptyList(),
    val message: String? = null,
    val statusCode: Int = 200
)

@Serializable
data class AniSkipResultDto(
    val interval: AniSkipIntervalDto,
    val skipType: String = "", // "op" | "ed" | "mixed-op" | "mixed-ed" | "recap"
    val skipId: String? = null,
    val episodeLength: Double? = null
)

@Serializable
data class AniSkipIntervalDto(
    val startTime: Double = 0.0,
    val endTime: Double = 0.0
)
