package com.pulselog.domain

import com.pulselog.data.CalendarDayStatus
import com.pulselog.data.DailyHealthRecord
import com.pulselog.data.DayRecordStatus
import java.time.YearMonth

object CalendarPolicy {
    fun buildMonthStatuses(
        month: YearMonth,
        records: List<DailyHealthRecord>
    ): List<CalendarDayStatus> {
        val recordMap = records.associateBy { it.dateIso }

        return (1..month.lengthOfMonth()).map { day ->
            val date = month.atDay(day).toString()
            val record = recordMap[date]
            CalendarDayStatus(
                dateIso = date,
                hasMorningRecord = record?.morningSystolic != null,
                hasEveningRecord = record?.eveningSystolic != null,
                hasWeight = record?.weightKg != null,
                status = record?.status() ?: DayRecordStatus.NONE
            )
        }
    }
}
