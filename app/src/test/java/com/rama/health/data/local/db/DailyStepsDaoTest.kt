package com.rama.health.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DailyStepsDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: DailyStepsDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.dailyStepsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertThenGetByDate_returnsInsertedEntity() = runTest {
        val entity = DailyStepsEntity(date = "2026-07-07", steps = 500)
        dao.upsert(entity)
        val result = dao.getByDate("2026-07-07")
        assertEquals(entity, result)
    }

    @Test
    fun upsertSameDateTwice_overwritesRatherThanDuplicates() = runTest {
        dao.upsert(DailyStepsEntity(date = "2026-07-07", steps = 500))
        dao.upsert(DailyStepsEntity(date = "2026-07-07", steps = 1200))
        val result = dao.getByDate("2026-07-07")
        assertEquals(1200, result?.steps)

        val all = dao.observeAll().first()
        assertEquals(1, all.size)
    }

    @Test
    fun observeAll_emitsInDescendingDateOrder() = runTest {
        dao.upsert(DailyStepsEntity(date = "2026-07-05", steps = 100))
        dao.upsert(DailyStepsEntity(date = "2026-07-07", steps = 300))
        dao.upsert(DailyStepsEntity(date = "2026-07-06", steps = 200))

        val all = dao.observeAll().first()
        assertEquals(listOf("2026-07-07", "2026-07-06", "2026-07-05"), all.map { it.date })
    }

    @Test
    fun getByDate_missingDate_returnsNull() = runTest {
        val result = dao.getByDate("2026-01-01")
        assertNull(result)
    }
}
