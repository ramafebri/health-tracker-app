package com.rama.health.domain.usecase

import com.rama.health.domain.model.MedicationReminder
import com.rama.health.domain.repository.MedicationReminderRepository
import com.rama.health.domain.repository.ReminderScheduler
import javax.inject.Inject

class SaveMedicationReminderUseCase @Inject constructor(
    private val repository: MedicationReminderRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(reminder: MedicationReminder) {
        repository.save(reminder)
        if (reminder.enabled) {
            scheduler.scheduleMedication(reminder)
        } else {
            scheduler.cancelMedication(reminder.id)
        }
    }
}
