package com.pulselog.domain

import com.pulselog.data.CalendarDayStatus
import com.pulselog.data.DailyHealthRecord
import com.pulselog.data.DailyNote
import com.pulselog.data.DayRecordStatus
import java.time.YearMonth

object CalendarPolicy {
    fun buildMonthStatuses(
        month: YearMonth,
        records: List<DailyHealthRecord>,
        notes: List<DailyNote>
    ): List<CalendarDayStatus> {
        val recordMap = records.associateBy { it.dateIso }
        val noteSet = notes.filter { it.note.isNotBlank() }.mapTo(hashSetOf()) { it.dateIso }

        return (1..month.lengthOfMonth()).map { day ->
            val date = month.atDay(day).toString()
            val record = recordMap[date]
            CalendarDayStatus(
                dateIso = date,
                hasMorningRecord = record?.morningSystolic != null,
                hasEveningRecord = record?.eveningSystolic != null,
                hasWeight = record?.weightKg != null,
                hasNote = noteSet.contains(date),
                status = record?.status() ?: DayRecordStatus.NONE
            )
        }
    }
}
