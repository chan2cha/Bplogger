package com.example.bplogger.ui

import com.example.bplogger.data.BpDao
import com.example.bplogger.data.CalendarDayStatus
import com.example.bplogger.data.DailyHealthRecord
import com.example.bplogger.data.DailyNote
import com.example.bplogger.data.DayRecordStatus
import com.example.bplogger.data.GraphPoint
import com.example.bplogger.data.NotificationSettings
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class BpRepository(private val dao: BpDao) {

    fun observeRecord(date: LocalDate): Flow<DailyHealthRecord?> = dao.observeRecord(date.toString())

    fun observeNote(date: LocalDate): Flow<DailyNote?> = dao.observeNote(date.toString())

    fun observeMonthStatuses(month: YearMonth): Flow<List<CalendarDayStatus>> {
        return combine(dao.observeAllRecords(), dao.observeAllNotes()) { records, notes ->
            val recordMap = records.associateBy { it.dateIso }
            val noteSet = notes.filter { it.note.isNotBlank() }.mapTo(hashSetOf()) { it.dateIso }

            (1..month.lengthOfMonth()).map { day ->
                val date = month.atDay(day).toString()
                val record = recordMap[date]
                val status = record?.status() ?: DayRecordStatus.NONE
                CalendarDayStatus(
                    dateIso = date,
                    hasMorningRecord = record?.morningSystolic != null,
                    hasEveningRecord = record?.eveningSystolic != null,
                    hasWeight = record?.weightKg != null,
                    hasNote = noteSet.contains(date),
                    status = status
                )
            }
        }
    }

    fun observeGraphPoints(days: Int): Flow<List<GraphPoint>> {
        val today = LocalDate.now()
        val from = today.minusDays((days - 1).toLong())
        return dao.observeAllRecords().map { records ->
            records.mapNotNull { record ->
                val date = runCatching { LocalDate.parse(record.dateIso) }.getOrNull() ?: return@mapNotNull null
                if (date < from || date > today) return@mapNotNull null
                GraphPoint(
                    dateIso = record.dateIso,
                    morningSystolic = record.morningSystolic,
                    morningDiastolic = record.morningDiastolic,
                    eveningSystolic = record.eveningSystolic,
                    eveningDiastolic = record.eveningDiastolic,
                    weightKg = record.weightKg
                )
            }.sortedBy { it.dateIso }
        }
    }

    fun observeNotificationSettings(): Flow<NotificationSettings> {
        return dao.observeSettings().map { it ?: NotificationSettings() }
    }

    suspend fun saveMorning(date: LocalDate, systolic: Int, diastolic: Int) {
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

    suspend fun saveEvening(date: LocalDate, systolic: Int, diastolic: Int) {
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

    suspend fun saveWeight(date: LocalDate, weightKg: Double) {
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

    suspend fun deleteMorning(date: LocalDate) {
        val current = dao.getRecord(date.toString()) ?: return
        persistOrDelete(
            current.copy(
                morningSystolic = null,
                morningDiastolic = null,
                morningMeasuredAtEpochMs = null,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteEvening(date: LocalDate) {
        val current = dao.getRecord(date.toString()) ?: return
        persistOrDelete(
            current.copy(
                eveningSystolic = null,
                eveningDiastolic = null,
                eveningMeasuredAtEpochMs = null,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteWeight(date: LocalDate) {
        val current = dao.getRecord(date.toString()) ?: return
        persistOrDelete(
            current.copy(
                weightKg = null,
                weightMeasuredAtEpochMs = null,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveNote(date: LocalDate, note: String) {
        val trimmed = note.trim()
        if (trimmed.isBlank()) {
            dao.deleteNote(date.toString())
            return
        }

        val now = System.currentTimeMillis()
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

    suspend fun deleteNote(date: LocalDate) {
        dao.deleteNote(date.toString())
    }

    suspend fun saveNotificationSettings(settings: NotificationSettings) {
        dao.upsertSettings(settings.copy(updatedAtEpochMs = System.currentTimeMillis()))
    }

    private suspend fun upsertRecord(
        date: LocalDate,
        transform: (DailyHealthRecord?, Long) -> DailyHealthRecord
    ) {
        val now = System.currentTimeMillis()
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
}
