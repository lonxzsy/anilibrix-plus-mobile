package com.anilibrix.plus.domain.usecase

import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.repository.LocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHistoryUseCase @Inject constructor(
    private val localRepository: LocalRepository
) {
    operator fun invoke(): Flow<List<HistoryEntry>> {
        return localRepository.getHistory()
    }
}
