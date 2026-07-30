package com.anilibrix.plus.core.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Вид отложенного действия.
 *
 * Хранится строкой, а не порядковым номером enum: порядок в enum меняется при
 * рефакторинге, и записи в очереди, пережившие обновление приложения,
 * превратились бы в чужие операции.
 */
enum class SyncOperationKind {
    /** Статус тайтла изменился (или снят, если статус в payload пустой). */
    COLLECTION_STATUS,

    /** Тайтл добавлен/убран из избранного. */
    FAVORITE,

    /** Позиция просмотра серии. */
    TIMECODE_UPDATE,

    /** Запись истории удалена — надо убрать таймкод и на сервере. */
    TIMECODE_DELETE,

    /** Прогресс и статус нужно донести до Shikimori. */
    SHIKIMORI_RATE,
    ;

    companion object {
        fun fromValue(value: String): SyncOperationKind? = entries.find { it.name == value }
    }
}

/**
 * Полезная нагрузка операции.
 *
 * Одна плоская структура на все виды вместо иерархии: у операций мало полей,
 * они сильно пересекаются, а сериализовать и версионировать одну форму
 * заметно проще, чем пять. Незначащие для конкретного вида поля остаются
 * пустыми.
 */
@Serializable
data class SyncPayload(
    /** Значение [com.anilibrix.plus.domain.model.CollectionType]; пусто — статус снят. */
    @SerialName("status") val status: String = "",
    @SerialName("in_favorites") val inFavorites: Boolean = false,
    @SerialName("release_episode_id") val releaseEpisodeId: String = "",
    @SerialName("position_ms") val positionMs: Long = 0L,
    @SerialName("duration_ms") val durationMs: Long = 0L,
    @SerialName("episode") val episode: Int = 0,
    @SerialName("score") val score: Int = 0,
    @SerialName("shikimori_id") val shikimoriId: Int = 0,
)
