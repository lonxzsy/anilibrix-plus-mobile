package com.anilibrix.plus.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class CatalogResponse(
    val data: List<ReleaseDto> = emptyList(),
    val meta: MetaDto? = null
)

@Serializable
data class MetaDto(
    val pagination: PaginationDto? = null
)

@Serializable
data class PaginationDto(
    @SerialName("current_page") val page: Int = 1,
    @SerialName("total_pages") val pages: Int = 1,
    val total: Int = 0
)

@Serializable
data class ReleaseDto(
    val id: Int = 0,
    val alias: String = "",
    val name: NamesDto? = null,
    val description: String? = null,
    val poster: PosterDto? = null,
    val genres: List<GenreDto>? = null,
    val type: TypeDto? = null,
    val season: SeasonDto? = null,
    val year: Int? = null,
    @SerialName("episodes_total") val episodesTotal: Int? = null,
    @SerialName("is_ongoing") val isOngoing: Boolean? = null,
    @SerialName("publish_day") val publishDay: PublishDayDto? = null,
    val score: Double? = null,
    val episodes: List<EpisodeDto>? = null,
    val torrents: List<TorrentDto>? = null,
    val inFavorites: Boolean? = null,
    @SerialName("mal_id") val malId: Int? = null
)

@Serializable
data class NamesDto(
    val main: String? = null,
    val english: String? = null,
    val alternative: String? = null
)

@Serializable
data class PosterDto(
    val src: String? = null,
    val preview: String? = null,
    val thumbnail: String? = null,
    val optimized: PosterOptimizedDto? = null
)

@Serializable
data class PosterOptimizedDto(
    val src: String? = null,
    val preview: String? = null,
    val thumbnail: String? = null
)

@Serializable
data class GenreDto(
    val id: Int = 0,
    val name: String? = null
)

@Serializable
data class TypeDto(
    val value: String? = null,
    val description: String? = null
)

@Serializable
data class SeasonDto(
    val value: String? = null,
    val description: String? = null
)

@Serializable
data class EpisodeDto(
    val id: String? = null, // Using String since id might be UUID in schedule API
    val name: String? = null,
    val ordinal: Double? = null,
    val duration: Double? = null,
    @SerialName("hls_480") val hls480: String? = null,
    @SerialName("hls_720") val hls720: String? = null,
    @SerialName("hls_1080") val hls1080: String? = null,
    val opening: SkipRangeDto? = null,
    val ending: SkipRangeDto? = null,
    @SerialName("release_id") val releaseId: Long? = null,
    val release: ReleaseDto? = null
)

@Serializable
data class SkipRangeDto(
    val start: Double? = null,
    val stop: Double? = null
)

@Serializable
data class TorrentDto(
    val id: Int = 0,
    val quality: TorrentQualityDto? = null,
    val series: String? = null,
    val size: Long? = null,
    val magnet: String? = null,
    val seeders: Int? = null,
    val leechers: Int? = null
)

@Serializable
data class TorrentQualityDto(
    val value: String? = null,
    val description: String? = null
)

@Serializable
data class LoginRequest(
    val login: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String? = null,
    @SerialName("session_token") val sessionToken: String? = null,
    @SerialName("access_token") val accessToken: String? = null
) {
    val effectiveToken: String?
        get() = token ?: sessionToken ?: accessToken
}

@Serializable
data class ProfileResponse(
    val id: Int = 0,
    val login: String? = null,
    val avatar: AvatarDto? = null,
    val nickname: String? = null,
    val email: String? = null,
    @SerialName("is_banned") val isBanned: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    val torrents: TorrentStatsDto? = null
) {
    val avatarUrl: String?
        get() = avatar?.let {
            it.preview ?: it.thumbnail ?: it.optimized?.preview ?: it.optimized?.thumbnail
        }?.takeIf { it.isNotEmpty() }
}

@Serializable
data class AvatarDto(
    val preview: String? = null,
    val thumbnail: String? = null,
    val optimized: AvatarOptimizedDto? = null
)

@Serializable
data class AvatarOptimizedDto(
    val preview: String? = null,
    val thumbnail: String? = null
)

@Serializable
data class TorrentStatsDto(
    val passkey: String? = null,
    val uploaded: Long = 0,
    val downloaded: Long = 0
)

@Serializable
data class FavoriteIdsResponse(
    val ids: List<Int>? = null
)

@Serializable
data class ScheduleItemDto(
    val release: ReleaseDto? = null,
    @SerialName("published_release_episode") val publishedReleaseEpisode: EpisodeDto? = null,
    @SerialName("next_release_episode_number") val nextReleaseEpisodeNumber: Int? = null
)

@Serializable
data class FranchiseNodeDto(
    val id: String? = null,
    val name: String? = null,
    @SerialName("franchise_releases") val franchiseReleases: List<FranchiseReleaseDto> = emptyList()
)

@Serializable
data class FranchiseReleaseDto(
    val id: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
    @SerialName("release_id") val releaseId: Long? = null,
    val release: ReleaseDto? = null
)

@Serializable
data class FavoriteActionRequest(
    @SerialName("release_id") val releaseId: Int
)

@Serializable
data class CollectionActionRequest(
    @SerialName("release_id") val releaseId: Int,
    @SerialName("type_of_collection") val typeOfCollection: String
)

@Serializable
data class CollectionIdsResponse(
    val ids: List<CollectionIdItem>? = null
)

@Serializable
data class CollectionIdItem(
    @SerialName("release_id") val releaseId: Int = 0,
    @SerialName("type_of_collection") val typeOfCollection: String? = null
)

@Serializable(with = TimecodeDtoSerializer::class)
data class TimecodeDto(
    @SerialName("release_episode_id") val releaseEpisodeId: String,
    val time: Double,
    @SerialName("is_watched") val isWatched: Boolean
)

@Serializable
data class TimecodeRequest(
    val time: Double,
    @SerialName("is_watched") val isWatched: Boolean,
    @SerialName("release_episode_id") val releaseEpisodeId: String
)

@Serializable
data class DeleteTimecodeRequest(
    @SerialName("release_episode_id") val releaseEpisodeId: String
)

object TimecodeDtoSerializer : KSerializer<TimecodeDto> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "TimecodeDto",
        PrimitiveKind.STRING
    )

    override fun deserialize(decoder: Decoder): TimecodeDto {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("TimecodeDto can only be decoded from JSON")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> TimecodeDto(
                releaseEpisodeId = element.getOrNull(0)?.jsonPrimitive?.content.orEmpty(),
                time = element.getOrNull(1)?.jsonPrimitive?.doubleOrNull ?: 0.0,
                isWatched = element.getOrNull(2)?.jsonPrimitive?.booleanOrNull ?: false
            )
            is JsonObject -> TimecodeDto(
                releaseEpisodeId = element["release_episode_id"]?.jsonPrimitive?.content.orEmpty(),
                time = element["time"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                isWatched = element["is_watched"]?.jsonPrimitive?.booleanOrNull ?: false
            )
            else -> TimecodeDto(
                releaseEpisodeId = "",
                time = 0.0,
                isWatched = false
            )
        }
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: TimecodeDto) {
        error("TimecodeDto is a read-only API response shape")
    }
}

@Serializable
data class PublishDayDto(
    val value: Int = 0,
    val description: String? = null
)
