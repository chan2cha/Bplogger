package com.pulselog.domain

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

object NotificationSchedulePolicy {
    fun nextTriggerEpochMs(time: String, nowEpochMs: Long, zoneId: ZoneId): Long {
        val localTime = LocalTime.parse(normalizeTime(time))
        val now = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId)
        var trigger = now.toLocalDate().atTime(localTime).atZone(zoneId)
        if (!trigger.isAfter(now)) {
            trigger = trigger.plusDays(1)
        }
        return trigger.toInstant().toEpochMilli()
    }

    private fun normalizeTime(time: String): String {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return "%02d:%02d".format(hour, minute)
    }
}
