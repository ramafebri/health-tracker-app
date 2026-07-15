package com.rama.health.domain.usecase

import com.rama.health.domain.model.WaterReminderSettings
import com.rama.health.domain.repository.WaterReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWaterReminderSettingsUseCase @Inject constructor(
    private val repository: WaterReminderRepository,
) {
    operator fun invoke(): Flow<WaterReminderSettings> = repository.observeSettings()
}
