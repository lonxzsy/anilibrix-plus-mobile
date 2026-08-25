package com.anilibrix.plus.data.repository

import com.anilibrix.plus.data.remote.api.AniSkipApi
import com.anilibrix.plus.domain.model.SkipRange
import com.anilibrix.plus.domain.repository.AniSkipRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniSkipRepositoryImpl @Inject constructor(
    private val api: AniSkipApi
) : AniSkipRepository {

    override suspend fun getSkipIntervals(
        malId: Long,
        episodeNumber: Int,
        episodeLengthSeconds: Double
    ): Pair<SkipRange?, SkipRange?> {
        if (malId <= 0 || episodeNumber <= 0) return Pair(null, null)

        return try {
            android.util.Log.d("AniSkipRepo", "Query skip times for malId=$malId, ep=$episodeNumber, len=$episodeLengthSeconds")
            val response = api.getSkipTimes(
                malId = malId,
                episodeNumber = episodeNumber,
                episodeLength = episodeLengthSeconds
            )

            if (!response.found || response.results.isEmpty()) {
                android.util.Log.d("AniSkipRepo", "No skip times found for malId=$malId, ep=$episodeNumber")
                return Pair(null, null)
            }

            var opening: SkipRange? = null
            var ending: SkipRange? = null

            for (item in response.results) {
                val start = item.interval.startTime
                val end = item.interval.endTime
                if (start >= end) continue

                when (item.skipType.lowercase()) {
                    "op", "mixed-op" -> {
                        if (opening == null) {
                            opening = SkipRange(start = start, stop = end)
                        }
                    }
                    "ed", "mixed-ed" -> {
                        if (ending == null) {
                            ending = SkipRange(start = start, stop = end)
                        }
                    }
                }
            }

            android.util.Log.d("AniSkipRepo", "AniSkip resolved: op=$opening, ed=$ending")
            Pair(opening, ending)
        } catch (e: Exception) {
            android.util.Log.e("AniSkipRepo", "AniSkip request failed: ${e.message}")
            Pair(null, null)
        }
    }
}
