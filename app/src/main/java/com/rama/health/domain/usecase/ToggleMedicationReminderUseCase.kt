package com.rama.health.domain.usecase

import com.rama.health.domain.repository.MedicationReminderRepository
import com.rama.health.domain.repository.ReminderScheduler
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class ToggleMedicationReminderUseCase @Inject constructor(
    private val repository: MedicationReminderRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(id: String, enabled: Boolean) {
        repository.toggleEnabled(id, enabled)
        if (enabled) {
            val reminder = repository.observeById(id).first() ?: return
            scheduler.scheduleMedication(reminder.copy(enabled = true))
        } else {
            scheduler.cancelMedication(id)
        }
    }
}
