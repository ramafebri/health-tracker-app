package com.rama.health.domain.usecase

import com.rama.health.domain.repository.MedicationReminderRepository
import com.rama.health.domain.repository.ReminderScheduler
import com.rama.health.domain.repository.WaterReminderRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class RescheduleAllRemindersUseCase @Inject constructor(
    private val waterReminderRepository: WaterReminderRepository,
    private val medicationReminderRepository: MedicationReminderRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend operator fun invoke() {
        val waterSettings = waterReminderRepository.observeSettings().first()
        val medications = medicationReminderRepository.observeAll().first()
        reminderScheduler.rescheduleAll(
            waterSettings = waterSettings,
            medications = medications,
        )
    }
}
