package com.pulselog.domain

import com.pulselog.data.DailyHealthRecord
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportPolicyTest {

    @Test
    fun csvIncludesUtf8BomHeaderAndRowsInDateOrder() {
        val records = listOf(
            record(dateIso = "2026-05-11", systolic = 122, diastolic = 81, updatedAt = 1_767_934_800_000L),
            record(dateIso = "2026-05-10", weight = 64.2, updatedAt = 1_767_848_400_000L)
        )

        val csv = ExportPolicy.toCsv(records, ZoneId.of("Asia/Seoul"))
        val lines = csv.lines().filter { it.isNotEmpty() }

        assertTrue(csv.startsWith('\uFEFF'))
        assertEquals(
            "\uFEFFdate,morning_systolic,morning_diastolic,morning_measured_at,evening_systolic,evening_diastolic,evening_measured_at,weight_kg,weight_measured_at,updated_at",
            lines[0]
        )
        assertTrue(lines[1].startsWith("2026-05-10,,,,,,,64.20,,"))
        assertTrue(lines[2].startsWith("2026-05-11,122,81,"))
    }

    @Test
    fun fileNameContainsRangeAndDate() {
        assertEquals(
            "pulse_log_recent_30_days_2026-05-11.csv",
            ExportPolicy.fileName(ExportRange.RECENT_30_DAYS, "2026-05-11")
        )
    }

    @Test
    fun summaryCalculatesCountsAveragesAndLatestRows() {
        val records = listOf(
            record(dateIso = "2026-05-09", systolic = 120, diastolic = 80, weight = 64.0, updatedAt = 1L),
            record(dateIso = "2026-05-10", systolic = 124, diastolic = 82, weight = 64.4, updatedAt = 2L)
        )

        val summary = ExportPolicy.buildSummary(
            range = ExportRange.RECENT_30_DAYS,
            records = records,
            todayIso = "2026-05-11"
        )
        val lines = ExportPolicy.summaryTextLines(summary)

        assertEquals("최근 30일", summary.rangeLabel)
        assertEquals("2026-05-09", summary.startDateIso)
        assertEquals("2026-05-10", summary.endDateIso)
        assertEquals(2, summary.totalRecordDays)
        assertEquals(2, summary.morningCount)
        assertEquals(0, summary.eveningCount)
        assertEquals(2, summary.weightCount)
        assertEquals(122, summary.averageMorningSystolic)
        assertEquals(81, summary.averageMorningDiastolic)
        assertEquals(64.2, summary.averageWeightKg ?: 0.0, 0.001)
        assertEquals(2, summary.trendPoints.size)
        assertEquals(124, summary.trendPoints.last().systolic)
        assertEquals(82, summary.trendPoints.last().diastolic)
        assertEquals("2026-05-09", summary.latestRows.first().dateIso)
        assertTrue(lines.any { it.contains("아침 혈압 기록: 2건 / 평균 122/81 mmHg") })
    }

    @Test
    fun pdfFileNameContainsRangeAndDate() {
        assertEquals(
            "pulse_log_summary_all_2026-05-11.pdf",
            ExportPolicy.pdfFileName(ExportRange.ALL, "2026-05-11")
        )
    }

    private fun record(
        dateIso: String,
        systolic: Int? = null,
        diastolic: Int? = null,
        weight: Double? = null,
        updatedAt: Long
    ): DailyHealthRecord {
        return DailyHealthRecord(
            dateIso = dateIso,
            morningSystolic = systolic,
            morningDiastolic = diastolic,
            morningMeasuredAtEpochMs = if (systolic == null) null else updatedAt,
            weightKg = weight,
            createdAtEpochMs = updatedAt,
            updatedAtEpochMs = updatedAt
        )
    }
}
