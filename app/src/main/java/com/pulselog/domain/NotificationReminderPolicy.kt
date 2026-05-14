package com.pulselog.domain

import com.pulselog.data.DailyHealthRecord

object NotificationReminderPolicy {
    const val SLOT_MORNING = "morning"
    const val SLOT_EVENING = "evening"

    fun shouldShowReminder(slot: String, record: DailyHealthRecord?): Boolean {
        return when (slot) {
            SLOT_MORNING -> record?.morningSystolic == null || record.morningDiastolic == null
            SLOT_EVENING -> record?.eveningSystolic == null || record.eveningDiastolic == null
            else -> false
        }
    }
}
