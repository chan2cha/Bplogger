package com.example.bplogger.ui

import java.time.LocalDate
import java.time.LocalTime

data class DateSlot(
    val dateIso: String,
    val slot: String // "AM" or "PM"
)

fun deriveDateSlotFromDeviceTime(): DateSlot {
    val dateIso = LocalDate.now().toString()
    val slot = if (LocalTime.now().hour < 12) "AM" else "PM"
    return DateSlot(dateIso, slot)
}