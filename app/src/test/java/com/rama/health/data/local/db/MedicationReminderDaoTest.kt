package com.rama.health.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MedicationReminderDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: MedicationReminderDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.medicationReminderDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertThenObserveById_returnsReminderWithTimes() = runTest {
        val reminder = sampleReminder(id = "med-1")
        val times = listOf(
            MedicationReminderTimeEntity(medicationId = "med-1", hour = 8, minute = 0),
            MedicationReminderTimeEntity(medicationId = "med-1", hour = 20, minute = 0),
        )

        dao.upsert(reminder, times)

        val result = dao.observeById("med-1").first()
        assertEquals(reminder, result?.reminder)
        assertEquals(listOf(8 to 0, 20 to 0), result?.times?.map { it.hour to it.minute })
    }

    @Test
    fun upsertTwice_replacesTimesRatherThanAppending() = runTest {
        val reminder = sampleReminder(id = "med-1")
        dao.upsert(
            reminder,
            listOf(MedicationReminderTimeEntity(medicationId = "med-1", hour = 8, minute = 0)),
        )
        dao.upsert(
            reminder,
            listOf(
                MedicationReminderTimeEntity(medicationId = "med-1", hour = 12, minute = 30),
                MedicationReminderTimeEntity(medicationId = "med-1", hour = 18, minute = 0),
            ),
        )

        val result = dao.observeById("med-1").first()
        assertEquals(2, result?.times?.size)
        assertEquals(listOf(12 to 30, 18 to 0), result?.times?.map { it.hour to it.minute })
    }

    @Test
    fun delete_cascadeRemovesTimes() = runTest {
        val reminder = sampleReminder(id = "med-1")
        dao.upsert(
            reminder,
            listOf(MedicationReminderTimeEntity(medicationId = "med-1", hour = 9, minute = 0)),
        )

        dao.delete("med-1")

        assertNull(dao.observeById("med-1").first())
        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun observeAll_emitsInDescendingCreatedAtOrder() = runTest {
        dao.upsert(
            sampleReminder(id = "older", createdAtMillis = 100L),
            listOf(MedicationReminderTimeEntity(medicationId = "older", hour = 8, minute = 0)),
        )
        dao.upsert(
            sampleReminder(id = "newer", createdAtMillis = 200L),
            listOf(MedicationReminderTimeEntity(medicationId = "newer", hour = 9, minute = 0)),
        )

        val all = dao.observeAll().first()
        assertEquals(listOf("newer", "older"), all.map { it.reminder.id })
    }

    @Test
    fun toggleEnabled_updatesEnabledFlag() = runTest {
        val reminder = sampleReminder(id = "med-1", enabled = true)
        dao.upsert(
            reminder,
            listOf(MedicationReminderTimeEntity(medicationId = "med-1", hour = 8, minute = 0)),
        )

        dao.toggleEnabled("med-1", enabled = false)

        assertEquals(false, dao.observeById("med-1").first()?.reminder?.enabled)
    }

    private fun sampleReminder(
        id: String,
        enabled: Boolean = true,
        createdAtMillis: Long = 1_000L,
    ) = MedicationReminderEntity(
        id = id,
        name = "Aspirin",
        dosage = "100mg",
        enabled = enabled,
        repeatDaysMask = 127,
        createdAtMillis = createdAtMillis,
    )
}
