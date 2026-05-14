package com.pulselog.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DailyHealthRecord::class, NotificationSettings::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bpDao(): BpDao
}
