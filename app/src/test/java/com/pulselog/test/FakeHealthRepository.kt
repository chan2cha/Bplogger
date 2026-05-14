package com.pulselog.test

import com.pulselog.data.CalendarDayStatus
import com.pulselog.data.DailyHealthRecord
import com.pulselog.data.GraphPoint
import com.pulselog.data.NotificationSettings
import com.pulselog.domain.ExportRange
import com.pulselog.domain.ExportSummary
import com.pulselog.domain.HealthRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

data class SaveMorningCall(
    val date: LocalDate,
    val systolic: Int,
    val diastolic: Int
)

data class SaveEveningCall(
    val date: LocalDate,
    val systolic: Int,
    val diastolic: Int
)

data class SaveWeightCall(
    val date: LocalDate,
    val weightKg: Double
)

class FakeHealthRepository : HealthRepository {
    val saveMorningCalls = mutableListOf<SaveMorningCall>()
    val saveEveningCalls = mutableListOf<SaveEveningCall>()
    val saveWeightCalls = mutableListOf<SaveWeightCall>()
    val deleteMorningCalls = mutableListOf<LocalDate>()
    val deleteEveningCalls = mutableListOf<LocalDate>()
    val deleteWeightCalls = mutableListOf<LocalDate>()
    val savedNotificationSettings = mutableListOf<NotificationSettings>()
    val exportCsvCalls = mutableListOf<ExportRange>()
    val exportSummaryCalls = mutableListOf<ExportRange>()
    var exportCsvResult: Pair<String, String> = "test.csv" to ""
    var exportSummaryResult: Pair<String, ExportSummary> = "test.pdf" to emptyExportSummary()

    override fun observeRecord(date: LocalDate): Flow<DailyHealthRecord?> = MutableStateFlow(null)

    override fun observeMonthStatuses(month: YearMonth): Flow<List<CalendarDayStatus>> {
        return MutableStateFlow(emptyList())
    }

    override fun observeGraphPoints(days: Int): Flow<List<GraphPoint>> {
        return MutableStateFlow(emptyList())
    }

    override fun observeNotificationSettings(): Flow<NotificationSettings> {
        return MutableStateFlow(NotificationSettings())
    }

    override suspend fun exportCsv(range: ExportRange): Pair<String, String> {
        exportCsvCalls += range
        return exportCsvResult
    }

    override suspend fun exportSummary(range: ExportRange): Pair<String, ExportSummary> {
        exportSummaryCalls += range
        return exportSummaryResult
    }

    override suspend fun saveMorning(date: LocalDate, systolic: Int, diastolic: Int) {
        saveMorningCalls += SaveMorningCall(date, systolic, diastolic)
    }

    override suspend fun saveEvening(date: LocalDate, systolic: Int, diastolic: Int) {
        saveEveningCalls += SaveEveningCall(date, systolic, diastolic)
    }

    override suspend fun saveWeight(date: LocalDate, weightKg: Double) {
        saveWeightCalls += SaveWeightCall(date, weightKg)
    }

    override suspend fun deleteMorning(date: LocalDate) {
        deleteMorningCalls += date
    }

    override suspend fun deleteEvening(date: LocalDate) {
        deleteEveningCalls += date
    }

    override suspend fun deleteWeight(date: LocalDate) {
        deleteWeightCalls += date
    }

    override suspend fun saveNotificationSettings(settings: NotificationSettings) {
        savedNotificationSettings += settings
    }

    override suspend fun seedGraphDemoData() = Unit

    companion object {
        fun emptyExportSummary(range: ExportRange = ExportRange.RECENT_30_DAYS): ExportSummary {
            return ExportSummary(
                rangeLabel = range.label,
                generatedDateIso = "2026-05-12",
                startDateIso = null,
                endDateIso = null,
                totalRecordDays = 0,
                morningCount = 0,
                eveningCount = 0,
                weightCount = 0,
                averageMorningSystolic = null,
                averageMorningDiastolic = null,
                averageEveningSystolic = null,
                averageEveningDiastolic = null,
                averageWeightKg = null,
                latestRows = emptyList(),
                trendPoints = emptyList()
            )
        }
    }
}
