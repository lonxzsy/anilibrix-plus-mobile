package com.anilibrix.plus.data.repository

import com.anilibrix.plus.data.remote.api.AnilibriaApi
import com.anilibrix.plus.data.remote.dto.CollectionActionRequest
import com.anilibrix.plus.data.remote.dto.FavoriteActionRequest
import com.anilibrix.plus.data.remote.dto.ReleaseDto
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnilibriaRepositoryImpl @Inject constructor(
    private val api: AnilibriaApi
) : AnilibriaRepository {

    override fun getCatalog(page: Int, limit: Int, search: String?): Flow<NetworkResult<List<Title>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = api.getCatalog(page, limit, search)
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
            val days = response.mapNotNull { it.release }
                .groupBy { it.publishDay?.value ?: 0 }.toSortedMap()
                .map { (dayValue, releases) ->
                    val dayName = releases.firstOrNull()?.publishDay?.description ?: "����������"
                    ScheduleDay(
                        day = dayName,
                        releases = releases.map { it.toDomain() }
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
                        poster = rel.poster?.let { Poster(it.thumbnail?.toFullUrl(), it.preview?.toFullUrl(), it.src?.toFullUrl()) },
                        relation = node.name
                    )
                }
            }.sortedBy { it.id }
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
            val response = api.getTimecodes()
            emit(NetworkResult.Success(response.map { it.toHistoryEntry() }))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun updateTimecode(titleId: Long, episodeId: Long, timestamp: Long, duration: Long, releaseEpisodeId: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            api.updateTimecode(
                com.anilibrix.plus.data.remote.dto.TimecodeRequest(
                    titleId = com.anilibrix.plus.data.remote.dto.TimecodeField(time = titleId, releaseEpisodeId = releaseEpisodeId),
                    episodeId = com.anilibrix.plus.data.remote.dto.TimecodeField(time = episodeId, isWatched = true, releaseEpisodeId = releaseEpisodeId),
                    timestamp = com.anilibrix.plus.data.remote.dto.TimecodeField(time = timestamp, isWatched = true, releaseEpisodeId = releaseEpisodeId),
                    duration = com.anilibrix.plus.data.remote.dto.TimecodeField(time = duration, releaseEpisodeId = releaseEpisodeId)
                )
            )
            emit(NetworkResult.Success(Unit))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    override fun deleteTimecode(titleId: Long, episodeId: Long): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            api.deleteTimecode(titleId, episodeId)
            emit(NetworkResult.Success(Unit))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error", e))
        }
    }

    private fun String.toFullUrl(): String = if (this.startsWith("/")) "https://aniliberty.top$this" else this

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
                original = it.src?.toFullUrl()
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
        season = season?.let {
            val seasonName = try {
                SeasonName.valueOf(it.value?.uppercase() ?: "UNKNOWN")
            } catch (_: IllegalArgumentException) {
                SeasonName.UNKNOWN
            }
            Season(seasonName, year ?: 0)
        },
        year = year ?: 0,
        episodesTotal = episodesTotal ?: 0,
        isOngoing = isOngoing ?: false,
        score = score,
        episodes = episodes?.map { it.toDomain() },
        torrents = torrents?.map { it.toDomain() },
        isExternal = false,
        malId = malId
    )

    private fun com.anilibrix.plus.data.remote.dto.EpisodeDto.toDomain(): Episode = Episode(
        id = id?.toLongOrNull()
            ?: id?.hashCode()?.toLong()
            ?: (ordinal?.toLong()?.let { -it })
            ?: 0L,
        releaseEpisodeId = id.orEmpty(),
        name = name.orEmpty(),
        ordinal = ordinal ?: 0,
        duration = duration ?: 0,
        hls480 = hls480?.toFullUrl(),
        hls720 = hls720?.toFullUrl(),
        hls1080 = hls1080?.toFullUrl(),
        opening = opening?.let { SkipRange(it.start ?: 0.0, it.stop ?: 0.0) },
        ending = ending?.let { SkipRange(it.start ?: 0.0, it.stop ?: 0.0) }
    )

    private fun com.anilibrix.plus.data.remote.dto.TorrentDto.toDomain(): Torrent = Torrent(
        id = id.toLong(),
        quality = quality?.value,
        series = series,
        size = size,
        magnet = magnet,
        seeders = seeders,
        leechers = leechers
    )

    private fun ReleaseDto.toHistoryEntry(): HistoryEntry {
        val firstEpisode = episodes?.firstOrNull()
        return HistoryEntry(
            titleId = id.toLong(),
            titleName = name?.main.orEmpty(),
            posterUrl = poster?.preview?.toFullUrl(),
            episodeId = firstEpisode?.toDomain()?.id ?: 0L,
            episodeNumber = firstEpisode?.ordinal ?: 0,
            timestamp = 0,
            duration = 0,
            watchedAt = System.currentTimeMillis()
        )
    }
}
