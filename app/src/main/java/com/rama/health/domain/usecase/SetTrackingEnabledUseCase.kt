package com.rama.health.domain.usecase

import com.rama.health.domain.repository.StepRepository
import javax.inject.Inject

class SetTrackingEnabledUseCase @Inject constructor(
    private val repository: StepRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setTrackingEnabled(enabled)
}
