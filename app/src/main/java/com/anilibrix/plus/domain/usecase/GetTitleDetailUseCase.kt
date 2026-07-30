package com.anilibrix.plus.domain.usecase

import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTitleDetailUseCase @Inject constructor(
    private val anilibriaRepository: AnilibriaRepository
) {
    operator fun invoke(idOrAlias: String): Flow<NetworkResult<Title>> {
        return anilibriaRepository.getRelease(idOrAlias)
    }
}
