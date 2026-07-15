package com.rama.health.domain.repository

import com.rama.health.domain.model.MedicationReminder
import com.rama.health.domain.model.WaterReminderSettings

interface ReminderScheduler {
    suspend fun scheduleWater(settings: WaterReminderSettings)
    suspend fun cancelWater()
    suspend fun scheduleMedication(reminder: MedicationReminder)
    suspend fun cancelMedication(medicationId: String)
    suspend fun cancelAll()
    suspend fun rescheduleAll(
        waterSettings: WaterReminderSettings,
        medications: List<MedicationReminder>,
    )
}
