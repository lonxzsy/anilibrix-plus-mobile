package com.anilibrix.plus.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты порогов и выбора «продолжить».
 *
 * Эти правила определяют, что человек видит на вкладке «Серии» и куда его
 * отправит кнопка возобновления. Ошибка здесь не падает, а тихо показывает
 * не ту серию, поэтому границы проверяются явно.
 */
class WatchProgressTest {

    private fun progress(
        episodeId: Long = 1L,
        number: Int = 1,
        position: Long,
        duration: Long = 100_000L,
        watchedAt: Long = 0L,
    ) = EpisodeProgress(
        episodeId = episodeId,
        releaseEpisodeId = "re-$episodeId",
        episodeNumber = number,
        positionMs = position,
        durationMs = duration,
        watchedAt = watchedAt,
    )

    private fun episode(id: Long, ordinal: Int) = Episode(
        id = id,
        releaseEpisodeId = "re-$id",
        name = "Серия $ordinal",
        ordinal = ordinal,
        duration = 1440,
        hls480 = null,
        hls720 = null,
        hls1080 = null,
        opening = null,
        ending = null,
    )

    @Test
    fun `серия считается просмотренной ровно с 90 процентов`() {
        assertFalse(progress(position = 89_999L).isWatched)
        assertTrue(progress(position = 90_000L).isWatched)
        assertTrue(progress(position = 100_000L).isWatched)
    }

    @Test
    fun `начатой считается серия после 3 процентов и до порога просмотра`() {
        assertFalse("ровно на пороге ещё не начата", progress(position = 3_000L).isStarted)
        assertTrue(progress(position = 3_001L).isStarted)
        assertFalse("просмотренная не является начатой", progress(position = 95_000L).isStarted)
    }

    @Test
    fun `нулевая длительность не приводит к делению на ноль`() {
        val p = progress(position = 5_000L, duration = 0L)
        assertEquals(0f, p.fraction, 0.0001f)
        assertFalse(p.isWatched)
        assertFalse(p.isStarted)
    }

    @Test
    fun `доля не выходит за единицу если позиция больше длительности`() {
        assertEquals(1f, progress(position = 200_000L, duration = 100_000L).fraction, 0.0001f)
    }

    @Test
    fun `счётчик для трекера считает только серии подряд с начала`() {
        // Просмотрены 1, 2 и 12-я. Честный ответ для трекера — 2.
        val titleProgress = TitleProgress(
            titleId = 1L,
            episodes = mapOf(
                1L to progress(episodeId = 1L, number = 1, position = 100_000L),
                2L to progress(episodeId = 2L, number = 2, position = 100_000L),
                12L to progress(episodeId = 12L, number = 12, position = 100_000L),
            ),
            totalEpisodes = 12,
        )
        assertEquals(2, titleProgress.consecutiveWatched)
        assertEquals(3, titleProgress.watchedCount)
    }

    @Test
    fun `продолжение ведёт на недосмотренную серию а не на следующую по порядку`() {
        val episodes = listOf(episode(1L, 1), episode(2L, 2), episode(3L, 3))
        val titleProgress = TitleProgress(
            titleId = 1L,
            episodes = mapOf(
                1L to progress(episodeId = 1L, number = 1, position = 100_000L, watchedAt = 10L),
                2L to progress(episodeId = 2L, number = 2, position = 40_000L, watchedAt = 20L),
            ),
            totalEpisodes = 3,
        )

        val target = titleProgress.resumeTarget(episodes)
        assertEquals(2, target?.episode?.ordinal)
        assertEquals(40_000L, target?.positionMs)
        assertTrue(target?.isResume == true)
    }

    @Test
    fun `если недосмотренных нет продолжение ведёт на следующую непросмотренную с нуля`() {
        val episodes = listOf(episode(1L, 1), episode(2L, 2), episode(3L, 3))
        val titleProgress = TitleProgress(
            titleId = 1L,
            episodes = mapOf(
                1L to progress(episodeId = 1L, number = 1, position = 100_000L, watchedAt = 10L),
            ),
            totalEpisodes = 3,
        )

        val target = titleProgress.resumeTarget(episodes)
        assertEquals(2, target?.episode?.ordinal)
        assertEquals(0L, target?.positionMs)
        assertFalse(target?.isResume == true)
    }

    @Test
    fun `полностью просмотренный тайтл предлагает начать сначала`() {
        val episodes = listOf(episode(1L, 1), episode(2L, 2))
        val titleProgress = TitleProgress(
            titleId = 1L,
            episodes = mapOf(
                1L to progress(episodeId = 1L, number = 1, position = 100_000L, watchedAt = 10L),
                2L to progress(episodeId = 2L, number = 2, position = 100_000L, watchedAt = 20L),
            ),
            totalEpisodes = 2,
        )

        assertTrue(titleProgress.isCompleted)
        assertEquals(1, titleProgress.resumeTarget(episodes)?.episode?.ordinal)
    }

    @Test
    fun `пустой список серий не даёт цели для продолжения`() {
        assertNull(TitleProgress(titleId = 1L).resumeTarget(emptyList()))
    }
}
