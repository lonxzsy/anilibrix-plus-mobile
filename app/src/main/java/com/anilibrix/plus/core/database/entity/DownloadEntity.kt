package com.anilibrix.plus.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Метаданные скачанной серии.
 *
 * Состояние загрузки (прогресс, статус, размер, ошибка) здесь намеренно **не**
 * хранится: единственный источник правды по нему — `DownloadIndex` из Media3.
 * Дублирование дало бы два расходящихся представления одного и того же, а
 * рассинхрон в загрузках выглядит для человека как потерянный файл.
 *
 * Здесь только то, чего Media3 про наши загрузки знать не может: как называется
 * тайтл, какой у него постер, какая это серия.
 *
 * [requestId] совпадает с `Download.request.id` — по нему две половины и
 * сшиваются.
 */
@Entity(tableName = "downloads", indices = [Index("titleId")])
data class DownloadEntity(
    @PrimaryKey val requestId: String,
    val titleId: Long,
    val titleName: String = "",
    val posterUrl: String? = null,
    val episodeId: Long = 0L,
    val releaseEpisodeId: String = "",
    val episodeNumber: Int = 0,
    val episodeName: String = "",
    val quality: String = "",
    val durationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
)
