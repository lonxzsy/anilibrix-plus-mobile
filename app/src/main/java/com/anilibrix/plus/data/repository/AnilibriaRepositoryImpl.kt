package com.anilibrix.plus.data.repository

import com.anilibrix.plus.data.remote.api.AnilibriaApi
import com.anilibrix.plus.data.remote.dto.DeleteTimecodeRequest
import com.anilibrix.plus.data.remote.dto.TimecodeRequest
import com.anilibrix.plus.data.remote.dto.CollectionActionRequest
import com.anilibrix.plus.data.remote.dto.FavoriteActionRequest
import com.anilibrix.plus.data.remote.dto.ReleaseDto
import com.anilibrix.plus.domain.model.CatalogQuery
import com.anilibrix.plus.domain.model.CatalogSort
import com.anilibrix.plus.domain.model.CatalogStatus
import com.anilibrix.plus.domain.model.CollectionItem
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.Episode
import com.anilibrix.plus.domain.model.FranchiseItem
import com.anilibrix.plus.domain.model.Genre
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.Poster
import com.anilibrix.plus.domain.model.ReleaseType
import com.anilibrix.plus.domain.model.ScheduleDay
import com.anilibrix.plus.domain.model.ScheduleEntry
import com.anilibrix.plus.domain.model.Season
import com.anilibrix.plus.domain.model.SeasonName
import com.anilibrix.plus.domain.model.SkipRange
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.domain.model.TitleName
import com.anilibrix.plus.domain.model.Torrent
import com.anilibrix.plus.domain.model.User
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnilibriaRepositoryImpl @Inject constructor(
    private val api: AnilibriaApi
) : AnilibriaRepository {

    override fun getCatalog(query: CatalogQuery): Flow<NetworkResult<List<Title>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.getCatalog(
                page = query.page,
                limit = query.limit,
                filters = query.toQueryMap()
            )
            // Никакой клиентской дофильтрации: сервер уже применил фильтры из
            // `toQueryMap()`, а повторный отсев на клиенте делал недостоверными
            // и номер страницы, и `hasMore`. Пагинация считает, что страница
            // отдала N элементов, а до экрана доходило меньше — подгрузка
            // останавливалась раньше времени и часть каталога становилась
            // недоступной.
            emit(NetworkResult.Success(response.data.map { it.toDomain() }))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getRelease(idOrAlias: String): Flow<NetworkResult<Title>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.getRelease(idOrAlias)
            emit(NetworkResult.Success(response.toDomain()))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getSchedule(): Flow<NetworkResult<List<ScheduleDay>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.getSchedule()
            val days = response
                .groupBy { it.release?.publishDay?.value ?: 0 }
                .toSortedMap()
                .map { (dayValue, items) ->
                    val dayName = items.firstOrNull()?.release?.publishDay?.description ?: "Неизвестно"
                    // Номера серий берём у КАЖДОГО релиза отдельно: раньше
                    // здесь стоял firstOrNull(), и номер серии первого тайтла
                    // приписывался всему дню.
                    val entries = items.mapNotNull { item ->
                        val release = item.release?.toDomain() ?: return@mapNotNull null
                        ScheduleEntry(
                            title = release,
                            publishedEpisode = item.publishedReleaseEpisode?.ordinal?.toInt(),
                            nextEpisode = item.nextReleaseEpisodeNumber,
                        )
                    }
                    ScheduleDay(
                        day = dayName,
                        entries = entries,
                        dayOfWeek = dayValue
                    )
                }
            emit(NetworkResult.Success(days))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getFranchise(releaseId: Long): Flow<NetworkResult<List<FranchiseItem>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.getFranchise(releaseId)
            val items = response.flatMap { node ->
                node.franchiseReleases.mapNotNull { fRel ->
                    val rel = fRel.release ?: return@mapNotNull null
                    FranchiseItem(
                        id = rel.id.toLong(),
                        name = rel.name?.main ?: node.name.orEmpty(),
                        poster = rel.poster?.let {
                            Poster(
                                small = it.thumbnail?.toFullUrl(),
                                medium = it.preview?.toFullUrl(),
                                original = it.src?.toFullUrl(),
                                smallOptimized = it.optimized?.thumbnail?.toFullUrl(),
                                mediumOptimized = it.optimized?.preview?.toFullUrl(),
                                originalOptimized = it.optimized?.src?.toFullUrl()
                            )
                        },
                        relation = node.name,
                        sortOrder = fRel.sortOrder ?: 0
                    )
                }
            }.sortedBy { it.sortOrder }
            emit(NetworkResult.Success(items))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getRecommended(limit: Int, releaseId: Long?): Flow<NetworkResult<List<Title>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.getRecommended(limit, releaseId)
            emit(NetworkResult.Success(response.map { it.toDomain() }))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getTorrents(releaseId: Long): Flow<NetworkResult<List<Torrent>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.getTorrents(releaseId)
            emit(NetworkResult.Success(response.map { it.toDomain() }))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun login(login: String, password: String): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.login(com.anilibrix.plus.data.remote.dto.LoginRequest(login, password))
            val token = response.effectiveToken
            if (token != null) {
                emit(NetworkResult.Success(token))
            } else {
                emit(NetworkResult.Error("Token is null"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getProfile(): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.getProfile()
            emit(NetworkResult.Success(User(
                id = response.id.toLong(),
                login = response.login.orEmpty(),
                nickname = response.nickname,
                email = response.email,
                avatarUrl = response.avatarUrl?.toFullUrl(),
                profileUrl = null,
                isBanned = response.isBanned,
                createdAt = response.createdAt,
                torrentStats = response.torrents?.let {
                    com.anilibrix.plus.domain.model.TorrentStats(
                        passkey = it.passkey,
                        uploaded = it.uploaded,
                        downloaded = it.downloaded
                    )
                }
            )))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getFavoriteIds(): Flow<NetworkResult<List<Long>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.getFavoriteIds()
            emit(NetworkResult.Success(response.ids?.map { it.toLong() } ?: emptyList()))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getFavoriteReleases(): Flow<NetworkResult<List<Title>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.getFavoriteReleases()
            emit(NetworkResult.Success(response.map { it.toDomain() }))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun addFavorite(releaseId: Long): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            api.addFavorite(listOf(FavoriteActionRequest(releaseId.toInt())))
            emit(NetworkResult.Success(Unit))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun removeFavorite(releaseId: Long): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            api.removeFavorite(listOf(FavoriteActionRequest(releaseId.toInt())))
            emit(NetworkResult.Success(Unit))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getCollectionIds(): Flow<NetworkResult<List<CollectionItem>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.getCollectionIds()
            val items = response.ids?.mapNotNull { item ->
                val type = CollectionType.fromValue(item.typeOfCollection ?: "") ?: return@mapNotNull null
                CollectionItem(
                    releaseId = item.releaseId.toLong(),
                    collectionType = type
                )
            } ?: emptyList()
            emit(NetworkResult.Success(items))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getCollectionReleases(collectionType: CollectionType, page: Int, limit: Int): Flow<NetworkResult<List<Title>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.getCollectionReleases(collectionType.value, page, limit)
            emit(NetworkResult.Success(response.data.map { it.toDomain() }))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun addToCollection(releaseId: Long, collectionType: CollectionType): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            api.addToCollection(listOf(CollectionActionRequest(releaseId.toInt(), collectionType.value)))
            emit(NetworkResult.Success(Unit))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun removeFromCollection(releaseId: Long, collectionType: CollectionType): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            api.removeFromCollection(listOf(CollectionActionRequest(releaseId.toInt(), collectionType.value)))
            emit(NetworkResult.Success(Unit))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getHistory(): Flow<NetworkResult<List<HistoryEntry>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.getHistory()
            emit(NetworkResult.Success(response.map { it.toHistoryEntry() }))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun getTimecodes(): Flow<NetworkResult<List<HistoryEntry>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val timecodes = api.getTimecodes()
            val entries = timecodes.mapNotNull { timecode ->
                if (timecode.releaseEpisodeId.isBlank() || timecode.time <= 0.0) return@mapNotNull null

                val episode = runCatching { api.getEpisode(timecode.releaseEpisodeId) }.getOrNull()
                val durationMs = episode?.duration?.secondsToMillis() ?: 0L
                val timestampMs = timecode.time.secondsToMillis().let { timestamp ->
                    if (durationMs > 0L) timestamp.coerceAtMost(durationMs) else timestamp
                }
                if (timestampMs <= 0L) return@mapNotNull null

                val release = episode?.release
                HistoryEntry(
                    titleId = release?.id?.toLong() ?: episode?.releaseId ?: 0L,
                    titleName = release?.name?.main ?: episode?.name.orEmpty(),
                    posterUrl = release?.poster?.preview?.toFullUrl(),
                    episodeId = episode?.toDomain()?.id ?: 0L,
                    episodeNumber = episode?.ordinal?.toInt() ?: 0,
                    timestamp = timestampMs,
                    duration = durationMs,
                    watchedAt = System.currentTimeMillis()
                )
            }
            emit(NetworkResult.Success(entries))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun updateTimecode(releaseEpisodeId: String, timestamp: Long, duration: Long): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            if (releaseEpisodeId.isBlank() || timestamp <= 0L || duration <= 0L) {
                emit(NetworkResult.Success(Unit))
                return@flow
            }
            api.updateTimecode(
                listOf(
                    TimecodeRequest(
                        time = timestamp.toSeconds(),
                        isWatched = timestamp >= duration - WATCHED_THRESHOLD_MS,
                        releaseEpisodeId = releaseEpisodeId
                    )
                )
            )
            emit(NetworkResult.Success(Unit))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun deleteTimecode(releaseEpisodeId: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            if (releaseEpisodeId.isBlank()) {
                emit(NetworkResult.Success(Unit))
                return@flow
            }
            api.deleteTimecode(listOf(DeleteTimecodeRequest(releaseEpisodeId)))
            emit(NetworkResult.Success(Unit))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    private fun String.toFullUrl(): String = if (this.startsWith("/")) "https://aniliberty.top$this" else this

    private fun Double.secondsToMillis(): Long = (this * 1000.0).toLong().coerceAtLeast(0L)

    private fun Long.toSeconds(): Double = this.coerceAtLeast(0L) / 1000.0

    private fun CatalogQuery.toQueryMap(): Map<String, String> = buildMap {
        search?.takeIf { it.isNotBlank() }?.let { put("f[search]", it) }
        if (genres.isNotEmpty()) put("f[genres]", genres.joinToString(","))
        year?.let { put("f[years]", it.toString()) }
        type?.takeIf { it != ReleaseType.UNKNOWN }?.let {
            put("f[types]", it.name.lowercase(Locale.ROOT))
        }
        season?.takeIf { it != SeasonName.UNKNOWN }?.let {
            put("f[seasons]", it.name.lowercase(Locale.ROOT))
        }
        status?.let {
            put("f[is_ongoing]", (it == CatalogStatus.ONGOING).toString())
            put("f[statuses]", it.apiValue)
        }
        if (sort != CatalogSort.UPDATED) put("sort", sort.apiValue)
    }

    private companion object {
        const val WATCHED_THRESHOLD_MS = 10_000L
    }

    private fun ReleaseDto.toDomain(): Title = Title(
        id = id.toLong(),
        alias = alias,
        name = TitleName(
            main = name?.main.orEmpty(),
            english = name?.english,
            alternative = name?.alternative
        ),
        description = description,
        poster = poster?.let {
            Poster(
                small = it.thumbnail?.toFullUrl(),
                medium = it.preview?.toFullUrl(),
                original = it.src?.toFullUrl(),
                smallOptimized = it.optimized?.thumbnail?.toFullUrl(),
                mediumOptimized = it.optimized?.preview?.toFullUrl(),
                originalOptimized = it.optimized?.src?.toFullUrl()
            )
        },
        genres = genres?.map { Genre(it.id.toLong(), it.name.orEmpty()) } ?: emptyList(),
        type = type?.let {
            try {
                ReleaseType.valueOf(it.value?.uppercase() ?: "UNKNOWN")
            } catch (_: IllegalArgumentException) {
                ReleaseType.UNKNOWN
            }
        },
        typeDescription = type?.description,
        season = season?.let {
            val seasonName = try {
                SeasonName.valueOf(it.value?.uppercase() ?: "UNKNOWN")
            } catch (_: IllegalArgumentException) {
                SeasonName.UNKNOWN
            }
            Season(seasonName, year ?: 0)
        },
        seasonDescription = season?.description,
        year = year ?: 0,
        episodesTotal = episodesTotal ?: 0,
        isOngoing = isOngoing ?: false,
        score = score,
        episodes = episodes?.map { it.toDomain() },
        torrents = torrents?.map { it.toDomain() },
        isExternal = false,
        malId = malId,
        inFavorites = inFavorites ?: false
    )

    private fun com.anilibrix.plus.data.remote.dto.EpisodeDto.toDomain(): Episode = Episode(
        id = id?.toLongOrNull()
            ?: (ordinal?.toLong()?.let { -it })
            ?: 0L,
        releaseEpisodeId = id.orEmpty(),
        name = name.orEmpty(),
        ordinal = ordinal?.toInt() ?: 0,
        duration = duration?.toInt() ?: 0,
        hls480 = hls480?.toFullUrl(),
        hls720 = hls720?.toFullUrl(),
        hls1080 = hls1080?.toFullUrl(),
        opening = opening?.let { SkipRange(it.start ?: 0.0, it.stop ?: 0.0) },
        ending = ending?.let { SkipRange(it.start ?: 0.0, it.stop ?: 0.0) }
    )

    private fun com.anilibrix.plus.data.remote.dto.TorrentDto.toDomain(): Torrent {
        val parsed = com.anilibrix.plus.core.torrent.TorrentNameParser.parse("[AniLibria] ${series.orEmpty()} [${quality?.value.orEmpty()}]")
        return Torrent(
            id = id.toLong(),
            quality = quality?.value ?: parsed.quality,
            series = if (!series.isNullOrBlank()) series else parsed.episodeLabel,
            size = size,
            magnet = magnet,
            seeders = seeders,
            leechers = leechers,
            rawTitle = series,
            releaseGroup = "AniLibria",
            cleanTitle = null,
            episodeNumbers = parsed.episodeNumbers,
            isBatch = parsed.isBatch || (parsed.episodeNumbers.size > 1),
            videoCodec = parsed.videoCodec,
            audioInfo = "RUS (AniLibria)",
            subtitleInfo = null,
            torrentUrl = "https://anilibria.top/public/torrent/download.php?id=$id"
        )
    }

    private fun ReleaseDto.toHistoryEntry(): HistoryEntry {
        val firstEpisode = episodes?.firstOrNull()
        return HistoryEntry(
            titleId = id.toLong(),
            titleName = name?.main.orEmpty(),
            posterUrl = poster?.preview?.toFullUrl(),
            episodeId = firstEpisode?.toDomain()?.id ?: 0L,
            episodeNumber = firstEpisode?.ordinal?.toInt() ?: 0,
            timestamp = 0,
            duration = 0,
            watchedAt = System.currentTimeMillis()
        )
    }
}
