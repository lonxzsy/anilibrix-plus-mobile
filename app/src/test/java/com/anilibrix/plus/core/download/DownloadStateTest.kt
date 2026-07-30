package com.anilibrix.plus.core.download

import androidx.media3.exoplayer.offline.Download
import com.anilibrix.plus.ui.downloads.groupByTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Перевод состояний Media3 в то, что видит человек.
 *
 * Разница между «в очереди» и «приостановлено» неочевидна: у Media3 это одно
 * и то же `STATE_QUEUED`, различающееся только `stopReason`. Ошибка здесь
 * означает, что приостановленная из-за мобильной сети загрузка выглядит как
 * зависшая, — человек будет ждать, а она не пойдёт.
 */
class DownloadStateTest {

    @Test
    fun `очередь без причины остановки это очередь`() {
        assertEquals(
            DownloadState.QUEUED,
            DownloadState.fromMedia3(Download.STATE_QUEUED, Download.STOP_REASON_NONE),
        )
    }

    @Test
    fun `очередь с причиной остановки это пауза`() {
        assertEquals(
            DownloadState.PAUSED,
            DownloadState.fromMedia3(Download.STATE_QUEUED, DownloadRepository.STOP_REASON_MANUAL),
        )
    }

    @Test
    fun `остановленная загрузка это пауза независимо от причины`() {
        assertEquals(
            DownloadState.PAUSED,
            DownloadState.fromMedia3(Download.STATE_STOPPED, Download.STOP_REASON_NONE),
        )
    }

    @Test
    fun `перезапуск показывается как удаление а не как загрузка`() {
        // RESTARTING означает, что кэш сбрасывается и качается заново;
        // показывать это как «скачивается» значило бы врать про прогресс.
        assertEquals(
            DownloadState.REMOVING,
            DownloadState.fromMedia3(Download.STATE_RESTARTING, Download.STOP_REASON_NONE),
        )
    }

    @Test
    fun `активными считаются только незавершённые`() {
        assertTrue(DownloadState.QUEUED.isActive)
        assertTrue(DownloadState.DOWNLOADING.isActive)
        assertTrue(DownloadState.PAUSED.isActive)
        assertFalse(DownloadState.COMPLETED.isActive)
        assertFalse(DownloadState.FAILED.isActive)
        assertFalse(DownloadState.REMOVING.isActive)
    }

    @Test
    fun `сводка разделяет активные и завершённые`() {
        val summary = DownloadSummary(
            items = listOf(
                item("a", state = DownloadState.DOWNLOADING),
                item("b", state = DownloadState.COMPLETED),
                item("c", state = DownloadState.COMPLETED),
            )
        )
        assertEquals(1, summary.active.size)
        assertEquals(2, summary.completed.size)
        assertTrue(summary.hasActive)
    }

    @Test
    fun `группировка по тайтлу сортирует серии по номеру`() {
        val groups = listOf(
            item("a", titleId = 1L, episodeNumber = 3),
            item("b", titleId = 1L, episodeNumber = 1),
            item("c", titleId = 2L, titleName = "Альфа", episodeNumber = 2),
        ).groupByTitle()

        assertEquals(2, groups.size)
        val first = groups.first { it.titleId == 1L }
        assertEquals(listOf(1, 3), first.items.map { it.episodeNumber })
    }

    @Test
    fun `идентификатор запроса стабилен для одной и той же серии`() {
        assertEquals(
            DownloadRepository.requestId(10L, 20L),
            DownloadRepository.requestId(10L, 20L),
        )
        // Повторная постановка не должна плодить дубликаты, а разные серии
        // обязаны различаться.
        assertTrue(
            DownloadRepository.requestId(10L, 20L) != DownloadRepository.requestId(10L, 21L)
        )
    }

    private fun item(
        id: String,
        titleId: Long = 1L,
        titleName: String = "Тайтл",
        episodeNumber: Int = 1,
        state: DownloadState = DownloadState.COMPLETED,
    ) = DownloadItem(
        requestId = id,
        titleId = titleId,
        titleName = titleName,
        posterUrl = null,
        episodeId = episodeNumber.toLong(),
        episodeNumber = episodeNumber,
        episodeName = "",
        quality = "720",
        state = state,
        progress = 0f,
        downloadedBytes = 0L,
        totalBytes = null,
        createdAt = 0L,
    )
}
