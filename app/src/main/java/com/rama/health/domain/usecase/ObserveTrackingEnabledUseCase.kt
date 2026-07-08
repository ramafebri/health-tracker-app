package com.rama.health.domain.usecase

import com.rama.health.domain.repository.StepRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTrackingEnabledUseCase @Inject constructor(
    private val repository: StepRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeTrackingEnabled()
}
