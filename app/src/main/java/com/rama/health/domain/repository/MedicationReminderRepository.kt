package com.rama.health.domain.repository

import com.rama.health.domain.model.MedicationReminder
import kotlinx.coroutines.flow.Flow

interface MedicationReminderRepository {
    fun observeAll(): Flow<List<MedicationReminder>>
    fun observeById(id: String): Flow<MedicationReminder?>
    suspend fun save(reminder: MedicationReminder)
    suspend fun delete(id: String)
    suspend fun toggleEnabled(id: String, enabled: Boolean)
}
