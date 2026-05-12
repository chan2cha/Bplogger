package com.pulselog.domain

import com.pulselog.data.CalendarDayStatus
import com.pulselog.data.DailyHealthRecord
import com.pulselog.data.DailyNote
import com.pulselog.data.GraphPoint
import com.pulselog.data.NotificationSettings
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

interface HealthRepository {
    fun observeRecord(date: LocalDate): Flow<DailyHealthRecord?>
    fun observeNote(date: LocalDate): Flow<DailyNote?>
    fun observeMonthStatuses(month: YearMonth): Flow<List<CalendarDayStatus>>
    fun observeGraphPoints(days: Int): Flow<List<GraphPoint>>
    fun observeNotificationSettings(): Flow<NotificationSettings>

    suspend fun exportCsv(range: ExportRange): Pair<String, String>
    suspend fun exportSummary(range: ExportRange): Pair<String, ExportSummary>

    suspend fun saveMorning(date: LocalDate, systolic: Int, diastolic: Int)
    suspend fun saveEvening(date: LocalDate, systolic: Int, diastolic: Int)
    suspend fun saveWeight(date: LocalDate, weightKg: Double)
    suspend fun deleteMorning(date: LocalDate)
    suspend fun deleteEvening(date: LocalDate)
    suspend fun deleteWeight(date: LocalDate)
    suspend fun saveNote(date: LocalDate, note: String)
    suspend fun deleteNote(date: LocalDate)
    suspend fun saveNotificationSettings(settings: NotificationSettings)

    suspend fun seedGraphDemoData()
}
