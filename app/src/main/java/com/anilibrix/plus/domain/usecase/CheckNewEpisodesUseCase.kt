package com.anilibrix.plus.domain.usecase

import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.domain.repository.LocalRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

/**
 * Ищет вышедшие серии по тому, что человек отслеживает.
 *
 * Прежняя версия делала по одному запросу `getRelease` на **каждый** тайтл в
 * избранном, последовательно, каждые шесть часов. На тридцати тайтлах это
 * тридцать сетевых запросов в фоне — заметно и по батарее, и по трафику, и
 * первые из них успевали устареть, пока шли последние.
 *
 * Теперь основной источник — недельное расписание: **один** запрос,
 * покрывающий все выходящие сегодня релизы. Точечный `getRelease` остаётся
 * только для тайтлов, которых в расписании нет (например, вышедших вне
 * графика), и только для отслеживаемых, а не для всего избранного.
 */
class CheckNewEpisodesUseCase @Inject constructor(
    private val localRepository: LocalRepository,
    private val anilibriaRepository: AnilibriaRepository,
    private val settingsDataStore: SettingsDataStore
) {

    suspend operator fun invoke(): List<NewEpisodeNotification> {
        if (!settingsDataStore.notificationsNewEpisodesEnabled.first()) return emptyList()
        if (settingsDataStore.notificationsQuietHours.first() && isQuietHour()) return emptyList()

        val tracked = trackedTitleIds()
        if (tracked.isEmpty()) {
            settingsDataStore.setNotificationsLastEpisodeSnapshot(null)
            return emptyList()
        }

        val oldSnapshot = parseSnapshot(settingsDataStore.notificationsLastEpisodeSnapshot.first())
        val currentSnapshot = oldSnapshot.toMutableMap()
        val notifications = mutableListOf<NewEpisodeNotification>()

        val scheduleResult = anilibriaRepository.getSchedule().first { it !is NetworkResult.Loading }
        val scheduled = (scheduleResult as? NetworkResult.Success)
            ?.data
            ?.flatMap { day -> day.entries }
            ?.filter { it.title.id in tracked }
            .orEmpty()

        for (entry in scheduled) {
            val title = entry.title
            val current = entry.publishedEpisode ?: title.currentEpisodeNumber()
            val previous = currentSnapshot[title.id]
            currentSnapshot[title.id] = current

            if (previous != null && current > previous) {
                notifications += title.toNotification(current, episodeId = null)
            }
        }

        // Тайтлы, которых в расписании нет, — уточняем точечно. Их обычно
        // единицы, поэтому цена запросов несопоставима с прежней схемой.
        val unresolved = tracked - scheduled.map { it.title.id }.toSet()
        for (titleId in unresolved) {
            val result = anilibriaRepository.getRelease(titleId.toString())
                .first { it !is NetworkResult.Loading }
            val title = (result as? NetworkResult.Success)?.data ?: continue

            val current = title.currentEpisodeNumber()
            val previous = currentSnapshot[title.id]
            currentSnapshot[title.id] = current

            if (previous != null && current > previous) {
                // Здесь список серий уже загружен, поэтому уведомление ведёт
                // прямо в нужную серию, а не просто на страницу тайтла.
                val episodeId = title.episodes.orEmpty()
                    .firstOrNull { it.ordinal == current }
                    ?.id
                notifications += title.toNotification(current, episodeId)
            }
        }

        settingsDataStore.setNotificationsLastEpisodeSnapshot(formatSnapshot(currentSnapshot))
        return notifications
    }

    /**
     * Что считать отслеживаемым.
     *
     * Избранное плюс активные статусы. «Просмотрено» и «Брошено» исключены:
     * присылать уведомление о новой серии брошенного тайтла — ровно тот спам,
     * из-за которого уведомления и отключают целиком.
     */
    private suspend fun trackedTitleIds(): Set<Long> {
        val favorites = localRepository.getFavorites().first().map { it.titleId }
        val watching = localRepository.getCollections(CollectionType.WATCHING).first().map { it.titleId }
        val planned = localRepository.getCollections(CollectionType.WATCH_LATER).first().map { it.titleId }
        val onHold = localRepository.getCollections(CollectionType.ON_HOLD).first().map { it.titleId }
        return (favorites + watching + planned + onHold).toSet()
    }

    private fun Title.toNotification(episodeNumber: Int, episodeId: Long?) = NewEpisodeNotification(
        titleId = id,
        titleName = name.main,
        episodeNumber = episodeNumber,
        posterUrl = poster?.cardUrl,
        episodeId = episodeId,
    )

    private fun Title.currentEpisodeNumber(): Int {
        return episodes.orEmpty().maxOfOrNull { it.ordinal } ?: episodesTotal
    }

    /** Ночью не будим: с 23:00 до 08:00 уведомления копятся до утра. */
    private fun isQuietHour(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= QUIET_FROM_HOUR || hour < QUIET_TO_HOUR
    }

    private fun parseSnapshot(snapshot: String?): Map<Long, Int> {
        if (snapshot.isNullOrBlank()) return emptyMap()
        return snapshot
            .split('|')
            .mapNotNull { item ->
                val parts = item.split(':')
                val titleId = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
                val episode = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                titleId to episode
            }
            .toMap()
    }

    private fun formatSnapshot(snapshot: Map<Long, Int>): String {
        return snapshot.entries.joinToString("|") { (titleId, episode) -> "$titleId:$episode" }
    }

    private companion object {
        const val QUIET_FROM_HOUR = 23
        const val QUIET_TO_HOUR = 8
    }
}

data class NewEpisodeNotification(
    val titleId: Long,
    val titleName: String,
    val episodeNumber: Int,
    val posterUrl: String?,
    /** `null`, если конкретную серию определить не удалось — тогда открываем тайтл. */
    val episodeId: Long? = null,
)
