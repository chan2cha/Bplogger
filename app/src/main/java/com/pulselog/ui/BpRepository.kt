package com.pulselog.ui

import com.pulselog.data.BpDao
import com.pulselog.data.CalendarDayStatus
import com.pulselog.data.DailyHealthRecord
import com.pulselog.data.DailyNote
import com.pulselog.data.GraphPoint
import com.pulselog.data.NotificationSettings
import com.pulselog.domain.CalendarPolicy
import com.pulselog.domain.ClockProvider
import com.pulselog.domain.ExportPolicy
import com.pulselog.domain.ExportRange
import com.pulselog.domain.ExportSummary
import com.pulselog.domain.GraphPolicy
import com.pulselog.domain.HealthRepository
import com.pulselog.domain.SystemClockProvider
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class BpRepository(
    private val dao: BpDao,
    private val clockProvider: ClockProvider = SystemClockProvider()
) : HealthRepository {

    override fun observeRecord(date: LocalDate): Flow<DailyHealthRecord?> = dao.observeRecord(date.toString())

    override fun observeNote(date: LocalDate): Flow<DailyNote?> = dao.observeNote(date.toString())

    override fun observeMonthStatuses(month: YearMonth): Flow<List<CalendarDayStatus>> {
        return combine(dao.observeAllRecords(), dao.observeAllNotes()) { records, notes ->
            CalendarPolicy.buildMonthStatuses(
                month = month,
                records = records,
                notes = notes
            )
        }
    }

    override fun observeGraphPoints(days: Int): Flow<List<GraphPoint>> {
        return dao.observeAllRecords().map { records ->
            GraphPolicy.buildGraphPoints(
                records = records,
                days = days,
                today = clockProvider.today()
            )
        }
    }

    override fun observeNotificationSettings(): Flow<NotificationSettings> {
        return dao.observeSettings().map { it ?: NotificationSettings() }
    }

    override suspend fun exportCsv(range: ExportRange): Pair<String, String> {
        val today = clockProvider.today()
        val records = getExportRecords(range, today)
        return ExportPolicy.fileName(range = range, todayIso = today.toString()) to ExportPolicy.toCsv(records)
    }

    override suspend fun exportSummary(range: ExportRange): Pair<String, ExportSummary> {
        val today = clockProvider.today()
        val records = getExportRecords(range, today)
        return ExportPolicy.pdfFileName(range = range, todayIso = today.toString()) to
            ExportPolicy.buildSummary(range = range, records = records, todayIso = today.toString())
    }

    override suspend fun saveMorning(date: LocalDate, systolic: Int, diastolic: Int) {
        upsertRecord(date) { existing, now ->
            val base = existing ?: DailyHealthRecord(
                dateIso = date.toString(),
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
            base.copy(
                morningSystolic = systolic,
                morningDiastolic = diastolic,
                morningMeasuredAtEpochMs = base.morningMeasuredAtEpochMs ?: now,
                updatedAtEpochMs = now
            )
        }
    }

    override suspend fun saveEvening(date: LocalDate, systolic: Int, diastolic: Int) {
        upsertRecord(date) { existing, now ->
            val base = existing ?: DailyHealthRecord(
                dateIso = date.toString(),
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
            base.copy(
                eveningSystolic = systolic,
                eveningDiastolic = diastolic,
                eveningMeasuredAtEpochMs = base.eveningMeasuredAtEpochMs ?: now,
                updatedAtEpochMs = now
            )
        }
    }

    override suspend fun saveWeight(date: LocalDate, weightKg: Double) {
        upsertRecord(date) { existing, now ->
            val base = existing ?: DailyHealthRecord(
                dateIso = date.toString(),
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
            base.copy(
                weightKg = weightKg,
                weightMeasuredAtEpochMs = base.weightMeasuredAtEpochMs ?: now,
                updatedAtEpochMs = now
            )
        }
    }

    override suspend fun deleteMorning(date: LocalDate) {
        val current = dao.getRecord(date.toString()) ?: return
        persistOrDelete(
            current.copy(
                morningSystolic = null,
                morningDiastolic = null,
                morningMeasuredAtEpochMs = null,
                updatedAtEpochMs = clockProvider.nowEpochMs()
            )
        )
    }

    override suspend fun deleteEvening(date: LocalDate) {
        val current = dao.getRecord(date.toString()) ?: return
        persistOrDelete(
            current.copy(
                eveningSystolic = null,
                eveningDiastolic = null,
                eveningMeasuredAtEpochMs = null,
                updatedAtEpochMs = clockProvider.nowEpochMs()
            )
        )
    }

    override suspend fun deleteWeight(date: LocalDate) {
        val current = dao.getRecord(date.toString()) ?: return
        persistOrDelete(
            current.copy(
                weightKg = null,
                weightMeasuredAtEpochMs = null,
                updatedAtEpochMs = clockProvider.nowEpochMs()
            )
        )
    }

    override suspend fun saveNote(date: LocalDate, note: String) {
        val trimmed = note.trim()
        if (trimmed.isBlank()) {
            dao.deleteNote(date.toString())
            return
        }

        val now = clockProvider.nowEpochMs()
        val existing = dao.getNote(date.toString())
        dao.upsertNote(
            DailyNote(
                dateIso = date.toString(),
                note = trimmed,
                createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now
            )
        )
    }

    override suspend fun deleteNote(date: LocalDate) {
        dao.deleteNote(date.toString())
    }

    override suspend fun saveNotificationSettings(settings: NotificationSettings) {
        dao.upsertSettings(settings.copy(updatedAtEpochMs = clockProvider.nowEpochMs()))
    }

    override suspend fun seedGraphDemoData() {
        val today = clockProvider.today()
        val baseEpoch = clockProvider.nowEpochMs()
        val demoWeights = listOf(
            65.2,
            65.0,
            64.8,
            64.9,
            64.6,
            64.4,
            64.1,
            64.3,
            64.0,
            63.8,
            63.7,
            63.9
        )

        repeat(30) { index ->
            val date = today.minusDays((29 - index).toLong())
            val systolicWave = when (index % 7) {
                0 -> -4
                1 -> -1
                2 -> 3
                3 -> 6
                4 -> 2
                5 -> -2
                else -> 1
            }
            val diastolicWave = when (index % 5) {
                0 -> -3
                1 -> 1
                2 -> 4
                3 -> 2
                else -> -1
            }
            val hasEvening = index % 6 != 1
            val hasWeight = index % 4 != 2
            val now = baseEpoch - ((29 - index) * 24L * 60L * 60L * 1000L)

            dao.upsertRecord(
                DailyHealthRecord(
                    dateIso = date.toString(),
                    morningSystolic = 118 + systolicWave + (index / 10),
                    morningDiastolic = 76 + diastolicWave,
                    morningMeasuredAtEpochMs = now + 8L * 60L * 60L * 1000L,
                    eveningSystolic = if (hasEvening) 124 + systolicWave + (index / 12) else null,
                    eveningDiastolic = if (hasEvening) 80 + diastolicWave else null,
                    eveningMeasuredAtEpochMs = if (hasEvening) now + 20L * 60L * 60L * 1000L else null,
                    weightKg = if (hasWeight) demoWeights[index % demoWeights.size] else null,
                    weightMeasuredAtEpochMs = if (hasWeight) now + 7L * 60L * 60L * 1000L else null,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
        }
    }

    private suspend fun upsertRecord(
        date: LocalDate,
        transform: (DailyHealthRecord?, Long) -> DailyHealthRecord
    ) {
        val now = clockProvider.nowEpochMs()
        val current = dao.getRecord(date.toString())
        dao.upsertRecord(transform(current, now))
    }

    private suspend fun persistOrDelete(record: DailyHealthRecord) {
        if (record.isEmpty()) {
            dao.deleteRecord(record.dateIso)
        } else {
            dao.upsertRecord(record)
        }
    }

    private suspend fun getExportRecords(range: ExportRange, today: LocalDate): List<DailyHealthRecord> {
        return when (range) {
            ExportRange.RECENT_30_DAYS -> dao.getRecordsFrom(today.minusDays(29).toString())
            ExportRange.ALL -> dao.getAllRecords()
        }
    }
}
