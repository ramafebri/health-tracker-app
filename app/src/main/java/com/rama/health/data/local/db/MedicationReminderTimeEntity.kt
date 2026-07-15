package com.rama.health.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_reminder_times",
    foreignKeys = [
        ForeignKey(
            entity = MedicationReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("medicationId")],
)
data class MedicationReminderTimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: String,
    val hour: Int,
    val minute: Int,
)
