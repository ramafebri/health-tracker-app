package com.rama.health.data.repository

import com.rama.health.data.local.db.MedicationReminderDao
import com.rama.health.data.local.db.MedicationReminderEntity
import com.rama.health.data.local.db.MedicationReminderTimeEntity
import com.rama.health.data.local.db.MedicationReminderWithTimes
import com.rama.health.domain.model.MedicationReminder
import com.rama.health.domain.model.MedicationTime
import com.rama.health.domain.model.RepeatDays
import com.rama.health.domain.repository.MedicationReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationReminderRepositoryImpl @Inject constructor(
    private val dao: MedicationReminderDao,
) : MedicationReminderRepository {

    override fun observeAll(): Flow<List<MedicationReminder>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeById(id: String): Flow<MedicationReminder?> =
        dao.observeById(id).map { row -> row?.toDomain() }

    override suspend fun save(reminder: MedicationReminder) {
        dao.upsert(reminder.toEntity(), reminder.toTimeEntities())
    }

    override suspend fun delete(id: String) = dao.delete(id)

    override suspend fun toggleEnabled(id: String, enabled: Boolean) = dao.toggleEnabled(id, enabled)

    private fun MedicationReminderWithTimes.toDomain(): MedicationReminder = MedicationReminder(
        id = reminder.id,
        name = reminder.name,
        dosage = reminder.dosage,
        enabled = reminder.enabled,
        repeatDays = RepeatDays(reminder.repeatDaysMask),
        times = times
            .map { MedicationTime(hour = it.hour, minute = it.minute) }
            .sortedWith(compareBy({ it.hour }, { it.minute })),
        createdAtMillis = reminder.createdAtMillis,
    )

    private fun MedicationReminder.toEntity(): MedicationReminderEntity = MedicationReminderEntity(
        id = id,
        name = name,
        dosage = dosage,
        enabled = enabled,
        repeatDaysMask = repeatDays.mask,
        createdAtMillis = createdAtMillis,
    )

    private fun MedicationReminder.toTimeEntities(): List<MedicationReminderTimeEntity> =
        times.map { time ->
            MedicationReminderTimeEntity(
                medicationId = id,
                hour = time.hour,
                minute = time.minute,
            )
        }
}
