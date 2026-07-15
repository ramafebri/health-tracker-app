package com.rama.health.domain.usecase

import com.rama.health.domain.model.WaterReminderSettings
import com.rama.health.domain.repository.ReminderScheduler
import com.rama.health.domain.repository.WaterReminderRepository
import javax.inject.Inject

class UpdateWaterReminderSettingsUseCase @Inject constructor(
    private val repository: WaterReminderRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(settings: WaterReminderSettings) {
        repository.updateSettings(settings)
        if (settings.enabled) {
            scheduler.scheduleWater(settings)
        } else {
            scheduler.cancelWater()
        }
    }
}
