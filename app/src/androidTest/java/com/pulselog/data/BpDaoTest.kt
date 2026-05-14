package com.pulselog.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BpDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BpDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.bpDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertRecordCanBeReadAndObserved() = runBlocking {
        val record = DailyHealthRecord(
            dateIso = "2026-05-14",
            morningSystolic = 121,
            morningDiastolic = 79,
            createdAtEpochMs = 10L,
            updatedAtEpochMs = 10L
        )

        dao.upsertRecord(record)

        assertEquals(record, dao.getRecord("2026-05-14"))
        assertEquals(record, dao.observeRecord("2026-05-14").first())
        assertEquals(listOf(record), dao.getAllRecords())
    }

    @Test
    fun deleteRecordRemovesOnlyMatchingDate() = runBlocking {
        val first = DailyHealthRecord(
            dateIso = "2026-05-14",
            createdAtEpochMs = 10L,
            updatedAtEpochMs = 10L
        )
        val second = DailyHealthRecord(
            dateIso = "2026-05-15",
            createdAtEpochMs = 20L,
            updatedAtEpochMs = 20L
        )
        dao.upsertRecord(first)
        dao.upsertRecord(second)

        dao.deleteRecord("2026-05-14")

        assertNull(dao.getRecord("2026-05-14"))
        assertEquals(listOf(second), dao.getAllRecords())
    }

    @Test
    fun upsertSettingsCanBeObserved() = runBlocking {
        val settings = NotificationSettings(
            morningEnabled = false,
            morningTime = "09:00",
            eveningEnabled = true,
            eveningTime = "21:00",
            repeatEnabled = true,
            repeatCount = 2,
            updatedAtEpochMs = 12L
        )

        dao.upsertSettings(settings)

        assertEquals(settings, dao.observeSettings().first())
    }

    @Test
    fun deleteAllTestHelpersClearRecords() = runBlocking {
        dao.upsertRecord(
            DailyHealthRecord(
                dateIso = "2026-05-14",
                createdAtEpochMs = 10L,
                updatedAtEpochMs = 10L
            )
        )

        dao.deleteAllRecords()

        assertEquals(emptyList<DailyHealthRecord>(), dao.getAllRecords())
    }
}
