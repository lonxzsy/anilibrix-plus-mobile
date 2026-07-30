package com.anilibrix.plus.core.download

import androidx.compose.runtime.Immutable
import androidx.media3.exoplayer.offline.Download

/** Состояние загрузки в терминах интерфейса, а не Media3. */
enum class DownloadState {
    QUEUED,
    DOWNLOADING,
    /** Приостановлена — вручную или из-за неподходящей сети. */
    PAUSED,
    COMPLETED,
    FAILED,
    REMOVING,
    ;

    val isActive: Boolean get() = this == QUEUED || this == DOWNLOADING || this == PAUSED

    companion object {
        fun fromMedia3(state: Int, stopReason: Int): DownloadState = when (state) {
            Download.STATE_QUEUED -> if (stopReason != Download.STOP_REASON_NONE) PAUSED else QUEUED
            Download.STATE_DOWNLOADING -> DOWNLOADING
            Download.STATE_COMPLETED -> COMPLETED
            Download.STATE_FAILED -> FAILED
            Download.STATE_REMOVING, Download.STATE_RESTARTING -> REMOVING
            Download.STATE_STOPPED -> PAUSED
            else -> QUEUED
        }
    }
}

/**
 * Загрузка целиком: прогресс из Media3 плюс метаданные из нашей таблицы.
 *
 * Прогресс и статус живут только в `DownloadIndex` — они здесь не хранятся, а
 * читаются. Название, постер и номер серии Media3 про наши загрузки знать не
 * может, поэтому лежат в Room.
 */
@Immutable
data class DownloadItem(
    val requestId: String,
    val titleId: Long,
    val titleName: String,
    val posterUrl: String?,
    val episodeId: Long,
    val episodeNumber: Int,
    val episodeName: String,
    val quality: String,
    val state: DownloadState,
    /** 0..1. У поставленных в очередь равен нулю. */
    val progress: Float,
    val downloadedBytes: Long,
    /** Общий размер; `null`, пока Media3 его не вычислил. */
    val totalBytes: Long?,
    val createdAt: Long,
) {
    val isPlayable: Boolean get() = state == DownloadState.COMPLETED
}

/** Сводка для экрана и настроек. */
@Immutable
data class DownloadSummary(
    val items: List<DownloadItem> = emptyList(),
    val usedBytes: Long = 0L,
) {
    val active: List<DownloadItem> get() = items.filter { it.state.isActive }
    val completed: List<DownloadItem> get() = items.filter { it.state == DownloadState.COMPLETED }
    val hasActive: Boolean get() = active.isNotEmpty()
}
