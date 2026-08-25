package com.anilibrix.plus.data.repository

import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.data.remote.api.KodikApi
import com.anilibrix.plus.data.remote.dto.KodikMaterialDto
import com.anilibrix.plus.domain.model.Episode
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.StreamSourceInfo
import com.anilibrix.plus.domain.model.VoiceoverOption
import com.anilibrix.plus.domain.model.VoiceoverProvider
import com.anilibrix.plus.domain.model.VoiceoverType
import com.anilibrix.plus.domain.repository.KodikRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KodikRepositoryImpl @Inject constructor(
    private val api: KodikApi,
    private val settingsDataStore: SettingsDataStore,
    private val okHttpClient: OkHttpClient
) : KodikRepository {

    private suspend fun resolveToken(): String {
        val userToken = settingsDataStore.kodikCustomToken.first()
        return if (userToken.isNotBlank()) userToken else DEFAULT_PUBLIC_TOKENS.first()
    }

    override fun getVoiceovers(
        shikimoriId: Int?,
        malId: Long?,
        title: String?
    ): Flow<NetworkResult<List<VoiceoverOption>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val token = resolveToken()
            val response = when {
                shikimoriId != null && shikimoriId > 0 -> {
                    api.search(token = token, shikimoriId = shikimoriId.toString())
                }
                malId != null && malId > 0 -> {
                    api.search(token = token, malId = malId.toString())
                }
                !title.isNullOrBlank() -> {
                    api.search(token = token, title = title)
                }
                else -> {
                    emit(NetworkResult.Success(emptyList()))
                    return@flow
                }
            }

            val options = response.results
                .filter { it.translation != null && it.translation.title.isNotBlank() }
                .distinctBy { it.translation?.id ?: it.id }
                .map { item ->
                    val tr = item.translation!!
                    val isSub = tr.type.equals("subtitles", ignoreCase = true)
                    VoiceoverOption(
                        id = "kodik_${tr.id}",
                        name = if (isSub) "Субтитры (${tr.title})" else tr.title,
                        provider = VoiceoverProvider.KODIK,
                        type = if (isSub) VoiceoverType.SUBTITLES else VoiceoverType.VOICE,
                        episodesCount = item.lastEpisode ?: item.episodesCount,
                        translationId = tr.id,
                        link = item.link,
                        quality = item.quality
                    )
                }
                .sortedWith(
                    compareBy<VoiceoverOption> { it.type != VoiceoverType.VOICE }
                        .thenBy { it.name }
                )

            emit(NetworkResult.Success(options))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Ошибка получения озвучек Kodik", e))
        }
    }

    override fun getEpisodes(
        shikimoriId: Int?,
        malId: Long?,
        translationId: Long?,
        kodikId: String?
    ): Flow<NetworkResult<List<Episode>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val token = resolveToken()
            val response = when {
                shikimoriId != null && shikimoriId > 0 -> {
                    api.search(token = token, shikimoriId = shikimoriId.toString(), withEpisodes = true)
                }
                malId != null && malId > 0 -> {
                    api.search(token = token, malId = malId.toString(), withEpisodes = true)
                }
                else -> {
                    emit(NetworkResult.Success(emptyList()))
                    return@flow
                }
            }

            val targetMaterial: KodikMaterialDto? = if (translationId != null) {
                response.results.firstOrNull { it.translation?.id == translationId }
            } else if (kodikId != null) {
                response.results.firstOrNull { it.id == kodikId }
            } else {
                response.results.firstOrNull()
            }

            if (targetMaterial == null) {
                emit(NetworkResult.Success(emptyList()))
                return@flow
            }

            val episodesList = mutableListOf<Episode>()
            val seasons = targetMaterial.seasons

            if (seasons != null && seasons.isNotEmpty()) {
                // Если есть структура по сезонам и сериям
                seasons.entries.forEach { (seasonNum, seasonDto) ->
                    val eps = seasonDto.episodes.orEmpty()
                    eps.entries.forEach { (epNumStr, link) ->
                        val ordinal = epNumStr.toIntOrNull() ?: 1
                        val fullLink = normalizeLink(link)
                        val epId = generateKodikEpisodeId(shikimoriId, targetMaterial.translation?.id, ordinal)
                        episodesList.add(
                            Episode(
                                id = epId,
                                releaseEpisodeId = fullLink,
                                name = "Серия $ordinal",
                                ordinal = ordinal,
                                duration = 1440,
                                hls480 = fullLink,
                                hls720 = fullLink,
                                hls1080 = fullLink,
                                opening = null,
                                ending = null
                            )
                        )
                    }
                }
            } else if (targetMaterial.link != null) {
                // Одиночный фильм или OVA
                val fullLink = normalizeLink(targetMaterial.link)
                val epId = generateKodikEpisodeId(shikimoriId, targetMaterial.translation?.id, 1)
                episodesList.add(
                    Episode(
                        id = epId,
                        releaseEpisodeId = fullLink,
                        name = targetMaterial.title ?: "Полнометражный фильм",
                        ordinal = 1,
                        duration = 5400,
                        hls480 = fullLink,
                        hls720 = fullLink,
                        hls1080 = fullLink,
                        opening = null,
                        ending = null
                    )
                )
            }

            episodesList.sortBy { it.ordinal }
            emit(NetworkResult.Success(episodesList))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Ошибка загрузки серий Kodik", e))
        }
    }

    override suspend fun resolveStreamUrl(episodeLink: String): StreamSourceInfo? {
        val fullUrl = normalizeLink(episodeLink)
        return StreamSourceInfo(
            url = fullUrl,
            headers = mapOf(
                "Referer" to "https://kodik.info/",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
        )
    }

    private fun normalizeLink(link: String): String {
        return when {
            link.startsWith("//") -> "https:$link"
            !link.startsWith("http://") && !link.startsWith("https://") -> "https://$link"
            else -> link
        }
    }

    private fun generateKodikEpisodeId(shikimoriId: Int?, translationId: Long?, ordinal: Int): Long {
        val sId = (shikimoriId ?: 0).toLong()
        val tId = (translationId ?: 0L) % 100000L
        return 9_000_000_000L + (sId * 100_000L) + (tId * 100L) + (ordinal % 100)
    }

    companion object {
        val DEFAULT_PUBLIC_TOKENS = listOf(
            "40738d82d49fae69fb2f3e09c855a828",
            "q82jh5b16124l1k4m092305886",
            "12a3d077c5520e060c492a2a0ff39ef2"
        )
    }
}
