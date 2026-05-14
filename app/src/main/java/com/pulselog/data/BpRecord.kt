package com.pulselog.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_health_records")
data class DailyHealthRecord(
    @PrimaryKey val dateIso: String,
    val morningSystolic: Int? = null,
    val morningDiastolic: Int? = null,
    val morningMeasuredAtEpochMs: Long? = null,
    val eveningSystolic: Int? = null,
    val eveningDiastolic: Int? = null,
    val eveningMeasuredAtEpochMs: Long? = null,
    val weightKg: Double? = null,
    val weightMeasuredAtEpochMs: Long? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    fun status(): DayRecordStatus = when {
        morningSystolic != null && eveningSystolic != null -> DayRecordStatus.BOTH
        morningSystolic != null -> DayRecordStatus.MORNING_ONLY
        eveningSystolic != null -> DayRecordStatus.EVENING_ONLY
        else -> DayRecordStatus.NONE
    }

    fun isEmpty(): Boolean {
        return morningSystolic == null &&
            morningDiastolic == null &&
            eveningSystolic == null &&
            eveningDiastolic == null &&
            weightKg == null
    }
}

@Entity(tableName = "notification_settings")
data class NotificationSettings(
    @PrimaryKey val id: Int = 1,
    val morningEnabled: Boolean = true,
    val morningTime: String = "08:00",
    val eveningEnabled: Boolean = true,
    val eveningTime: String = "20:00",
    val repeatEnabled: Boolean = false,
    val repeatCount: Int = 0,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

enum class DayRecordStatus {
    NONE,
    MORNING_ONLY,
    EVENING_ONLY,
    BOTH
}

data class CalendarDayStatus(
    val dateIso: String,
    val hasMorningRecord: Boolean,
    val hasEveningRecord: Boolean,
    val hasWeight: Boolean,
    val status: DayRecordStatus
)

data class GraphPoint(
    val dateIso: String,
    val morningSystolic: Int?,
    val morningDiastolic: Int?,
    val eveningSystolic: Int?,
    val eveningDiastolic: Int?,
    val weightKg: Double?
)
