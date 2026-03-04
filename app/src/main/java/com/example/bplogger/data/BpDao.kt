package com.example.bplogger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface BpDao {

    // 날짜+slot이 unique → 같은 키면 REPLACE로 덮어쓰기
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: BpRecord)

    @Query("SELECT * FROM bp_records WHERE dateIso = :dateIso ORDER BY slot ASC")
    fun observeByDate(dateIso: String): Flow<List<BpRecord>>
}