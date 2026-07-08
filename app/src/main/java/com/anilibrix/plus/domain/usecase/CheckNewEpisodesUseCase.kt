package com.anilibrix.plus.domain.usecase

import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.domain.repository.LocalRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CheckNewEpisodesUseCase @Inject constructor(
    private val localRepository: LocalRepository,
    private val anilibriaRepository: AnilibriaRepository,
    private val settingsDataStore: SettingsDataStore
) {

    suspend operator fun invoke(): List<NewEpisodeNotification> {
        if (!settingsDataStore.notificationsNewEpisodesEnabled.first()) return emptyList()

        val favorites = localRepository.getFavorites().first()
        if (favorites.isEmpty()) {
            settingsDataStore.setNotificationsLastEpisodeSnapshot(null)
            return emptyList()
        }

        val oldSnapshot = parseSnapshot(settingsDataStore.notificationsLastEpisodeSnapshot.first())
        val currentSnapshot = mutableMapOf<Long, Int>()
        val notifications = mutableListOf<NewEpisodeNotification>()

        favorites.forEach { favorite ->
            val title = when (val result = anilibriaRepository.getRelease(favorite.titleId.toString()).first { it !is NetworkResult.Loading }) {
                is NetworkResult.Success -> result.data
                else -> null
            } ?: return@forEach

            val currentEpisode = title.currentEpisodeNumber()
            currentSnapshot[title.id] = currentEpisode

            val previousEpisode = oldSnapshot[title.id]
            if (previousEpisode != null && currentEpisode > previousEpisode) {
                notifications += NewEpisodeNotification(
                    titleId = title.id,
                    titleName = title.name.main,
                    episodeNumber = currentEpisode,
                    posterUrl = title.poster?.medium ?: title.poster?.small
                )
            }
        }

        settingsDataStore.setNotificationsLastEpisodeSnapshot(formatSnapshot(currentSnapshot))
        return notifications
    }

    private fun Title.currentEpisodeNumber(): Int {
        return episodes.orEmpty().maxOfOrNull { it.ordinal } ?: episodesTotal
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
}

data class NewEpisodeNotification(
    val titleId: Long,
    val titleName: String,
    val episodeNumber: Int,
    val posterUrl: String?
)
