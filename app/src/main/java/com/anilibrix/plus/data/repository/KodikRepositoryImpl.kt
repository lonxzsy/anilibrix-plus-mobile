package com.anilibrix.plus.data.repository

import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.data.remote.api.KodikApi
import com.anilibrix.plus.data.remote.dto.KodikMaterialDto
import com.anilibrix.plus.data.remote.dto.KodikSearchResponse
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

    private suspend fun getCandidateTokens(): List<String> {
        val userToken = settingsDataStore.kodikCustomToken.first()
        return buildList {
            if (userToken.isNotBlank()) add(userToken)
            addAll(DEFAULT_PUBLIC_TOKENS)
        }.distinct()
    }

    private suspend fun searchWithFallback(
        shikimoriId: Int?,
        malId: Long?,
        title: String?,
        withEpisodes: Boolean = false
    ): KodikSearchResponse {
        val tokens = getCandidateTokens()
        var lastException: Exception? = null

        for (token in tokens) {
            try {
                // 1. Поиск по Shikimori ID
                if (shikimoriId != null && shikimoriId > 0) {
                    val resp = api.search(token = token, shikimoriId = shikimoriId.toString(), withEpisodes = withEpisodes)
                    if (resp.results.isNotEmpty()) {
                        android.util.Log.d("KodikRepo", "Found ${resp.results.size} results by shikimoriId=$shikimoriId")
                        return resp
                    }
                }

                // 2. Поиск по MAL ID
                if (malId != null && malId > 0) {
                    val resp = api.search(token = token, malId = malId.toString(), withEpisodes = withEpisodes)
                    if (resp.results.isNotEmpty()) {
                        android.util.Log.d("KodikRepo", "Found ${resp.results.size} results by malId=$malId")
                        return resp
                    }
                }

                // 3. Поиск по названию
                if (!title.isNullOrBlank()) {
                    val cleanTitle = cleanSearchQuery(title)
                    val resp = api.search(token = token, title = cleanTitle, withEpisodes = withEpisodes)
                    if (resp.results.isNotEmpty()) {
                        android.util.Log.d("KodikRepo", "Found ${resp.results.size} results by title='$cleanTitle'")
                        return resp
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("KodikRepo", "Token ${token.take(6)}... failed with: ${e.message}, trying next token")
                lastException = e
            }
        }

        if (lastException != null) throw lastException
        return KodikSearchResponse(total = 0, results = emptyList())
    }

    private fun cleanSearchQuery(title: String): String {
        return title
            .replace(Regex("\\((?:ТВ|TV|Сезон|Season)[^)]*\\)"), "")
            .replace(Regex("\\[[^]]*]"), "")
            .trim()
    }

    override fun getVoiceovers(
        shikimoriId: Int?,
        malId: Long?,
        title: String?
    ): Flow<NetworkResult<List<VoiceoverOption>>> = flow {
        emit(NetworkResult.Loading)
        try {
            android.util.Log.d("KodikRepo", "getVoiceovers start: shikimoriId=$shikimoriId, malId=$malId, title='$title'")
            val response = searchWithFallback(shikimoriId, malId, title, withEpisodes = true)

            android.util.Log.d("KodikRepo", "getVoiceovers response: total=${response.total}, results=${response.results.size}")

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

            android.util.Log.d("KodikRepo", "Parsed voiceover options: ${options.size}")
            emit(NetworkResult.Success(options))
        } catch (e: Exception) {
            val errorMsg = if (e is retrofit2.HttpException) {
                val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
                if (body?.contains("неверный токен", ignoreCase = true) == true) {
                    "Неверный или устаревший токен Kodik API (укажите персональный токен в Настройках)"
                } else {
                    "Ошибка Kodik API (HTTP ${e.code()}): ${body ?: e.message()}"
                }
            } else {
                e.message ?: "Ошибка получения озвучек Kodik"
            }
            android.util.Log.e("KodikRepo", "getVoiceovers failed: $errorMsg", e)
            emit(NetworkResult.Error(errorMsg, e))
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
            android.util.Log.d("KodikRepo", "getEpisodes: shikimoriId=$shikimoriId, malId=$malId, translationId=$translationId, kodikId=$kodikId")
            val response = searchWithFallback(shikimoriId, malId, null, withEpisodes = true)

            android.util.Log.d("KodikRepo", "getEpisodes response: results=${response.results.size}")

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
            android.util.Log.d("KodikRepo", "Parsed ${episodesList.size} episodes from Kodik")
            emit(NetworkResult.Success(episodesList))
        } catch (e: Exception) {
            val errorMsg = if (e is retrofit2.HttpException) {
                val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
                if (body?.contains("неверный токен", ignoreCase = true) == true) {
                    "Неверный или устаревший токен Kodik API (укажите персональный токен в Настройках)"
                } else {
                    "Ошибка Kodik API (HTTP ${e.code()}): ${body ?: e.message()}"
                }
            } else {
                e.message ?: "Ошибка загрузки серий Kodik"
            }
            android.util.Log.e("KodikRepo", "getEpisodes failed: $errorMsg", e)
            emit(NetworkResult.Error(errorMsg, e))
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
            "12a3d077c5520e060c492a2a0ff39ef2",
            "e33286395b0c95ec1a0a58ad85532dd1",
            "a60c754d9241b71ef63402435e07661b",
            "15a1f6a1d82136e053f319202a906cf6"
        )
    }
}
