package com.pulselog.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.pulselog.data.AppDatabase
import com.pulselog.data.AppDatabaseMigrations
import com.pulselog.data.NotificationSettings
import com.pulselog.domain.SystemClockProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PulseLogBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = loadSettings(context)
                if (settings != null) {
                    AlarmNotificationScheduler(context, SystemClockProvider()).apply(settings)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun loadSettings(context: Context): NotificationSettings? {
        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "bp-db"
        )
            .addMigrations(*AppDatabaseMigrations.ALL)
            .build()

        return try {
            db.bpDao().getSettings()
        } finally {
            db.close()
        }
    }
}
