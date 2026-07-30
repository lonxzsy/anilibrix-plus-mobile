package com.anilibrix.plus.data.sync

import com.anilibrix.plus.domain.model.CollectionType

/**
 * Перевод статусов и оценок между нашей моделью и Shikimori.
 *
 * Собран в одном месте намеренно: как только такое соответствие расползается
 * по вызовам, две стороны начинают расходиться на одном-двух редких значениях,
 * и человек видит в трекере не тот статус, который поставил.
 */
object ShikimoriStatusMapper {

    private const val PLANNED = "planned"
    private const val WATCHING = "watching"
    private const val COMPLETED = "completed"
    private const val ON_HOLD = "on_hold"
    private const val DROPPED = "dropped"
    private const val REWATCHING = "rewatching"

    fun toShikimori(type: CollectionType): String = when (type) {
        CollectionType.WATCH_LATER -> PLANNED
        CollectionType.WATCHING -> WATCHING
        CollectionType.COMPLETED -> COMPLETED
        CollectionType.ON_HOLD -> ON_HOLD
        CollectionType.DROPPED -> DROPPED
    }

    fun fromShikimori(status: String): CollectionType? = when (status.lowercase()) {
        PLANNED -> CollectionType.WATCH_LATER
        WATCHING -> CollectionType.WATCHING
        COMPLETED -> CollectionType.COMPLETED
        ON_HOLD -> CollectionType.ON_HOLD
        DROPPED -> CollectionType.DROPPED
        // У нас нет отдельного «пересматриваю»; ближайшее по смыслу — «смотрю».
        // Обратно оно уедет как watching, и это осознанная потеря: заводить
        // шестой статус ради него в интерфейсе не окупается.
        REWATCHING -> CollectionType.WATCHING
        else -> null
    }

    /**
     * Наши 0..5 звёзд ↔ шкала Shikimori 1..10.
     *
     * Половинки звёзд у нас не выставляются, поэтому перевод точный в обе
     * стороны и на круг не теряет: 4.0 → 8 → 4.0.
     */
    fun scoreToShikimori(rating: Float): Int =
        (rating * 2f).toInt().coerceIn(0, 10)

    fun scoreFromShikimori(score: Int): Float =
        (score.coerceIn(0, 10) / 2f)
}
