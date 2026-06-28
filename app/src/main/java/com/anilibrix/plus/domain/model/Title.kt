package com.anilibrix.plus.domain.model

data class Title(
    val id: Long,
    val alias: String,
    val name: TitleName,
    val description: String?,
    val poster: Poster?,
    val genres: List<Genre>,
    val type: ReleaseType?,
    val season: Season?,
    val year: Int,
    val episodesTotal: Int,
    val isOngoing: Boolean,
    val score: Double?,
    val episodes: List<Episode>?,
    val torrents: List<Torrent>?,
    val isExternal: Boolean,
    val malId: Int?
)

data class TitleName(
    val main: String,
    val english: String?,
    val alternative: String?
)

data class Poster(
    val small: String?,
    val medium: String?,
    val original: String?
)

data class Genre(
    val id: Long,
    val name: String
)

enum class ReleaseType(val displayName: String) {
    TV("TV"),
    OVA("OVA"),
    ONA("ONA"),
    MOVIE("Movie"),
    SPECIAL("Special"),
    UNKNOWN("Unknown")
}

data class Season(
    val name: SeasonName,
    val year: Int
)

enum class SeasonName {
    WINTER, SPRING, SUMMER, FALL, UNKNOWN
}

data class Episode(
    val id: Long,
    val releaseEpisodeId: String,
    val name: String,
    val ordinal: Int,
    val duration: Int,
    val hls480: String?,
    val hls720: String?,
    val hls1080: String?,
    val opening: SkipRange?,
    val ending: SkipRange?
)

data class SkipRange(
    val start: Double,
    val stop: Double
)

data class Torrent(
    val id: Long,
    val quality: String?,
    val series: String?,
    val size: Long?,
    val magnet: String?,
    val seeders: Int?,
    val leechers: Int?
)

data class User(
    val id: Long,
    val login: String,
    val nickname: String?,
    val email: String?,
    val avatarUrl: String?,
    val profileUrl: String?,
    val isBanned: Boolean,
    val createdAt: String?,
    val torrentStats: TorrentStats?
)

data class TorrentStats(
    val passkey: String?,
    val uploaded: Long,
    val downloaded: Long
)

data class ScheduleDay(
    val day: String,
    val releases: List<Title>
)

data class FranchiseItem(
    val id: Long,
    val name: String,
    val poster: Poster?,
    val relation: String?
)

data class HistoryEntry(
    val titleId: Long,
    val titleName: String,
    val posterUrl: String?,
    val episodeId: Long,
    val episodeNumber: Int,
    val timestamp: Long,
    val duration: Long,
    val watchedAt: Long
)

data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val items: List<PlaylistItem>
)

data class PlaylistItem(
    val playlistId: Long,
    val titleId: Long,
    val titleName: String
)

data class StudioResult(
    val source: String,
    val id: String,
    val title: String,
    val url: String?,
    val image: String?
)

data class StudioEpisode(
    val id: String,
    val number: Int,
    val title: String?
)

data class StudioVideo(
    val url: String,
    val quality: String?
)

data class ChangelogRelease(
    val tagName: String,
    val name: String,
    val body: String?,
    val publishedAt: String,
    val htmlUrl: String
)

data class FavoriteTitle(
    val titleId: Long,
    val titleName: String,
    val posterUrl: String?
)

data class MalAnime(
    val malId: Long,
    val title: String,
    val score: Double?,
    val rank: Int?,
    val popularity: Int?,
    val imageUrl: String?,
    val type: String?,
    val synopsis: String?
)

data class MalCharacter(
    val malId: Long,
    val name: String,
    val role: String?,
    val imageUrl: String?,
    val seiyuu: Seiyuu?
)

data class Seiyuu(
    val malId: Long,
    val name: String,
    val role: String?,
    val imageUrl: String?
)

enum class CollectionType(val value: String, val displayName: String) {
    WATCH_LATER("WATCH_LATER", "Буду смотреть"),
    WATCHING("WATCHING", "Смотрю"),
    COMPLETED("COMPLETED", "Просмотрено"),
    ON_HOLD("ON_HOLD", "Отложено"),
    DROPPED("DROPPED", "Брошено");

    companion object {
        fun fromValue(value: String): CollectionType? {
            return values().find { it.value == value }
        }
    }
}

data class CollectionItem(
    val releaseId: Long,
    val collectionType: CollectionType
)

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()
}
