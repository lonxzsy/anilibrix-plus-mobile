package com.anilibrix.plus.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Title(
    val id: Long,
    val alias: String,
    val name: TitleName,
    val description: String?,
    val poster: Poster?,
    val genres: List<Genre>,
    val type: ReleaseType?,
    val typeDescription: String?,
    val season: Season?,
    val seasonDescription: String?,
    val year: Int,
    val episodesTotal: Int,
    val isOngoing: Boolean,
    val score: Double?,
    val episodes: List<Episode>?,
    val torrents: List<Torrent>?,
    val isExternal: Boolean,
    val malId: Int?,
    val inFavorites: Boolean = false
)

@Immutable
data class TitleName(
    val main: String,
    val english: String?,
    val alternative: String?
)

@Immutable
data class Poster(
    val small: String?,
    val medium: String?,
    val original: String?,
    val smallOptimized: String? = null,
    val mediumOptimized: String? = null,
    val originalOptimized: String? = null
) {
    /**
     * Оптимизированные варианты (WebP) отдаются тем же API и весят заметно
     * меньше, но не использовались нигде: приложение всё это время грузило
     * тяжёлые оригиналы. На сетке из сорока карточек разница видна и по
     * скорости появления, и по трафику.
     *
     * Фолбэк на неоптимизированный обязателен — у части старых релизов
     * оптимизированных вариантов нет.
     */
    val thumbUrl: String? get() = smallOptimized ?: small

    /** Для карточек в сетке и списках. */
    val cardUrl: String? get() = mediumOptimized ?: medium ?: smallOptimized ?: small

    /** Для крупных изображений — бэкдроп, hero, полноэкранный просмотр. */
    val fullUrl: String? get() = originalOptimized ?: original ?: mediumOptimized ?: medium
}

@Immutable
data class Genre(
    val id: Long,
    val name: String
)

enum class ReleaseType(val displayName: String) {
    TV("TV"),
    OVA("OVA"),
    ONA("ONA"),
    MOVIE("Фильм"),
    SPECIAL("Спешл"),
    UNKNOWN("Неизвестно")
}

data class Season(
    val name: SeasonName,
    val year: Int
)

enum class SeasonName {
    WINTER, SPRING, SUMMER, FALL, UNKNOWN
}

@Immutable
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

@Immutable
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
    val leechers: Int?,
    val rawTitle: String? = null,
    val releaseGroup: String? = null,
    val cleanTitle: String? = null,
    val episodeNumbers: List<Int> = emptyList(),
    val isBatch: Boolean = false,
    val videoCodec: String? = null,
    val audioInfo: String? = null,
    val subtitleInfo: String? = null,
    val torrentUrl: String? = null
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
    val entries: List<ScheduleEntry>,
    /**
     * День недели по ISO-8601: 1 — понедельник, 7 — воскресенье.
     * Нужен, чтобы экран расписания открывался сразу на сегодняшнем дне.
     */
    val dayOfWeek: Int = 0
) {
    val releases: List<Title> get() = entries.map { it.title }
}

/**
 * Один релиз в расписании вместе с номерами серий.
 *
 * Раньше `publishedEpisode` и `nextEpisode` лежали на самом дне и брались у
 * **первого** релиза списка — то есть номер серии одного тайтла приписывался
 * всем остальным в этот день. Впрочем, до экрана они всё равно не доходили:
 * их никто не отрисовывал.
 */
@Immutable
data class ScheduleEntry(
    val title: Title,
    /** Номер уже вышедшей серии. */
    val publishedEpisode: Int? = null,
    /** Номер серии, которая выйдет следующей. */
    val nextEpisode: Int? = null,
)

@Immutable
data class FranchiseItem(
    val id: Long,
    val name: String,
    val poster: Poster?,
    val relation: String?,
    val sortOrder: Int = 0
)

@Immutable
data class HistoryEntry(
    val titleId: Long,
    val titleName: String,
    val posterUrl: String?,
    val episodeId: Long,
    val episodeNumber: Int,
    val timestamp: Long,
    val duration: Long,
    val watchedAt: Long,
    /**
     * Серверный ключ серии. Нужен, чтобы удаление записи доехало до Anilibria:
     * `deleteTimecode` принимает именно его, а не числовой [episodeId].
     */
    val releaseEpisodeId: String = ""
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
    val htmlUrl: String,
    /**
     * Готовый APK из релиза.
     *
     * `browser_download_url` разбирался из ответа GitHub с самого начала, но
     * до доменной модели не доходил — обновиться из приложения было нельзя,
     * хотя файл лежал в двух шагах.
     */
    val apkUrl: String? = null,
    val apkSizeBytes: Long = 0,
    val isPrerelease: Boolean = false,
)

data class GitHubIssue(
    val number: Int,
    val title: String,
    val body: String?,
    val state: String,
    val createdAt: String,
    val htmlUrl: String,
    val userLogin: String?,
    val userAvatarUrl: String?
)

data class ShikimoriAnime(
    val id: Int,
    val name: String,
    val russian: String?,
    val score: Double?,
    val status: String?,
    val kind: String?,
    val episodes: Int,
    val episodesAired: Int,
    val airedOn: String?,
    val posterOriginal: String?,
    val posterPreview: String?,
    val description: String?,
    val url: String?
)

data class ShikimoriCharacter(
    val id: Int,
    val name: String,
    val russian: String?,
    val imageUrl: String?,
    val role: String?,
    val seiyus: List<ShikimoriSeiyu>
)

data class ShikimoriSeiyu(
    val id: Int,
    val name: String,
    val russian: String?,
    val imageUrl: String?
)

/** Кадр из аниме. `preview` — для ленты, `original` — для полноэкранного просмотра. */
@Immutable
data class ShikimoriScreenshot(
    val preview: String?,
    val original: String?,
)

/** Персонаж из поиска: столько, сколько нужно строке подсказки. */
@Immutable
data class ShikimoriCharacterSearchResult(
    val id: Int,
    val name: String,
    val russian: String?,
    val imageUrl: String?,
) {
    val displayName: String get() = russian?.takeIf { it.isNotBlank() } ?: name
}

data class ShikimoriRelated(
    val id: Int,
    val name: String,
    val russian: String?,
    val imageUrl: String?,
    val relation: String?
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

/**
 * Полная карточка персонажа — то, что показывает отдельный экран.
 *
 * Отличается от [MalCharacter] тем, что это сам персонаж, а не его роль в
 * конкретном тайтле: есть биография, все озвучки и список аниме с ним.
 */
@Immutable
data class MalCharacterDetail(
    val malId: Long,
    val name: String,
    val nameKanji: String?,
    val nicknames: List<String>,
    val about: String?,
    val favorites: Int,
    val imageUrl: String?,
    val voiceActors: List<Seiyuu>,
    val appearsIn: List<CharacterAppearance>,
)

@Immutable
data class CharacterAppearance(
    val malId: Long,
    val title: String,
    val role: String?,
    val imageUrl: String?,
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

@Immutable
data class CollectionItem(
    val releaseId: Long,
    val collectionType: CollectionType
)

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()
}
