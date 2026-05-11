package com.pulselog.domain

import com.pulselog.data.DailyHealthRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

enum class ExportRange(
    val fileToken: String,
    val label: String
) {
    RECENT_30_DAYS("recent_30_days", "최근 30일"),
    ALL("all", "전체")
}

data class ExportSummary(
    val rangeLabel: String,
    val generatedDateIso: String,
    val startDateIso: String?,
    val endDateIso: String?,
    val totalRecordDays: Int,
    val morningCount: Int,
    val eveningCount: Int,
    val weightCount: Int,
    val averageMorningSystolic: Int?,
    val averageMorningDiastolic: Int?,
    val averageEveningSystolic: Int?,
    val averageEveningDiastolic: Int?,
    val averageWeightKg: Double?,
    val trendPoints: List<ExportTrendPoint>,
    val latestRows: List<ExportSummaryRow>
)

data class ExportTrendPoint(
    val dateIso: String,
    val systolic: Int?,
    val diastolic: Int?,
    val weightKg: Double?
)

data class ExportSummaryRow(
    val dateIso: String,
    val morningBloodPressure: String,
    val eveningBloodPressure: String,
    val weightKg: String
)

object ExportPolicy {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)

    fun toCsv(records: List<DailyHealthRecord>, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val builder = StringBuilder()
        builder.append('\uFEFF')
        builder.appendLine(
            listOf(
                "date",
                "morning_systolic",
                "morning_diastolic",
                "morning_measured_at",
                "evening_systolic",
                "evening_diastolic",
                "evening_measured_at",
                "weight_kg",
                "weight_measured_at",
                "updated_at"
            ).joinToString(",")
        )

        records.sortedBy { it.dateIso }.forEach { record ->
            builder.appendLine(
                listOf(
                    record.dateIso,
                    record.morningSystolic?.toString().orEmpty(),
                    record.morningDiastolic?.toString().orEmpty(),
                    formatEpoch(record.morningMeasuredAtEpochMs, zoneId),
                    record.eveningSystolic?.toString().orEmpty(),
                    record.eveningDiastolic?.toString().orEmpty(),
                    formatEpoch(record.eveningMeasuredAtEpochMs, zoneId),
                    record.weightKg?.let { formatWeightForCsv(it) }.orEmpty(),
                    formatEpoch(record.weightMeasuredAtEpochMs, zoneId),
                    formatEpoch(record.updatedAtEpochMs, zoneId)
                ).joinToString(",") { escapeCsv(it) }
            )
        }

        return builder.toString()
    }

    fun fileName(range: ExportRange, todayIso: String): String {
        return "pulse_log_${range.fileToken}_$todayIso.csv"
    }

    fun pdfFileName(range: ExportRange, todayIso: String): String {
        return "pulse_log_summary_${range.fileToken}_$todayIso.pdf"
    }

    fun buildSummary(
        range: ExportRange,
        records: List<DailyHealthRecord>,
        todayIso: String
    ): ExportSummary {
        val sortedRecords = records.sortedBy { it.dateIso }
        val morningRecords = sortedRecords.filter { it.morningSystolic != null && it.morningDiastolic != null }
        val eveningRecords = sortedRecords.filter { it.eveningSystolic != null && it.eveningDiastolic != null }
        val weightRecords = sortedRecords.filter { it.weightKg != null }

        return ExportSummary(
            rangeLabel = range.label,
            generatedDateIso = todayIso,
            startDateIso = sortedRecords.firstOrNull()?.dateIso,
            endDateIso = sortedRecords.lastOrNull()?.dateIso,
            totalRecordDays = sortedRecords.size,
            morningCount = morningRecords.size,
            eveningCount = eveningRecords.size,
            weightCount = weightRecords.size,
            averageMorningSystolic = averageInt(morningRecords.mapNotNull { it.morningSystolic }),
            averageMorningDiastolic = averageInt(morningRecords.mapNotNull { it.morningDiastolic }),
            averageEveningSystolic = averageInt(eveningRecords.mapNotNull { it.eveningSystolic }),
            averageEveningDiastolic = averageInt(eveningRecords.mapNotNull { it.eveningDiastolic }),
            averageWeightKg = averageDouble(weightRecords.mapNotNull { it.weightKg }),
            trendPoints = sortedRecords
                .takeLast(30)
                .map { record ->
                    ExportTrendPoint(
                        dateIso = record.dateIso,
                        systolic = averageInt(listOfNotNull(record.morningSystolic, record.eveningSystolic)),
                        diastolic = averageInt(listOfNotNull(record.morningDiastolic, record.eveningDiastolic)),
                        weightKg = record.weightKg
                    )
                },
            latestRows = sortedRecords
                .takeLast(14)
                .map { record ->
                    ExportSummaryRow(
                        dateIso = record.dateIso,
                        morningBloodPressure = formatBloodPressure(record.morningSystolic, record.morningDiastolic),
                        eveningBloodPressure = formatBloodPressure(record.eveningSystolic, record.eveningDiastolic),
                        weightKg = record.weightKg?.let { formatWeightForDisplay(it) }.orEmpty()
                    )
                }
        )
    }

    fun summaryTextLines(summary: ExportSummary): List<String> {
        val period = if (summary.startDateIso == null || summary.endDateIso == null) {
            "기록 없음"
        } else {
            "${summary.startDateIso} ~ ${summary.endDateIso}"
        }
        val morningAverage = formatAverageBloodPressure(
            summary.averageMorningSystolic,
            summary.averageMorningDiastolic
        )
        val eveningAverage = formatAverageBloodPressure(
            summary.averageEveningSystolic,
            summary.averageEveningDiastolic
        )
        val weightAverage = summary.averageWeightKg?.let { "${formatWeightForDisplay(it)} kg" } ?: "-"

        return listOf(
            "Pulse Log 혈압/체중 요약본",
            "생성일: ${summary.generatedDateIso}",
            "범위: ${summary.rangeLabel}",
            "기록 기간: $period",
            "기록 일수: ${summary.totalRecordDays}일",
            "아침 혈압 기록: ${summary.morningCount}건 / 평균 $morningAverage",
            "저녁 혈압 기록: ${summary.eveningCount}건 / 평균 $eveningAverage",
            "체중 기록: ${summary.weightCount}건 / 평균 $weightAverage",
            "최근 기록",
            "날짜 | 아침 혈압 | 저녁 혈압 | 체중"
        ) + summary.latestRows.map { row ->
            "${row.dateIso} | ${row.morningBloodPressure.ifBlank { "-" }} | " +
                "${row.eveningBloodPressure.ifBlank { "-" }} | ${row.weightKg.ifBlank { "-" }}"
        }
    }

    private fun formatEpoch(epochMs: Long?, zoneId: ZoneId): String {
        return epochMs
            ?.let { Instant.ofEpochMilli(it).atZone(zoneId).format(dateTimeFormatter) }
            .orEmpty()
    }

    private fun formatWeightForCsv(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", value)
        }
    }

    private fun formatWeightForDisplay(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
    }

    private fun formatBloodPressure(systolic: Int?, diastolic: Int?): String {
        return if (systolic == null || diastolic == null) {
            ""
        } else {
            "$systolic/$diastolic"
        }
    }

    private fun formatAverageBloodPressure(systolic: Int?, diastolic: Int?): String {
        return if (systolic == null || diastolic == null) {
            "-"
        } else {
            "$systolic/$diastolic mmHg"
        }
    }

    private fun averageInt(values: List<Int>): Int? {
        if (values.isEmpty()) return null
        return values.average().roundToInt()
    }

    private fun averageDouble(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        return values.average()
    }

    private fun escapeCsv(value: String): String {
        val needsEscaping = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsEscaping) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }
}
