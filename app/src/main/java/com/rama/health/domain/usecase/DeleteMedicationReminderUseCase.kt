package com.rama.health.domain.usecase

import com.rama.health.domain.repository.MedicationReminderRepository
import com.rama.health.domain.repository.ReminderScheduler
import javax.inject.Inject

class DeleteMedicationReminderUseCase @Inject constructor(
    private val repository: MedicationReminderRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(id: String) {
        repository.delete(id)
        scheduler.cancelMedication(id)
    }
}
