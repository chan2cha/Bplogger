package com.pulselog.domain

import com.pulselog.data.DailyHealthRecord
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GraphPolicyTest {

    @Test
    fun buildGraphPoints_returnsCompleteRecentDateRange() {
        val today = LocalDate.parse("2026-05-11")
        val points = GraphPolicy.buildGraphPoints(
            records = listOf(
                record("2026-05-09", morningSystolic = 120, morningDiastolic = 80),
                record("2026-05-11", eveningSystolic = 130, eveningDiastolic = 85, weightKg = 64.5)
            ),
            days = 3,
            today = today
        )

        assertEquals(listOf("2026-05-09", "2026-05-10", "2026-05-11"), points.map { it.dateIso })
        assertEquals(120, points[0].morningSystolic)
        assertNull(points[1].morningSystolic)
        assertEquals(130, points[2].eveningSystolic)
        assertEquals(64.5, points[2].weightKg ?: -1.0, 0.0)
    }

    @Test
    fun buildGraphPoints_excludesOutOfRangeAndInvalidDateRecords() {
        val today = LocalDate.parse("2026-05-11")
        val points = GraphPolicy.buildGraphPoints(
            records = listOf(
                record("2026-05-08", morningSystolic = 111),
                record("2026-05-10", morningSystolic = 122),
                record("2026-05-12", morningSystolic = 133),
                record("not-a-date", morningSystolic = 144)
            ),
            days = 2,
            today = today
        )

        assertEquals(listOf("2026-05-10", "2026-05-11"), points.map { it.dateIso })
        assertEquals(122, points[0].morningSystolic)
        assertNull(points[1].morningSystolic)
    }

    @Test
    fun buildDateRange_coercesDaysToAtLeastOne() {
        val range = GraphPolicy.buildDateRange(
            today = LocalDate.parse("2026-05-11"),
            days = 0
        )

        assertEquals(listOf(LocalDate.parse("2026-05-11")), range)
    }

    @Test
    fun valueRange_addsPaddingAroundDifferentValues() {
        val range = GraphPolicy.valueRange(
            series = listOf(listOf(100.0, 200.0)),
            paddingRatio = 0.1
        )

        requireNotNull(range)
        assertEquals(90.0, range.min, 0.0)
        assertEquals(210.0, range.max, 0.0)
        assertEquals(120.0, range.span, 0.0)
    }

    @Test
    fun valueRange_expandsSingleFlatValue() {
        val range = GraphPolicy.valueRange(series = listOf(listOf(null, 120.0, 120.0)))

        requireNotNull(range)
        assertEquals(119.0, range.min, 0.0)
        assertEquals(121.0, range.max, 0.0)
        assertEquals(2.0, range.span, 0.0)
    }

    @Test
    fun valueRange_returnsNullWhenNoValuesExist() {
        assertNull(GraphPolicy.valueRange(series = listOf(listOf(null, null))))
    }

    private fun record(
        dateIso: String,
        morningSystolic: Int? = null,
        morningDiastolic: Int? = null,
        eveningSystolic: Int? = null,
        eveningDiastolic: Int? = null,
        weightKg: Double? = null
    ): DailyHealthRecord {
        return DailyHealthRecord(
            dateIso = dateIso,
            morningSystolic = morningSystolic,
            morningDiastolic = morningDiastolic,
            morningMeasuredAtEpochMs = null,
            eveningSystolic = eveningSystolic,
            eveningDiastolic = eveningDiastolic,
            eveningMeasuredAtEpochMs = null,
            weightKg = weightKg,
            weightMeasuredAtEpochMs = null,
            createdAtEpochMs = 1L,
            updatedAtEpochMs = 1L
        )
    }
}
