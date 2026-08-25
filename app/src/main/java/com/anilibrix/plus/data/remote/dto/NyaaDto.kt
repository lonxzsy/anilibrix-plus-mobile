package com.anilibrix.plus.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NyaaTorrentDto(
    val id: String = "",
    val name: String = "",
    val category: String? = null,
    val size: String? = null,
    val date: String? = null,
    val seeders: Int = 0,
    val leechers: Int = 0,
    val completed: Int = 0,
    val comments: Int = 0,
    val link: String? = null,
    val magnet: String = ""
)

@Serializable
data class NyaaSearchResponse(
    val data: List<NyaaTorrentDto> = emptyList()
)
