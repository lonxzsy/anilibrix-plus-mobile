package com.anilibrix.plus.core.database.entity

import androidx.room.Entity

@Entity(tableName = "history", primaryKeys = ["titleId", "episodeId"])
data class HistoryEntity(
    val titleId: Long,
    val episodeId: Long,
    val episodeNumber: Int,
    val timestamp: Long,
    val duration: Long,
    val watchedAt: Long = System.currentTimeMillis(),
    val titleName: String = "",
    val posterUrl: String? = null,
    /**
     * Идентификатор серии на стороне Anilibria (строковый, отдельный от [episodeId]).
     *
     * Без него удаление записи невозможно донести до сервера: `deleteTimecode`
     * принимает именно этот ключ. Пустая строка означает «запись создана до
     * появления поля» — такие записи удаляются только локально.
     */
    val releaseEpisodeId: String = "",
)
