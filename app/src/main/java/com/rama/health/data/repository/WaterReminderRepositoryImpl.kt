package com.rama.health.data.repository

import com.rama.health.data.local.datastore.WaterReminderPreferencesDataSource
import com.rama.health.domain.model.WaterReminderSettings
import com.rama.health.domain.repository.WaterReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterReminderRepositoryImpl @Inject constructor(
    private val prefs: WaterReminderPreferencesDataSource,
) : WaterReminderRepository {

    override fun observeSettings(): Flow<WaterReminderSettings> = prefs.settings

    override suspend fun updateSettings(settings: WaterReminderSettings) = prefs.updateSettings(settings)

    override suspend fun logIntake(amountMl: Int) = prefs.logIntake(amountMl)

    override suspend fun clearIntake() = prefs.clearIntake()
}
