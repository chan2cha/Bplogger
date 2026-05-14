package com.pulselog.ui

import com.pulselog.data.BpDao
import com.pulselog.data.DailyHealthRecord
import com.pulselog.data.NotificationSettings
import com.pulselog.test.FakeClockProvider
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BpRepositoryTest {

    @Test
    fun saveMorning_usesClockProviderForCreatedUpdatedAndMeasuredTimes() = runBlocking {
        val dao = FakeBpDao()
        val repo = BpRepository(
            dao = dao,
            clockProvider = FakeClockProvider(epochMs = 123_456L)
        )

        repo.saveMorning(LocalDate.parse("2026-05-12"), systolic = 121, diastolic = 79)

        val record = requireNotNull(dao.getRecord("2026-05-12"))
        assertEquals(123_456L, record.createdAtEpochMs)
        assertEquals(123_456L, record.updatedAtEpochMs)
        assertEquals(123_456L, record.morningMeasuredAtEpochMs)
    }

    @Test
    fun deleteMorning_usesClockProviderForUpdatedTimeBeforePersistingRemainingFields() = runBlocking {
        val dao = FakeBpDao()
        dao.upsertRecord(
            DailyHealthRecord(
                dateIso = "2026-05-12",
                morningSystolic = 121,
                morningDiastolic = 79,
                morningMeasuredAtEpochMs = 1L,
                weightKg = 65.4,
                weightMeasuredAtEpochMs = 2L,
                createdAtEpochMs = 1L,
                updatedAtEpochMs = 1L
            )
        )
        val repo = BpRepository(
            dao = dao,
            clockProvider = FakeClockProvider(epochMs = 999L)
        )

        repo.deleteMorning(LocalDate.parse("2026-05-12"))

        val record = requireNotNull(dao.getRecord("2026-05-12"))
        assertNull(record.morningSystolic)
        assertNull(record.morningDiastolic)
        assertNull(record.morningMeasuredAtEpochMs)
        assertEquals(65.4, record.weightKg ?: 0.0, 0.0)
        assertEquals(999L, record.updatedAtEpochMs)
    }

    @Test
    fun saveSettings_usesClockProviderForTimestamp() = runBlocking {
        val dao = FakeBpDao()
        val repo = BpRepository(
            dao = dao,
            clockProvider = FakeClockProvider(epochMs = 77L)
        )

        repo.saveNotificationSettings(NotificationSettings(updatedAtEpochMs = 1L))

        assertEquals(77L, dao.settings.value?.updatedAtEpochMs)
    }

    private class FakeBpDao : BpDao {
        private val records = MutableStateFlow<List<DailyHealthRecord>>(emptyList())
        val settings = MutableStateFlow<NotificationSettings?>(null)

        override fun observeAllRecords(): Flow<List<DailyHealthRecord>> = records

        override fun observeRecord(dateIso: String): Flow<DailyHealthRecord?> {
            return records.map { items -> items.firstOrNull { it.dateIso == dateIso } }
        }

        override suspend fun getRecord(dateIso: String): DailyHealthRecord? {
            return records.value.firstOrNull { it.dateIso == dateIso }
        }

        override suspend fun getAllRecords(): List<DailyHealthRecord> = records.value

        override suspend fun getRecordsFrom(fromDateIso: String): List<DailyHealthRecord> {
            return records.value.filter { it.dateIso >= fromDateIso }.sortedBy { it.dateIso }
        }

        override suspend fun upsertRecord(record: DailyHealthRecord) {
            records.value = records.value.filterNot { it.dateIso == record.dateIso } + record
        }

        override suspend fun deleteRecord(dateIso: String) {
            records.value = records.value.filterNot { it.dateIso == dateIso }
        }

        override suspend fun deleteAllRecords() {
            records.value = emptyList()
        }

        override fun observeSettings(): Flow<NotificationSettings?> = settings

        override suspend fun getSettings(): NotificationSettings? = settings.value

        override suspend fun upsertSettings(settings: NotificationSettings) {
            this.settings.value = settings
        }
    }
}
