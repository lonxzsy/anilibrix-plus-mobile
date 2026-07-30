package com.anilibrix.plus.core.sync

import com.anilibrix.plus.core.database.dao.SyncOperationDao
import com.anilibrix.plus.core.database.entity.SyncOperationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Журнал действий, которые ещё не доехали до сервера.
 *
 * Раньше приложение работало по схеме «сходили в сеть — и будь что будет»:
 * без сети действие просто не сохранялось нигде, кроме локальной базы, и
 * молча расходилось с сервером навсегда. Синхронизация при этом была
 * односторонней и запускалась только при открытии вкладки «Профиль».
 *
 * Теперь порядок обратный: сначала пишем локально (интерфейс отвечает
 * мгновенно и работает офлайн), потом кладём запись сюда, а [SyncWorker]
 * разгребает очередь, когда появляется сеть.
 */
@Singleton
class SyncQueue @Inject constructor(
    private val dao: SyncOperationDao,
    private val json: Json,
) {

    val pendingCount: Flow<Int> = dao.countFlow()

    /**
     * Ставит операцию в очередь, вытесняя предыдущие того же вида по тому же
     * тайтлу.
     *
     * Вытеснение существенно: если статус переключили три раза подряд, до
     * сервера должно доехать только последнее значение. Иначе в трекере
     * промелькнут состояния, которых человек не выбирал, а на слабой сети
     * очередь будет разгребаться дольше, чем пользователь тыкает.
     */
    suspend fun enqueue(kind: SyncOperationKind, titleId: Long, payload: SyncPayload = SyncPayload()) {
        dao.deleteSupersededBy(kind.name, titleId)
        dao.insert(
            SyncOperationEntity(
                kind = kind.name,
                titleId = titleId,
                payload = json.encodeToString(SyncPayload.serializer(), payload),
            )
        )
    }

    suspend fun peek(limit: Int = BATCH_SIZE): List<PendingOperation> {
        dao.dropExhausted(MAX_ATTEMPTS)
        return dao.peek(limit).mapNotNull { entity ->
            val kind = SyncOperationKind.fromValue(entity.kind) ?: run {
                // Запись из будущей или уже удалённой версии — выполнить её
                // нечем, держать в очереди бессмысленно.
                dao.delete(entity.id)
                return@mapNotNull null
            }
            val payload = runCatching {
                json.decodeFromString(SyncPayload.serializer(), entity.payload)
            }.getOrElse {
                dao.delete(entity.id)
                return@mapNotNull null
            }
            PendingOperation(entity.id, kind, entity.titleId, payload, entity.attempts)
        }
    }

    suspend fun markDone(id: Long) = dao.delete(id)

    suspend fun markFailed(id: Long) = dao.incrementAttempts(id)

    suspend fun clear() = dao.deleteAll()

    companion object {
        const val BATCH_SIZE = 50

        /**
         * Отравленная операция (тайтл удалён на сервере, доступ отозван) не
         * должна блокировать очередь вечно — после трёх попыток она
         * отбрасывается.
         */
        const val MAX_ATTEMPTS = 3
    }
}

data class PendingOperation(
    val id: Long,
    val kind: SyncOperationKind,
    val titleId: Long,
    val payload: SyncPayload,
    val attempts: Int,
)
