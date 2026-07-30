package com.anilibrix.plus.domain.usecase

import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecommendedUseCase @Inject constructor(
    private val anilibriaRepository: AnilibriaRepository
) {
    operator fun invoke(limit: Int = 10, releaseId: Long? = null): Flow<NetworkResult<List<Title>>> {
        return anilibriaRepository.getRecommended(limit, releaseId)
    }
}
