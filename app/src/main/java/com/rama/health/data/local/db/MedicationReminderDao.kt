package com.rama.health.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationReminderDao {
    @Transaction
    @Query("SELECT * FROM medication_reminders ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<MedicationReminderWithTimes>>

    @Transaction
    @Query("SELECT * FROM medication_reminders WHERE id = :id")
    fun observeById(id: String): Flow<MedicationReminderWithTimes?>

    @Upsert
    suspend fun upsertReminder(reminder: MedicationReminderEntity)

    @Insert
    suspend fun insertTimes(times: List<MedicationReminderTimeEntity>)

    @Query("DELETE FROM medication_reminder_times WHERE medicationId = :medicationId")
    suspend fun deleteTimesForMedication(medicationId: String)

    @Transaction
    suspend fun upsert(reminder: MedicationReminderEntity, times: List<MedicationReminderTimeEntity>) {
        upsertReminder(reminder)
        deleteTimesForMedication(reminder.id)
        if (times.isNotEmpty()) {
            insertTimes(times)
        }
    }

    @Query("DELETE FROM medication_reminders WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE medication_reminders SET enabled = :enabled WHERE id = :id")
    suspend fun toggleEnabled(id: String, enabled: Boolean)
}
