package com.example.bplogger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bplogger.data.BpRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BpViewModel(private val repo: BpRepository) : ViewModel() {

    // 오늘 날짜 기준 목록
    private fun todayIso(): String = java.time.LocalDate.now().toString()

    val todayRecords: StateFlow<List<BpRecord>> =
        repo.observeByDate(todayIso())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveBp(sys: Int, dia: Int, pulse: Int?, note: String?) {
        val ds = deriveDateSlotFromDeviceTime()

        viewModelScope.launch {
            repo.upsert(
                BpRecord(
                    dateIso = ds.dateIso,
                    slot = ds.slot,
                    systolic = sys,
                    diastolic = dia,
                    pulse = pulse,
                    note = note?.takeIf { it.isNotBlank() }
                )
            )
        }
    }
}