package com.pulselog.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pulselog.data.NotificationSettings
import com.pulselog.domain.ClockProvider
import com.pulselog.domain.NotificationSchedulePolicy
import com.pulselog.domain.NotificationScheduler

class AlarmNotificationScheduler(
    private val context: Context,
    private val clockProvider: ClockProvider
) : NotificationScheduler {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    override suspend fun apply(settings: NotificationSettings) {
        cancelAll()

        if (settings.morningEnabled) {
            scheduleNextBaseSlot(SLOT_MORNING, settings.morningTime)
        }
        if (settings.eveningEnabled) {
            scheduleNextBaseSlot(SLOT_EVENING, settings.eveningTime)
        }
    }

    override suspend fun cancelAll() {
        listOf(SLOT_MORNING, SLOT_EVENING).forEach { slot ->
            for (repeatIndex in 0..MAX_REPEAT_COUNT) {
                alarmManager.cancel(pendingIntent(slot, repeatIndex))
            }
        }
    }

    fun scheduleNextBaseSlot(slot: String, settings: NotificationSettings) {
        when (slot) {
            SLOT_MORNING -> if (settings.morningEnabled) scheduleNextBaseSlot(slot, settings.morningTime)
            SLOT_EVENING -> if (settings.eveningEnabled) scheduleNextBaseSlot(slot, settings.eveningTime)
        }
    }

    fun scheduleRepeatsFromNow(slot: String, settings: NotificationSettings) {
        if (!settings.repeatEnabled) return

        val repeatCount = settings.repeatCount.coerceAtMost(MAX_REPEAT_COUNT)
        if (repeatCount <= 0) return

        val now = clockProvider.nowEpochMs()
        for (repeatIndex in 1..repeatCount) {
            setAlarm(
                slot = slot,
                repeatIndex = repeatIndex,
                triggerAtMillis = NotificationSchedulePolicy.repeatTriggerEpochMs(now, repeatIndex)
            )
        }
    }

    private fun scheduleNextBaseSlot(slot: String, time: String) {
        val triggerAtMillis = NotificationSchedulePolicy.nextTriggerEpochMs(
            time = time,
            nowEpochMs = clockProvider.nowEpochMs(),
            zoneId = clockProvider.zoneId()
        )
        setAlarm(slot, repeatIndex = 0, triggerAtMillis = triggerAtMillis)
    }

    private fun setAlarm(slot: String, repeatIndex: Int, triggerAtMillis: Long) {
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent(slot, repeatIndex)
        )
    }

    private fun pendingIntent(slot: String, repeatIndex: Int): PendingIntent {
        val intent = Intent(appContext, PulseLogNotificationReceiver::class.java)
            .setAction(PulseLogNotificationReceiver.ACTION_REMINDER)
            .putExtra(PulseLogNotificationReceiver.EXTRA_SLOT, slot)
            .putExtra(PulseLogNotificationReceiver.EXTRA_REPEAT_INDEX, repeatIndex)

        return PendingIntent.getBroadcast(
            appContext,
            requestCode(slot, repeatIndex),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(slot: String, repeatIndex: Int): Int {
        val slotOffset = if (slot == SLOT_MORNING) 100 else 200
        return slotOffset + repeatIndex
    }

    companion object {
        const val SLOT_MORNING = "morning"
        const val SLOT_EVENING = "evening"
        const val MAX_REPEAT_COUNT = 10
    }
}
