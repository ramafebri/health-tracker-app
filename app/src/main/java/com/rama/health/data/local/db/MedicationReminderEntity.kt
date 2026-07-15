package com.rama.health.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_reminders")
data class MedicationReminderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dosage: String?,
    val enabled: Boolean,
    val repeatDaysMask: Int,
    val createdAtMillis: Long,
)
