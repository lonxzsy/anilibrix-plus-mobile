package com.anilibrix.plus.domain.usecase

import com.anilibrix.plus.domain.model.CatalogQuery
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCatalogUseCase @Inject constructor(
    private val anilibriaRepository: AnilibriaRepository
) {
    operator fun invoke(page: Int = 1, limit: Int = 30, search: String? = null): Flow<NetworkResult<List<Title>>> {
        return anilibriaRepository.getCatalog(page, limit, search)
    }

    operator fun invoke(query: CatalogQuery): Flow<NetworkResult<List<Title>>> {
        return anilibriaRepository.getCatalog(query)
    }
}
