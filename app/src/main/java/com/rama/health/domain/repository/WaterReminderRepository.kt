package com.rama.health.domain.repository

import com.rama.health.domain.model.WaterReminderSettings
import kotlinx.coroutines.flow.Flow

interface WaterReminderRepository {
    fun observeSettings(): Flow<WaterReminderSettings>
    suspend fun updateSettings(settings: WaterReminderSettings)
    suspend fun logIntake(amountMl: Int)
    suspend fun clearIntake()
}
