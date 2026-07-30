package com.anilibrix.plus.domain.usecase

import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.ScheduleDay
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetScheduleUseCase @Inject constructor(
    private val anilibriaRepository: AnilibriaRepository
) {
    operator fun invoke(): Flow<NetworkResult<List<ScheduleDay>>> {
        return anilibriaRepository.getSchedule()
    }
}
