package com.example.bplogger.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BpRecord::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bpDao(): BpDao
}