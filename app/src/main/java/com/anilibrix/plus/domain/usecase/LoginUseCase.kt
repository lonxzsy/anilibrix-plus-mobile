package com.anilibrix.plus.domain.usecase

import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val anilibriaRepository: AnilibriaRepository
) {
    operator fun invoke(login: String, password: String): Flow<NetworkResult<String>> {
        return anilibriaRepository.login(login, password)
    }
}
