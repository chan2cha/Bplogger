package com.example.bplogger.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "bp_records",
    indices = [Index(value = ["dateIso", "slot"], unique = true)]
)
data class BpRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateIso: String,          // yyyy-MM-dd
    val slot: String,             // "AM" or "PM"
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int? = null,
    val note: String? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    companion object {
        fun todayIso(): String = LocalDate.now().toString()
    }
}