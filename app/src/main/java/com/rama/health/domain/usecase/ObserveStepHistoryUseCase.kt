package com.rama.health.domain.usecase

import com.rama.health.domain.model.DailyStepRecord
import com.rama.health.domain.repository.StepRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveStepHistoryUseCase @Inject constructor(
    private val repository: StepRepository,
) {
    operator fun invoke(): Flow<List<DailyStepRecord>> = repository.observeHistory()
}
