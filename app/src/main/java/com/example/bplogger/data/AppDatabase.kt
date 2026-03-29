package com.example.bplogger.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DailyHealthRecord::class, DailyNote::class, NotificationSettings::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bpDao(): BpDao
}
