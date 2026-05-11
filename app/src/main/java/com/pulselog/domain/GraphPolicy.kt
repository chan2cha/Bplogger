package com.pulselog.domain

import com.pulselog.data.DailyHealthRecord
import com.pulselog.data.GraphPoint
import java.time.LocalDate

data class ChartValueRange(
    val min: Double,
    val max: Double
) {
    val span: Double = (max - min).takeIf { it > 0.0 } ?: 1.0
}

object GraphPolicy {

    fun buildGraphPoints(
        records: List<DailyHealthRecord>,
        days: Int,
        today: LocalDate
    ): List<GraphPoint> {
        val range = buildDateRange(today = today, days = days)
        val recordsByDate = records.mapNotNull { record ->
            val date = runCatching { LocalDate.parse(record.dateIso) }.getOrNull() ?: return@mapNotNull null
            date to record
        }.toMap()

        return range.map { date ->
            val record = recordsByDate[date]
            GraphPoint(
                dateIso = date.toString(),
                morningSystolic = record?.morningSystolic,
                morningDiastolic = record?.morningDiastolic,
                eveningSystolic = record?.eveningSystolic,
                eveningDiastolic = record?.eveningDiastolic,
                weightKg = record?.weightKg
            )
        }
    }

    fun buildDateRange(today: LocalDate, days: Int): List<LocalDate> {
        val safeDays = days.coerceAtLeast(1)
        val from = today.minusDays((safeDays - 1).toLong())
        return List(safeDays) { index -> from.plusDays(index.toLong()) }
    }

    fun valueRange(series: List<List<Double?>>, paddingRatio: Double = 0.08): ChartValueRange? {
        val values = series.flatten().filterNotNull()
        if (values.isEmpty()) return null

        val minValue = values.minOrNull() ?: return null
        val maxValue = values.maxOrNull() ?: return null
        if (minValue == maxValue) {
            return ChartValueRange(min = minValue - 1.0, max = maxValue + 1.0)
        }

        val padding = (maxValue - minValue) * paddingRatio.coerceAtLeast(0.0)
        return ChartValueRange(min = minValue - padding, max = maxValue + padding)
    }
}
