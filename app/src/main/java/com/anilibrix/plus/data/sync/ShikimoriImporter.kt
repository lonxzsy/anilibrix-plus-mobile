package com.anilibrix.plus.data.sync

import com.anilibrix.plus.core.database.dao.CollectionDao
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.data.remote.api.ShikimoriApi
import com.anilibrix.plus.domain.model.CatalogQuery
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.domain.repository.LocalRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Импорт списков с Shikimori.
 *
 * Правила разрешения конфликтов заданы явно и одинаково для всех записей:
 *
 *  - если тайтла у нас нет — импортируем;
 *  - если есть и локальная запись изменена **позже** времени с Shikimori —
 *    оставляем локальную. Иначе синхронизация отменяла бы только что
 *    сделанный выбор;
 *  - прогресс никогда не уменьшается. Откат номера серии назад человек
 *    воспринимает как потерю данных, даже если формально «сервер новее».
 */
@Singleton
class ShikimoriImporter @Inject constructor(
    private val api: ShikimoriApi,
    private val settings: SettingsDataStore,
    private val collectionDao: CollectionDao,
    private val localRepository: LocalRepository,
    private val anilibriaRepository: AnilibriaRepository,
) {

    /** Сколько записей есть в Shikimori — показывается перед импортом. */
    suspend fun preview(): ImportPreview {
        val userId = settings.shikimoriUserId.first() ?: return ImportPreview()
        val rates = runCatching { api.getUserRates(userId) }.getOrNull().orEmpty()
        return ImportPreview(total = rates.size)
    }

    suspend fun import(): ImportResult {
        val userId = settings.shikimoriUserId.first() ?: return ImportResult()
        val rates = runCatching { api.getUserRates(userId) }.getOrNull() ?: return ImportResult()

        var imported = 0
        var updated = 0
        var skipped = 0

        for (rate in rates) {
            val status = ShikimoriStatusMapper.fromShikimori(rate.status) ?: continue

            val existing = collectionDao.getAllOnce().firstOrNull { it.shikimoriId == rate.targetId }
            if (existing != null) {
                val remoteAt = rate.updatedAt.toEpochMillisOrZero()
                if (existing.updatedAt > remoteAt) {
                    // Локальное изменение новее — не трогаем.
                    skipped++
                    continue
                }
                localRepository.setCollectionType(existing.titleId, status)
                collectionDao.updateProgress(
                    titleId = existing.titleId,
                    // Прогресс только вперёд.
                    episode = maxOf(existing.progressEpisode, rate.episodes),
                    updatedAt = System.currentTimeMillis(),
                )
                if (rate.score > 0) {
                    localRepository.setRating(
                        existing.titleId,
                        ShikimoriStatusMapper.scoreFromShikimori(rate.score),
                    )
                }
                updated++
                continue
            }

            // Тайтла у нас нет — пробуем найти его в каталоге Anilibria по
            // названию с Shikimori.
            val anime = runCatching { api.getAnime(rate.targetId) }.getOrNull() ?: continue
            val query = anime.russian?.takeIf { it.isNotBlank() } ?: anime.name
            if (query.isBlank()) continue

            val catalog = anilibriaRepository
                .getCatalog(CatalogQuery(page = 1, limit = 5, search = query))
                .first { it !is NetworkResult.Loading }
            val candidates = (catalog as? NetworkResult.Success)?.data.orEmpty()

            val normalized = query.normalizeForMatch()
            val match = candidates.firstOrNull { title ->
                listOfNotNull(title.name.main, title.name.english, title.name.alternative)
                    .any { it.normalizeForMatch() == normalized }
            }
            if (match == null) {
                // Только точное совпадение: промах здесь испортил бы список
                // молча, а исправлять его человеку пришлось бы вручную.
                skipped++
                continue
            }

            localRepository.setCollectionType(
                titleId = match.id,
                collectionType = status,
                titleName = match.name.main,
                posterUrl = match.poster?.cardUrl,
            )
            collectionDao.updateShikimoriId(match.id, rate.targetId)
            collectionDao.updateProgress(match.id, rate.episodes, System.currentTimeMillis())
            if (rate.score > 0) {
                localRepository.setRating(match.id, ShikimoriStatusMapper.scoreFromShikimori(rate.score))
            }
            imported++
        }

        settings.setShikimoriLastSync(System.currentTimeMillis())
        return ImportResult(imported = imported, updated = updated, skipped = skipped)
    }
}

data class ImportPreview(val total: Int = 0)

data class ImportResult(
    val imported: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
) {
    fun summary(): String = buildList {
        if (imported > 0) add("добавлено $imported")
        if (updated > 0) add("обновлено $updated")
        if (skipped > 0) add("пропущено $skipped")
    }.joinToString(", ").ifEmpty { "изменений нет" }
}

/** Shikimori отдаёт ISO-8601; неразобранная дата не должна выигрывать конфликт. */
private fun String?.toEpochMillisOrZero(): Long {
    if (this.isNullOrBlank()) return 0L
    return runCatching { java.time.Instant.parse(this).toEpochMilli() }.getOrDefault(0L)
}

private fun String.normalizeForMatch(): String = lowercase().filter { it.isLetterOrDigit() }
