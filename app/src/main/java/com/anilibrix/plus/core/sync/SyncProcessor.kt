package com.anilibrix.plus.core.sync

import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Разгребает [SyncQueue].
 *
 * Вынесен из воркера отдельным классом, потому что запускается из трёх мест:
 * периодически, при появлении сети и по кнопке «Синхронизировать сейчас».
 * Логика при этом одна и та же.
 *
 * Операция, которая не удалась из-за сети, остаётся в очереди и повторится.
 * Операция, которая не удалась по существу (сервер отверг запрос), считается
 * выполненной: повторять её бессмысленно, а очередь она бы заблокировала.
 * Различает их код ответа: сетевые ошибки приходят исключением, отказ
 * сервера — [NetworkResult.Error].
 */
@Singleton
class SyncProcessor @Inject constructor(
    private val syncQueue: SyncQueue,
    private val anilibriaRepository: AnilibriaRepository,
    private val shikimoriSync: ShikimoriSyncHandler,
) {

    /** @return сколько операций удалось выполнить. */
    suspend fun drain(): Int {
        var done = 0
        val batch = syncQueue.peek()
        for (operation in batch) {
            val success = runCatching { execute(operation) }.getOrElse { false }
            if (success) {
                syncQueue.markDone(operation.id)
                done++
            } else {
                syncQueue.markFailed(operation.id)
            }
        }
        return done
    }

    private suspend fun execute(operation: PendingOperation): Boolean = when (operation.kind) {
        SyncOperationKind.COLLECTION_STATUS -> syncCollectionStatus(operation)
        SyncOperationKind.FAVORITE -> syncFavorite(operation)
        SyncOperationKind.TIMECODE_UPDATE -> syncTimecodeUpdate(operation)
        SyncOperationKind.TIMECODE_DELETE -> syncTimecodeDelete(operation)
        SyncOperationKind.SHIKIMORI_RATE -> shikimoriSync.push(operation)
    }

    private suspend fun syncCollectionStatus(operation: PendingOperation): Boolean {
        val status = CollectionType.fromValue(operation.payload.status)
        return if (status == null) {
            // Статус снят. У Anilibria нет операции «убрать из всех списков»,
            // поэтому удаляем из каждого: лишние запросы дешевле, чем
            // хранить локально копию серверного состояния ради одного удаления.
            CollectionType.entries.all { type ->
                anilibriaRepository.removeFromCollection(operation.titleId, type).succeeded()
            }
        } else {
            CollectionType.entries
                .filter { it != status }
                .forEach { anilibriaRepository.removeFromCollection(operation.titleId, it).first { r -> r !is NetworkResult.Loading } }
            anilibriaRepository.addToCollection(operation.titleId, status).succeeded()
        }
    }

    private suspend fun syncFavorite(operation: PendingOperation): Boolean {
        return if (operation.payload.inFavorites) {
            anilibriaRepository.addFavorite(operation.titleId).succeeded()
        } else {
            anilibriaRepository.removeFavorite(operation.titleId).succeeded()
        }
    }

    private suspend fun syncTimecodeUpdate(operation: PendingOperation): Boolean {
        val id = operation.payload.releaseEpisodeId
        if (id.isBlank()) return true
        return anilibriaRepository
            .updateTimecode(id, operation.payload.positionMs, operation.payload.durationMs)
            .succeeded()
    }

    private suspend fun syncTimecodeDelete(operation: PendingOperation): Boolean {
        val id = operation.payload.releaseEpisodeId
        // Записи, созданные до появления серверного ключа, удалять на сервере
        // нечем — считаем операцию выполненной, иначе она застрянет навсегда.
        if (id.isBlank()) return true
        return anilibriaRepository.deleteTimecode(id).succeeded()
    }
}

private suspend fun kotlinx.coroutines.flow.Flow<NetworkResult<Unit>>.succeeded(): Boolean =
    first { it !is NetworkResult.Loading } is NetworkResult.Success
