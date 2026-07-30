package com.anilibrix.plus.domain.usecase

import com.anilibrix.plus.core.sync.SyncOperationKind
import com.anilibrix.plus.core.sync.SyncPayload
import com.anilibrix.plus.core.sync.SyncQueue
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.FavoriteTitle
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.domain.repository.LocalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Единственная точка изменения статуса тайтла.
 *
 * Прежняя версия этого класса не вызывалась ниоткуда: из пяти статусов наружу
 * был выведен только «Буду смотреть», и трекер, полностью готовый и в базе, и
 * на сервере, просто не был подключён к интерфейсу.
 *
 * Заодно исправлены две структурные проблемы прежней реализации:
 *
 *  - она заводила собственный `CoroutineScope(Dispatchers.IO)`, который никто
 *    никогда не отменял, и запускала в нём работу из обычных функций. Теперь
 *    функции `suspend`, а областью владеет вызывающий — операция отменяется
 *    вместе с экраном, а не переживает его;
 *  - она писала на сервер немедленно и теряла действие, если сети не было.
 *    Теперь запись идёт локально, а до сервера её доносит [SyncQueue].
 */
class ManageCollectionsUseCase @Inject constructor(
    private val localRepository: LocalRepository,
    private val anilibriaRepository: AnilibriaRepository,
    private val syncQueue: SyncQueue,
) {

    fun getCollections(collectionType: CollectionType): Flow<List<FavoriteTitle>> =
        localRepository.getCollections(collectionType)

    fun getCounts(): Flow<Map<CollectionType, Int>> = localRepository.getCollectionCounts()

    fun observeStatus(titleId: Long): Flow<CollectionType?> =
        localRepository.observeCollectionType(titleId)

    suspend fun getStatus(titleId: Long): CollectionType? =
        localRepository.getCollectionType(titleId)

    /**
     * Ставит статус. Возвращает то, что должен показать интерфейс, сразу —
     * не дожидаясь сети.
     */
    suspend fun setStatus(
        titleId: Long,
        collectionType: CollectionType,
        titleName: String = "",
        posterUrl: String? = null,
    ) {
        localRepository.setCollectionType(titleId, collectionType, titleName, posterUrl)
        syncQueue.enqueue(
            kind = SyncOperationKind.COLLECTION_STATUS,
            titleId = titleId,
            payload = SyncPayload(status = collectionType.value),
        )
        syncQueue.enqueue(
            kind = SyncOperationKind.SHIKIMORI_RATE,
            titleId = titleId,
            payload = SyncPayload(status = collectionType.value),
        )
    }

    /** Убирает тайтл из всех списков. Пустой статус в payload означает снятие. */
    suspend fun clearStatus(titleId: Long) {
        localRepository.removeFromCollections(titleId)
        syncQueue.enqueue(SyncOperationKind.COLLECTION_STATUS, titleId, SyncPayload(status = ""))
        syncQueue.enqueue(SyncOperationKind.SHIKIMORI_RATE, titleId, SyncPayload(status = ""))
    }

    suspend fun toggleStatus(
        titleId: Long,
        collectionType: CollectionType,
        titleName: String = "",
        posterUrl: String? = null,
    ) {
        if (localRepository.getCollectionType(titleId) == collectionType) {
            clearStatus(titleId)
        } else {
            setStatus(titleId, collectionType, titleName, posterUrl)
        }
    }

    suspend fun isInCollection(titleId: Long, collectionType: CollectionType): Boolean =
        localRepository.isInCollection(titleId, collectionType)

    suspend fun getCollectionTypesForTitle(titleId: Long): List<CollectionType> =
        localRepository.getCollectionTypesForTitle(titleId)

    /**
     * Забирает коллекции с сервера.
     *
     * Локальные записи, изменённые позже серверных, **не** перетираются: у
     * серверного ответа нет времени изменения, поэтому единственный честный
     * ориентир — наш `updatedAt`. Действие, которое ещё лежит в очереди,
     * новее по определению, и затирать его ответом сервера значило бы отменять
     * то, что человек только что сделал.
     */
    suspend fun syncFromApi(token: String?): SyncSummary {
        if (token.isNullOrBlank()) return SyncSummary()

        val idsResult = anilibriaRepository.getCollectionIds().first { it !is NetworkResult.Loading }
        if (idsResult !is NetworkResult.Success) return SyncSummary()

        var imported = 0
        val byType = idsResult.data.groupBy { it.collectionType }
        for ((collectionType, _) in byType) {
            val releases = anilibriaRepository
                .getCollectionReleases(collectionType)
                .first { it !is NetworkResult.Loading }
            if (releases !is NetworkResult.Success) continue

            for (title in releases.data) {
                if (localRepository.getCollectionType(title.id) != null) continue
                localRepository.setCollectionType(
                    titleId = title.id,
                    collectionType = collectionType,
                    titleName = title.name.main,
                    posterUrl = title.poster?.cardUrl,
                )
                imported++
            }
        }
        return SyncSummary(imported = imported)
    }
}

data class SyncSummary(val imported: Int = 0)
