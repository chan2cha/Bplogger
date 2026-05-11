package com.pulselog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BpDao {

    @Query("SELECT * FROM daily_health_records ORDER BY dateIso ASC")
    fun observeAllRecords(): Flow<List<DailyHealthRecord>>

    @Query("SELECT * FROM daily_health_records WHERE dateIso = :dateIso")
    fun observeRecord(dateIso: String): Flow<DailyHealthRecord?>

    @Query("SELECT * FROM daily_health_records WHERE dateIso = :dateIso")
    suspend fun getRecord(dateIso: String): DailyHealthRecord?

    @Query("SELECT * FROM daily_health_records ORDER BY dateIso ASC")
    suspend fun getAllRecords(): List<DailyHealthRecord>

    @Query("SELECT * FROM daily_health_records WHERE dateIso >= :fromDateIso ORDER BY dateIso ASC")
    suspend fun getRecordsFrom(fromDateIso: String): List<DailyHealthRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(record: DailyHealthRecord)

    @Query("DELETE FROM daily_health_records WHERE dateIso = :dateIso")
    suspend fun deleteRecord(dateIso: String)

    @Query("SELECT * FROM daily_notes ORDER BY dateIso ASC")
    fun observeAllNotes(): Flow<List<DailyNote>>

    @Query("SELECT * FROM daily_notes WHERE dateIso = :dateIso")
    fun observeNote(dateIso: String): Flow<DailyNote?>

    @Query("SELECT * FROM daily_notes WHERE dateIso = :dateIso")
    suspend fun getNote(dateIso: String): DailyNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: DailyNote)

    @Query("DELETE FROM daily_notes WHERE dateIso = :dateIso")
    suspend fun deleteNote(dateIso: String)

    @Query("SELECT * FROM notification_settings WHERE id = 1")
    fun observeSettings(): Flow<NotificationSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: NotificationSettings)
}
