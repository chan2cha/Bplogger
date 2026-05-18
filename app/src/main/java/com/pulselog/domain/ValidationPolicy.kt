package com.pulselog.domain

import java.math.BigDecimal
import java.math.RoundingMode

object ValidationPolicy {
    fun parsePressure(value: String): Int? {
        val parsed = value.toIntOrNull() ?: return null
        return parsed.takeIf { it in 50..200 }
    }

    fun parseWeightKg(value: String): Double? {
        val parsed = value.toDoubleOrNull() ?: return null
        if (parsed < 0.0 || parsed > 100.0) return null

        return BigDecimal.valueOf(parsed)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun isValidTime(value: String): Boolean {
        val parts = value.split(":")
        if (parts.size != 2) return false
        val hour = parts[0].toIntOrNull() ?: return false
        val minute = parts[1].toIntOrNull() ?: return false
        return hour in 0..23 && minute in 0..59
    }

}
