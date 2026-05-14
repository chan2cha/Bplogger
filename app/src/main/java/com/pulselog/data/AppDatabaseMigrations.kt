package com.pulselog.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseMigrations {
    /**
     * Version 3 is the first exported schema baseline kept in this repository.
     * Add future migrations here and cover them in AppDatabaseMigrationTest.
     */
    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS daily_notes")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_3_4)
}
