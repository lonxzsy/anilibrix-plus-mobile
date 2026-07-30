package com.anilibrix.plus.core.database.entity

import androidx.room.Entity

@Entity(tableName = "collections", primaryKeys = ["titleId", "collectionType"])
data class CollectionEntity(
    val titleId: Long,
    val collectionType: String,
    val titleName: String = "",
    val posterUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    /**
     * Время последнего изменения статуса. Арбитр при расхождении с внешним
     * трекером: выигрывает более позднее изменение.
     */
    val updatedAt: Long = System.currentTimeMillis(),
    /** Связанное аниме на Shikimori; `null`, пока связь не установлена. */
    val shikimoriId: Int? = null,
    /**
     * Номер последней просмотренной серии в том виде, в каком его понимает
     * внешний трекер. Хранится отдельно от истории: история — это позиции
     * внутри серий, а трекеру нужен один счётчик на тайтл.
     */
    val progressEpisode: Int = 0,
)
