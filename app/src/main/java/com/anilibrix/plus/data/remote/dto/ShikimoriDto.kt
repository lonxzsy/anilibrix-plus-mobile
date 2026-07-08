package com.anilibrix.plus.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShikimoriAnimeDto(
    val id: Int = 0,
    val name: String = "",
    @SerialName("russian") val russian: String? = null,
    val score: String? = null,
    val status: String? = null,
    val kind: String? = null,
    val episodes: Int = 0,
    @SerialName("episodesaired") val episodesAired: Int = 0,
    val airedOn: String? = null,
    val poster: ShikimoriImageDto? = null,
    val description: String? = null,
    val url: String? = null
)

@Serializable
data class ShikimoriImageDto(
    val original: String? = null,
    val preview: String? = null,
    val x96: String? = null,
    val x48: String? = null
)

@Serializable
data class ShikimoriCharacterDto(
    val id: Int = 0,
    val name: String = "",
    @SerialName("russian") val russian: String? = null,
    val image: ShikimoriImageDto? = null,
    val role: String? = null,
    val seiyus: List<ShikimoriSeiyuDto>? = null
)

@Serializable
data class ShikimoriRoleDto(
    val roles: List<String> = emptyList(),
    @SerialName("roles_russian") val rolesRussian: List<String> = emptyList(),
    val character: ShikimoriPersonDto? = null,
    val person: ShikimoriPersonDto? = null
)

@Serializable
data class ShikimoriPersonDto(
    val id: Int = 0,
    val name: String = "",
    @SerialName("russian") val russian: String? = null,
    val image: ShikimoriImageDto? = null
)

@Serializable
data class ShikimoriSeiyuDto(
    val id: Int = 0,
    val name: String = "",
    @SerialName("russian") val russian: String? = null,
    val image: ShikimoriImageDto? = null
)

@Serializable
data class ShikimoriScreenshotDto(
    val original: String? = null,
    val preview: String? = null
)

@Serializable
data class ShikimoriRelatedDto(
    val id: Int = 0,
    val name: String = "",
    @SerialName("russian") val russian: String? = null,
    val image: ShikimoriImageDto? = null,
    val relation: String? = null
)
