package com.anilibrix.plus.domain.repository

import com.anilibrix.plus.domain.model.Episode
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.StreamSourceInfo
import com.anilibrix.plus.domain.model.VoiceoverOption
import kotlinx.coroutines.flow.Flow

interface KodikRepository {

    /**
     * Поиск доступных вариантов озвучек в Kodik по shikimoriId, malId или названию.
     */
    fun getVoiceovers(
        shikimoriId: Int?,
        malId: Long?,
        title: String?
    ): Flow<NetworkResult<List<VoiceoverOption>>>

    /**
     * Получить список серий для конкретного варианта озвучки Kodik (по translationId или kodik id).
     */
    fun getEpisodes(
        shikimoriId: Int?,
        malId: Long?,
        translationId: Long?,
        kodikId: String?
    ): Flow<NetworkResult<List<Episode>>>

    /**
     * Получить прямую ссылку на видеопоток или параметры плеера Kodik.
     */
    suspend fun resolveStreamUrl(episodeLink: String): StreamSourceInfo?
}
