package com.anilibrix.plus.domain.model

import androidx.compose.runtime.Immutable

/**
 * Прогресс по одной серии.
 *
 * Данные для этого всегда лежали в базе — `history` хранит и позицию, и
 * длительность, — но никто их не читал: `HistoryDao.getByTitleId()` не
 * вызывался ни разу. Поэтому на вкладке «Серии» не было ни одной отметки о
 * просмотре, а «продолжить» приходилось искать глазами.
 */
@Immutable
data class EpisodeProgress(
    val episodeId: Long,
    val releaseEpisodeId: String,
    val episodeNumber: Int,
    val positionMs: Long,
    val durationMs: Long,
    val watchedAt: Long,
) {
    val fraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    /**
     * Серия считается просмотренной с 90%.
     *
     * Не 100%: на титрах человек почти всегда закрывает плеер, и требовать
     * досмотра до последнего кадра значило бы не засчитывать вообще ничего.
     */
    val isWatched: Boolean
        get() = fraction >= WATCHED_THRESHOLD

    /** Начата, но не досмотрена — только такие серии имеет смысл «продолжать». */
    val isStarted: Boolean
        get() = !isWatched && fraction > STARTED_THRESHOLD

    val remainingMs: Long
        get() = (durationMs - positionMs).coerceAtLeast(0L)

    companion object {
        const val WATCHED_THRESHOLD = 0.90f
        const val STARTED_THRESHOLD = 0.03f
    }
}

/**
 * Сводный прогресс по тайтлу: что просмотрено, где остановились, что дальше.
 */
@Immutable
data class TitleProgress(
    val titleId: Long,
    /** Ключ — `episodeId`. */
    val episodes: Map<Long, EpisodeProgress> = emptyMap(),
    val totalEpisodes: Int = 0,
) {
    val watchedCount: Int get() = episodes.values.count { it.isWatched }

    /** Последняя по времени активность — источник для «Продолжить». */
    val lastWatched: EpisodeProgress? get() = episodes.values.maxByOrNull { it.watchedAt }

    val isCompleted: Boolean
        get() = totalEpisodes > 0 && watchedCount >= totalEpisodes

    /**
     * Номер серии для внешнего трекера: сколько серий подряд с начала
     * просмотрено.
     *
     * Именно подряд, а не всего: трекеры оперируют одним счётчиком, и если
     * человек посмотрел 1, 2 и 12-ю серию, честный ответ — 2, а не 3 и не 12.
     */
    val consecutiveWatched: Int
        get() {
            val byNumber = episodes.values.filter { it.isWatched }.map { it.episodeNumber }.toSet()
            var n = 0
            while (byNumber.contains(n + 1)) n++
            return n
        }

    fun progressOf(episodeId: Long): EpisodeProgress? = episodes[episodeId]

    /**
     * С какой серии продолжать.
     *
     * Приоритет у начатой-но-недосмотренной: человек ушёл с середины и
     * возвращаться хочет туда же. Если недосмотренных нет — следующая
     * непросмотренная по порядку.
     */
    fun resumeTarget(episodes: List<Episode>): ResumeTarget? {
        if (episodes.isEmpty()) return null

        val started = this.episodes.values
            .filter { it.isStarted }
            .maxByOrNull { it.watchedAt }
        if (started != null) {
            episodes.firstOrNull { it.id == started.episodeId }?.let {
                return ResumeTarget(episode = it, positionMs = started.positionMs, isResume = true)
            }
        }

        val nextUnwatched = episodes.firstOrNull { episode ->
            this.episodes[episode.id]?.isWatched != true
        }
        return when {
            nextUnwatched != null -> ResumeTarget(nextUnwatched, positionMs = 0L, isResume = false)
            // Всё просмотрено — предлагаем пересмотреть с начала, а не
            // показываем недоступную кнопку.
            else -> ResumeTarget(episodes.first(), positionMs = 0L, isResume = false)
        }
    }
}

/** Куда ведёт кнопка «Смотреть»/«Продолжить». */
@Immutable
data class ResumeTarget(
    val episode: Episode,
    val positionMs: Long,
    /** `true`, если это возврат к недосмотренной серии, а не старт новой. */
    val isResume: Boolean,
)
