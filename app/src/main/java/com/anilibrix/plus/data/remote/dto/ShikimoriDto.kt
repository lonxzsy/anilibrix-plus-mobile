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

// --- OAuth и пользовательские списки ---------------------------------------

@Serializable
data class ShikimoriTokenDto(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("token_type") val tokenType: String = "Bearer",
    /** Секунды жизни токена с момента выдачи ([createdAt]). */
    @SerialName("expires_in") val expiresIn: Long = 0L,
    @SerialName("created_at") val createdAt: Long = 0L,
) {
    /** Абсолютный момент истечения в миллисекундах. */
    fun expiresAtMillis(): Long = (createdAt + expiresIn) * 1000L
}

@Serializable
data class ShikimoriUserDto(
    val id: Int = 0,
    val nickname: String = "",
    val avatar: String? = null,
    val image: ShikimoriImageDto? = null,
    val url: String? = null,
)

/**
 * Запись пользовательского списка.
 *
 * `status` — одно из `planned` / `watching` / `completed` / `on_hold` /
 * `dropped` / `rewatching`. `score` — 0..10, где 0 означает «оценки нет».
 */
@Serializable
data class UserRateDto(
    val id: Long = 0L,
    @SerialName("user_id") val userId: Int = 0,
    @SerialName("target_id") val targetId: Int = 0,
    @SerialName("target_type") val targetType: String = "Anime",
    val score: Int = 0,
    val status: String = "",
    val episodes: Int = 0,
    val rewatches: Int = 0,
    val text: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class UserRateRequest(
    @SerialName("user_rate") val userRate: UserRateBody,
)

@Serializable
data class UserRateBody(
    @SerialName("user_id") val userId: Int? = null,
    @SerialName("target_id") val targetId: Int? = null,
    @SerialName("target_type") val targetType: String? = null,
    val status: String? = null,
    val score: Int? = null,
    val episodes: Int? = null,
)
