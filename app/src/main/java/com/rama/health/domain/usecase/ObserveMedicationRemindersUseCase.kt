package com.rama.health.domain.usecase

import com.rama.health.domain.model.MedicationReminder
import com.rama.health.domain.repository.MedicationReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMedicationRemindersUseCase @Inject constructor(
    private val repository: MedicationReminderRepository,
) {
    operator fun invoke(): Flow<List<MedicationReminder>> = repository.observeAll()
}
