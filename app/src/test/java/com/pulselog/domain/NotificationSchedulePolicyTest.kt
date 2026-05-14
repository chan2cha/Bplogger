package com.pulselog.domain

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSchedulePolicyTest {
    private val zoneId = ZoneId.of("Asia/Seoul")

    @Test
    fun nextTriggerEpochMs_usesTodayWhenTimeIsStillAhead() {
        val now = epochMs("2026-05-14T07:00:00")

        val trigger = NotificationSchedulePolicy.nextTriggerEpochMs("08:00", now, zoneId)

        assertEquals(epochMs("2026-05-14T08:00:00"), trigger)
    }

    @Test
    fun nextTriggerEpochMs_usesTomorrowWhenTimeAlreadyPassed() {
        val now = epochMs("2026-05-14T21:00:00")

        val trigger = NotificationSchedulePolicy.nextTriggerEpochMs("20:00", now, zoneId)

        assertEquals(epochMs("2026-05-15T20:00:00"), trigger)
    }

    @Test
    fun repeatTriggerEpochMs_addsTenMinutesPerRepeat() {
        val first = epochMs("2026-05-14T08:00:00")

        val trigger = NotificationSchedulePolicy.repeatTriggerEpochMs(first, repeatIndex = 2)

        assertEquals(epochMs("2026-05-14T08:20:00"), trigger)
    }

    private fun epochMs(value: String): Long {
        return LocalDateTime.parse(value).atZone(zoneId).toInstant().toEpochMilli()
    }
}
