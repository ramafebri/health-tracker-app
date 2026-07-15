package com.rama.health.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rama.health.data.local.db.AppDatabase
import com.rama.health.domain.model.MedicationReminder
import com.rama.health.domain.model.MedicationTime
import com.rama.health.domain.model.RepeatDays
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MedicationReminderRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: MedicationReminderRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = MedicationReminderRepositoryImpl(database.medicationReminderDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun save_upsertReplacesTimes() = runTest {
        val reminder = sampleReminder(
            times = listOf(MedicationTime(8, 0), MedicationTime(20, 0)),
        )

        repository.save(reminder)
        repository.save(
            reminder.copy(times = listOf(MedicationTime(12, 30))),
        )

        val saved = repository.observeById(reminder.id).first()
        assertEquals(listOf(MedicationTime(12, 30)), saved?.times)
    }

    @Test
    fun toggleEnabled_updatesEnabledFlag() = runTest {
        val reminder = sampleReminder(times = listOf(MedicationTime(8, 0)))
        repository.save(reminder)

        repository.toggleEnabled(reminder.id, enabled = false)

        val saved = repository.observeById(reminder.id).first()
        assertFalse(saved?.enabled == true)
    }

    @Test
    fun observeAll_mapsDomainRemindersWithSortedTimes() = runTest {
        val reminder = sampleReminder(
            times = listOf(MedicationTime(20, 0), MedicationTime(8, 0)),
        )
        repository.save(reminder)

        val all = repository.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(listOf(MedicationTime(8, 0), MedicationTime(20, 0)), all.first().times)
    }

    private fun sampleReminder(
        id: String = "med-1",
        times: List<MedicationTime>,
    ) = MedicationReminder(
        id = id,
        name = "Vitamin D",
        dosage = "1000 IU",
        enabled = true,
        repeatDays = RepeatDays.allDays(),
        times = times,
        createdAtMillis = 1_000L,
    )
}
