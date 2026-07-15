package com.rama.health.domain.usecase

import com.rama.health.domain.repository.WaterReminderRepository
import javax.inject.Inject

class LogWaterIntakeUseCase @Inject constructor(
    private val repository: WaterReminderRepository,
) {
    suspend operator fun invoke(amountMl: Int = DEFAULT_LOG_AMOUNT_ML) {
        repository.logIntake(amountMl)
    }

    companion object {
        const val DEFAULT_LOG_AMOUNT_ML = 250
    }
}
