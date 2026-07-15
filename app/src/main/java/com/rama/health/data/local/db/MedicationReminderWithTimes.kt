package com.rama.health.data.local.db

import androidx.room.Embedded
import androidx.room.Relation

data class MedicationReminderWithTimes(
    @Embedded val reminder: MedicationReminderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "medicationId",
    )
    val times: List<MedicationReminderTimeEntity>,
)
