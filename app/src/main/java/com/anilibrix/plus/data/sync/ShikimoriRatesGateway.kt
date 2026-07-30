package com.anilibrix.plus.data.sync

import com.anilibrix.plus.core.database.dao.CollectionDao
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.core.sync.PendingOperation
import com.anilibrix.plus.data.remote.api.ShikimoriApi
import com.anilibrix.plus.data.remote.dto.UserRateBody
import com.anilibrix.plus.data.remote.dto.UserRateRequest
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.repository.LocalRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Отправка статуса, прогресса и оценки в списки Shikimori.
 *
 * Связь «релиз Anilibria ↔ аниме Shikimori» ищется поиском по названию и
 * запоминается в `collections.shikimoriId`. Поиск делается один раз на тайтл:
 * на каждой отправке ходить за ним заново — лишний запрос к чужому API с
 * жёстким лимитом (5 rps).
 */
@Singleton
class ShikimoriRatesGateway @Inject constructor(
    private val api: ShikimoriApi,
    private val settings: SettingsDataStore,
    private val collectionDao: CollectionDao,
    private val localRepository: LocalRepository,
) {

    /** @return `true`, если операция закрыта и повторять её не нужно. */
    suspend fun push(operation: PendingOperation): Boolean {
        val userId = settings.shikimoriUserId.first() ?: return true

        val entity = collectionDao.getForTitle(operation.titleId)
        val status = CollectionType.fromValue(operation.payload.status)

        // Статус снят: если запись на Shikimori была — удаляем её там же.
        if (status == null) {
            val rateId = findRateId(userId, entity?.shikimoriId ?: return true) ?: return true
            runCatching { api.deleteRate(rateId) }.getOrElse { return false }
            return true
        }
        if (entity == null) return true

        val shikimoriId = entity.shikimoriId
            ?: resolveShikimoriId(operation.titleId, entity.titleName)
            // Соответствие не нашлось (нет на Shikimori, другое название) —
            // повторять поиск на каждой синхронизации бессмысленно.
            ?: return true

        val score = if (settings.shikimoriPushRatings.first()) {
            localRepository.getRating(operation.titleId)
                ?.let { ShikimoriStatusMapper.scoreToShikimori(it) }
        } else {
            null
        }

        val body = UserRateBody(
            userId = userId,
            targetId = shikimoriId,
            targetType = "Anime",
            status = ShikimoriStatusMapper.toShikimori(status),
            episodes = entity.progressEpisode.takeIf { it > 0 },
            score = score,
        )

        return runCatching {
            val existing = findRateId(userId, shikimoriId)
            if (existing != null) {
                api.updateRate(existing, UserRateRequest(body))
            } else {
                api.createRate(UserRateRequest(body))
            }
            true
        }.getOrElse { false }
    }

    private suspend fun findRateId(userId: Int, shikimoriId: Int): Long? {
        return runCatching {
            api.getUserRates(userId = userId)
                .firstOrNull { it.targetId == shikimoriId }
                ?.id
        }.getOrNull()
    }

    /**
     * Ищет аниме на Shikimori по названию и запоминает найденное.
     *
     * Совпадение принимается только точное по нормализованному названию:
     * у франшиз названия отличаются одним словом, и «почти совпало» регулярно
     * означает не тот сезон. Лучше не связать вовсе, чем связать неверно и
     * молча испортить чужой список.
     */
    private suspend fun resolveShikimoriId(titleId: Long, titleName: String): Int? {
        if (titleName.isBlank()) return null
        val candidates = runCatching { api.searchAnime(titleName, limit = 5) }.getOrNull().orEmpty()
        val normalized = titleName.normalizeForMatch()
        val match = candidates.firstOrNull {
            it.name.normalizeForMatch() == normalized || it.russian?.normalizeForMatch() == normalized
        } ?: return null

        collectionDao.updateShikimoriId(titleId, match.id)
        return match.id
    }
}

private fun String.normalizeForMatch(): String =
    lowercase().filter { it.isLetterOrDigit() }
