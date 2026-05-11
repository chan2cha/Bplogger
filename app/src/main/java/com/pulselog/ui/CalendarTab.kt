package com.pulselog.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 메인 캘린더 탭.
 * 이 파일은 화면 상태를 모으고, 실제 UI 블록은 캘린더/빠른입력 컴포넌트로 위임한다.
 */
@Composable
internal fun CalendarScreen(
    vm: BpViewModel,
    onOpenEntry: () -> Unit
) {
    val currentMonth by vm.currentMonth.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val statuses by vm.monthStatuses.collectAsState()
    val record by vm.selectedRecord.collectAsState()
    val statusMap = remember(statuses) { statuses.associateBy { it.dateIso } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CompactCalendarCard(
                month = currentMonth,
                selectedDate = selectedDate,
                selectedRecord = record,
                statuses = statusMap,
                onPrevious = vm::previousMonth,
                onNext = vm::nextMonth,
                onDateClick = vm::setSelectedDate,
                onOpenEntry = onOpenEntry
            )
        }
    }
}
