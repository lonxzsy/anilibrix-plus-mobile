package com.anilibrix.plus.domain.repository

import com.anilibrix.plus.domain.model.SkipRange

interface AniSkipRepository {

    /**
     * Получить таймкоды пропуска опенинга и эндинга для указанной серии.
     */
    suspend fun getSkipIntervals(
        malId: Long,
        episodeNumber: Int,
        episodeLengthSeconds: Double = 0.0
    ): Pair<SkipRange?, SkipRange?>
}
