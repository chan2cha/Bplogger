package com.pulselog.domain

import com.pulselog.data.DailyHealthRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationReminderPolicyTest {
    @Test
    fun shouldShowReminder_returnsTrueWhenMorningRecordIsMissing() {
        val record = record(eveningSystolic = 120, eveningDiastolic = 80)

        assertTrue(NotificationReminderPolicy.shouldShowReminder(NotificationReminderPolicy.SLOT_MORNING, record))
    }

    @Test
    fun shouldShowReminder_returnsFalseWhenMorningRecordExists() {
        val record = record(morningSystolic = 118, morningDiastolic = 76)

        assertFalse(NotificationReminderPolicy.shouldShowReminder(NotificationReminderPolicy.SLOT_MORNING, record))
    }

    @Test
    fun shouldShowReminder_returnsFalseForUnknownSlot() {
        assertFalse(NotificationReminderPolicy.shouldShowReminder("unknown", null))
    }

    private fun record(
        morningSystolic: Int? = null,
        morningDiastolic: Int? = null,
        eveningSystolic: Int? = null,
        eveningDiastolic: Int? = null
    ): DailyHealthRecord {
        return DailyHealthRecord(
            dateIso = "2026-05-14",
            morningSystolic = morningSystolic,
            morningDiastolic = morningDiastolic,
            eveningSystolic = eveningSystolic,
            eveningDiastolic = eveningDiastolic,
            createdAtEpochMs = 1L,
            updatedAtEpochMs = 1L
        )
    }
}
