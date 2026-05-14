package com.pulselog.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun version3To4DropsNotesAndPreservesCurrentData() {
        helper.createDatabase(TEST_DB, 3).use { db ->
            db.execSQL(
                """
                INSERT INTO daily_health_records (
                    dateIso,
                    morningSystolic,
                    morningDiastolic,
                    morningMeasuredAtEpochMs,
                    eveningSystolic,
                    eveningDiastolic,
                    eveningMeasuredAtEpochMs,
                    weightKg,
                    weightMeasuredAtEpochMs,
                    createdAtEpochMs,
                    updatedAtEpochMs
                ) VALUES (
                    '2026-05-14',
                    121,
                    79,
                    10,
                    124,
                    82,
                    20,
                    65.4,
                    30,
                    1,
                    2
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO daily_notes (
                    dateIso,
                    note,
                    createdAtEpochMs,
                    updatedAtEpochMs
                ) VALUES (
                    '2026-05-14',
                    'memo',
                    1,
                    2
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO notification_settings (
                    id,
                    morningEnabled,
                    morningTime,
                    eveningEnabled,
                    eveningTime,
                    repeatEnabled,
                    repeatCount,
                    updatedAtEpochMs
                ) VALUES (
                    1,
                    1,
                    '08:00',
                    1,
                    '20:00',
                    1,
                    2,
                    3
                )
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            *AppDatabaseMigrations.ALL
        ).use { db ->
            assertEquals(1, db.countRows("daily_health_records"))
            assertEquals(1, db.countRows("notification_settings"))
            assertTrue(db.hasRecord("daily_health_records", "dateIso", "2026-05-14"))
            assertEquals(false, db.hasTable("daily_notes"))
        }
    }

    private fun SupportSQLiteDatabase.countRows(tableName: String): Int {
        query("SELECT COUNT(*) FROM $tableName").use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }

    private fun SupportSQLiteDatabase.hasRecord(
        tableName: String,
        columnName: String,
        value: String
    ): Boolean {
        query("SELECT COUNT(*) FROM $tableName WHERE $columnName = ?", arrayOf(value)).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0) == 1
        }
    }

    private fun SupportSQLiteDatabase.hasTable(tableName: String): Boolean {
        query("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(tableName)).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0) == 1
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
