package com.pulselog.notifications

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.pulselog.MainActivity
import com.pulselog.R
import com.pulselog.data.AppDatabase
import com.pulselog.data.AppDatabaseMigrations
import com.pulselog.data.DailyHealthRecord
import com.pulselog.data.NotificationSettings
import com.pulselog.domain.NotificationReminderPolicy
import com.pulselog.domain.SystemClockProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PulseLogNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val clockProvider = SystemClockProvider()
                val scheduler = AlarmNotificationScheduler(context, clockProvider)
                val state = loadNotificationState(context, clockProvider.today().toString())
                when (intent.action) {
                    ACTION_REMINDER -> {
                        val slot = intent.getStringExtra(EXTRA_SLOT).orEmpty()
                        val repeatIndex = intent.getIntExtra(EXTRA_REPEAT_INDEX, 0)
                        if (repeatIndex > 0) return@launch

                        if (NotificationReminderPolicy.shouldShowReminder(slot, state.todayRecord)) {
                            showReminder(context, slot)
                        }
                        scheduler.scheduleNextBaseSlot(slot, state.settings)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun loadNotificationState(context: Context, todayIso: String): NotificationState {
        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "bp-db"
        )
            .addMigrations(*AppDatabaseMigrations.ALL)
            .build()

        return try {
            NotificationState(
                settings = db.bpDao().getSettings() ?: NotificationSettings(),
                todayRecord = db.bpDao().getRecord(todayIso)
            )
        } finally {
            db.close()
        }
    }

    @SuppressLint("MissingPermission")
    private fun showReminder(context: Context, slot: String) {
        if (!canPostNotifications(context)) return

        createChannel(context)
        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = android.app.PendingIntent.getActivity(
            context,
            1,
            openAppIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (slot == AlarmNotificationScheduler.SLOT_EVENING) {
            "저녁 기록 시간입니다"
        } else {
            "아침 기록 시간입니다"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pulse_log)
            .setContentTitle(title)
            .setContentText("혈압과 체중 기록을 확인하세요.")
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId(slot), notification)
        } catch (_: SecurityException) {
            // Permission can be revoked after the explicit check on Android 13+.
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "기록 알림",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "혈압과 체중 기록 시간을 알려줍니다."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun notificationId(slot: String): Int {
        return if (slot == AlarmNotificationScheduler.SLOT_EVENING) 2001 else 1001
    }

    companion object {
        const val ACTION_REMINDER = "com.pulselog.notifications.REMINDER"
        const val EXTRA_SLOT = "slot"
        const val EXTRA_REPEAT_INDEX = "repeat_index"
        private const val CHANNEL_ID = "pulse_log_record_reminders"
    }
}

private data class NotificationState(
    val settings: NotificationSettings,
    val todayRecord: DailyHealthRecord?
)
