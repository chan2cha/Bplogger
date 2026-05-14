package com.pulselog

import android.content.Context
import androidx.room.Room
import com.pulselog.data.AppDatabase
import com.pulselog.data.AppDatabaseMigrations
import com.pulselog.domain.ClockProvider
import com.pulselog.domain.NotificationScheduler
import com.pulselog.domain.SystemClockProvider
import com.pulselog.notifications.AlarmNotificationScheduler
import com.pulselog.ui.BpRepository
import com.pulselog.ui.BpViewModel

class AppGraph(context: Context) {
    private val appContext = context.applicationContext

    private val db: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "bp-db"
    )
        .addMigrations(*AppDatabaseMigrations.ALL)
        .build()

    val clockProvider: ClockProvider = SystemClockProvider()
    val notificationScheduler: NotificationScheduler = AlarmNotificationScheduler(
        context = appContext,
        clockProvider = clockProvider
    )
    val repository = BpRepository(
        dao = db.bpDao(),
        clockProvider = clockProvider
    )

    fun createViewModel(): BpViewModel {
        return BpViewModel(
            repo = repository,
            clockProvider = clockProvider,
            notificationScheduler = notificationScheduler
        )
    }
}
