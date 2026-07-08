package com.anilibrix.plus.domain.usecase

import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.repository.LocalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetContinueWatchingUseCase @Inject constructor(
    private val localRepository: LocalRepository
) {
    operator fun invoke(): Flow<List<HistoryEntry>> {
        return localRepository.getHistory().map { history ->
            history
                .filter { entry ->
                    val progress = if (entry.duration > 0) entry.timestamp.toFloat() / entry.duration.toFloat() else 0f
                    progress > 0f && progress < 0.9f
                }
                .sortedByDescending { it.watchedAt }
                .take(6)
        }
    }
}
