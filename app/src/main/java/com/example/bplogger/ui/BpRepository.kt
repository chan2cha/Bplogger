package com.example.bplogger.ui

import com.example.bplogger.data.BpDao
import com.example.bplogger.data.BpRecord
import kotlinx.coroutines.flow.Flow

class BpRepository(private val dao: BpDao) {
    suspend fun upsert(record: BpRecord) = dao.upsert(record)
    fun observeByDate(dateIso: String): Flow<List<BpRecord>> = dao.observeByDate(dateIso)
}