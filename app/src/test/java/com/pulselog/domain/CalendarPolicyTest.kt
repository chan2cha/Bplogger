package com.pulselog.domain

import com.pulselog.data.DailyHealthRecord
import com.pulselog.data.DailyNote
import com.pulselog.data.DayRecordStatus
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarPolicyTest {

    @Test
    fun buildMonthStatusesReturnsOneStatusPerDayInMonth() {
        val statuses = CalendarPolicy.buildMonthStatuses(
            month = YearMonth.parse("2026-02"),
            records = emptyList(),
            notes = emptyList()
        )

        assertEquals(28, statuses.size)
        assertEquals("2026-02-01", statuses.first().dateIso)
        assertEquals("2026-02-28", statuses.last().dateIso)
        assertTrue(statuses.all { it.status == DayRecordStatus.NONE })
    }

    @Test
    fun buildMonthStatusesMapsRecordFieldsAndStatus() {
        val statuses = CalendarPolicy.buildMonthStatuses(
            month = YearMonth.parse("2026-05"),
            records = listOf(
                record("2026-05-03", morningSystolic = 120),
                record("2026-05-04", eveningSystolic = 130),
                record("2026-05-05", morningSystolic = 121, eveningSystolic = 131, weightKg = 64.2)
            ),
            notes = emptyList()
        ).associateBy { it.dateIso }

        val morningOnly = requireNotNull(statuses["2026-05-03"])
        assertTrue(morningOnly.hasMorningRecord)
        assertFalse(morningOnly.hasEveningRecord)
        assertEquals(DayRecordStatus.MORNING_ONLY, morningOnly.status)

        val eveningOnly = requireNotNull(statuses["2026-05-04"])
        assertFalse(eveningOnly.hasMorningRecord)
        assertTrue(eveningOnly.hasEveningRecord)
        assertEquals(DayRecordStatus.EVENING_ONLY, eveningOnly.status)

        val both = requireNotNull(statuses["2026-05-05"])
        assertTrue(both.hasMorningRecord)
        assertTrue(both.hasEveningRecord)
        assertTrue(both.hasWeight)
        assertEquals(DayRecordStatus.BOTH, both.status)
    }

    @Test
    fun buildMonthStatusesMarksOnlyNonBlankNotes() {
        val statuses = CalendarPolicy.buildMonthStatuses(
            month = YearMonth.parse("2026-05"),
            records = emptyList(),
            notes = listOf(
                note("2026-05-03", "check"),
                note("2026-05-04", "   ")
            )
        ).associateBy { it.dateIso }

        assertTrue(requireNotNull(statuses["2026-05-03"]).hasNote)
        assertFalse(requireNotNull(statuses["2026-05-04"]).hasNote)
    }

    @Test
    fun buildMonthStatusesIgnoresRecordsAndNotesOutsideMonth() {
        val statuses = CalendarPolicy.buildMonthStatuses(
            month = YearMonth.parse("2026-05"),
            records = listOf(record("2026-04-30", morningSystolic = 120)),
            notes = listOf(note("2026-06-01", "outside"))
        )

        assertEquals(31, statuses.size)
        assertTrue(statuses.all { !it.hasMorningRecord && !it.hasNote })
    }

    private fun record(
        dateIso: String,
        morningSystolic: Int? = null,
        eveningSystolic: Int? = null,
        weightKg: Double? = null
    ): DailyHealthRecord {
        return DailyHealthRecord(
            dateIso = dateIso,
            morningSystolic = morningSystolic,
            morningDiastolic = morningSystolic?.minus(40),
            eveningSystolic = eveningSystolic,
            eveningDiastolic = eveningSystolic?.minus(45),
            weightKg = weightKg,
            createdAtEpochMs = 1L,
            updatedAtEpochMs = 1L
        )
    }

    private fun note(dateIso: String, note: String): DailyNote {
        return DailyNote(
            dateIso = dateIso,
            note = note,
            createdAtEpochMs = 1L,
            updatedAtEpochMs = 1L
        )
    }
}
