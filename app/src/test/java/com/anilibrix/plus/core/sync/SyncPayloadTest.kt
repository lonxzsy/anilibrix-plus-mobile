package com.anilibrix.plus.core.sync

import com.anilibrix.plus.domain.model.CollectionType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Очередь переживает обновление приложения.
 *
 * Записи лежат в базе, а разбирает их код следующей версии. Если форма
 * payload или имена видов операций изменятся несовместимо, накопленные офлайн
 * действия молча потеряются — поэтому и то, и другое зафиксировано тестом.
 */
class SyncPayloadTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `payload переживает сериализацию без потерь`() {
        val original = SyncPayload(
            status = CollectionType.WATCHING.value,
            inFavorites = true,
            releaseEpisodeId = "re-42",
            positionMs = 123_456L,
            durationMs = 1_440_000L,
            episode = 7,
            score = 9,
            shikimoriId = 1234,
        )
        val restored = json.decodeFromString(
            SyncPayload.serializer(),
            json.encodeToString(SyncPayload.serializer(), original),
        )
        assertEquals(original, restored)
    }

    @Test
    fun `запись из старой версии читается со значениями по умолчанию`() {
        // Так выглядит payload, записанный версией, где ещё не было полей
        // прогресса и оценки.
        val legacy = """{"status":"WATCHING"}"""
        val restored = json.decodeFromString(SyncPayload.serializer(), legacy)

        assertEquals(CollectionType.WATCHING.value, restored.status)
        assertEquals(0, restored.episode)
        assertEquals(0L, restored.positionMs)
    }

    @Test
    fun `неизвестные поля не ломают разбор`() {
        val future = """{"status":"DROPPED","something_new":true}"""
        assertEquals(
            CollectionType.DROPPED.value,
            json.decodeFromString(SyncPayload.serializer(), future).status,
        )
    }

    @Test
    fun `вид операции хранится строкой и читается обратно`() {
        SyncOperationKind.entries.forEach { kind ->
            assertEquals(kind, SyncOperationKind.fromValue(kind.name))
        }
    }

    @Test
    fun `вид операции из будущей версии не угадывается`() {
        assertNull(SyncOperationKind.fromValue("SOMETHING_NEW"))
        assertNull(SyncOperationKind.fromValue(""))
    }

    @Test
    fun `пустой статус означает снятие отметки`() {
        val payload = SyncPayload(status = "")
        assertNull(CollectionType.fromValue(payload.status))

        val set = SyncPayload(status = CollectionType.COMPLETED.value)
        assertNotNull(CollectionType.fromValue(set.status))
    }
}
