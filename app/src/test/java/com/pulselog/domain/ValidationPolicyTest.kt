package com.pulselog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationPolicyTest {

    @Test
    fun parsePressureAcceptsInclusiveRange() {
        assertEquals(50, ValidationPolicy.parsePressure("50"))
        assertEquals(200, ValidationPolicy.parsePressure("200"))
        assertEquals(121, ValidationPolicy.parsePressure("121"))
    }

    @Test
    fun parsePressureRejectsInvalidOrOutOfRangeValues() {
        assertNull(ValidationPolicy.parsePressure("49"))
        assertNull(ValidationPolicy.parsePressure("201"))
        assertNull(ValidationPolicy.parsePressure("12.5"))
        assertNull(ValidationPolicy.parsePressure("abc"))
    }

    @Test
    fun parseWeightAcceptsRangeAndRoundsToTwoDecimals() {
        assertEquals(0.0, ValidationPolicy.parseWeightKg("0") ?: -1.0, 0.0)
        assertEquals(100.0, ValidationPolicy.parseWeightKg("100") ?: -1.0, 0.0)
        assertEquals(64.24, ValidationPolicy.parseWeightKg("64.235") ?: -1.0, 0.0)
    }

    @Test
    fun parseWeightRejectsInvalidOrOutOfRangeValues() {
        assertNull(ValidationPolicy.parseWeightKg("-0.1"))
        assertNull(ValidationPolicy.parseWeightKg("100.1"))
        assertNull(ValidationPolicy.parseWeightKg("abc"))
    }

    @Test
    fun isValidTimeAcceptsTwentyFourHourHHmmValues() {
        assertTrue(ValidationPolicy.isValidTime("00:00"))
        assertTrue(ValidationPolicy.isValidTime("8:30"))
        assertTrue(ValidationPolicy.isValidTime("08:30"))
        assertTrue(ValidationPolicy.isValidTime("23:59"))
    }

    @Test
    fun isValidTimeRejectsMalformedOrOutOfRangeValues() {
        assertFalse(ValidationPolicy.isValidTime("24:00"))
        assertFalse(ValidationPolicy.isValidTime("08:60"))
        assertFalse(ValidationPolicy.isValidTime("08-30"))
    }

}
