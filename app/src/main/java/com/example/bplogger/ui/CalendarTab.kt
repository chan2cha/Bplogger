package com.example.bplogger.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.example.bplogger.data.DayRecordStatus

/**
 * 메인 캘린더 탭.
 * 이 파일은 화면 상태를 모으고, 실제 UI 블록은 캘린더/빠른입력 컴포넌트로 위임한다.
 */
@Composable
internal fun CalendarScreen(vm: BpViewModel, onOpenDetail: () -> Unit) {
    val focusManager = LocalFocusManager.current
    val currentMonth by vm.currentMonth.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val statuses by vm.monthStatuses.collectAsState()
    val record by vm.selectedRecord.collectAsState()
    val note by vm.selectedNote.collectAsState()
    val statusMap = remember(statuses) { statuses.associateBy { it.dateIso } }
    val selectedStatus = statusMap[selectedDate.toString()]

    var morningSys by remember { mutableStateOf("") }
    var morningDia by remember { mutableStateOf("") }
    var eveningSys by remember { mutableStateOf("") }
    var eveningDia by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    // 선택 날짜 또는 저장 데이터가 바뀌면 입력 폼을 해당 날짜 값으로 다시 동기화한다.
    LaunchedEffect(selectedDate, record?.morningSystolic, record?.morningDiastolic) {
        morningSys = record?.morningSystolic?.toString().orEmpty()
        morningDia = record?.morningDiastolic?.toString().orEmpty()
    }
    LaunchedEffect(selectedDate, record?.eveningSystolic, record?.eveningDiastolic) {
        eveningSys = record?.eveningSystolic?.toString().orEmpty()
        eveningDia = record?.eveningDiastolic?.toString().orEmpty()
    }
    LaunchedEffect(selectedDate, record?.weightKg) {
        weight = record?.weightKg?.let(::formatWeight).orEmpty()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize()
            .imePadding()
            .clearFocusOnTap(focusManager)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CompactCalendarCard(
                month = currentMonth,
                selectedDate = selectedDate,
                notePreview = note?.note,
                statuses = statusMap,
                onPrevious = vm::previousMonth,
                onNext = vm::nextMonth,
                onDateClick = vm::setSelectedDate,
                onOpenDetail = onOpenDetail
            )
        }

        item {
            QuickEntryCard(
                selectedDate = selectedDate,
                record = record,
                morningSys = morningSys,
                morningDia = morningDia,
                eveningSys = eveningSys,
                eveningDia = eveningDia,
                weight = weight,
                onMorningSysChange = { morningSys = it.filter(Char::isDigit) },
                onMorningDiaChange = { morningDia = it.filter(Char::isDigit) },
                onEveningSysChange = { eveningSys = it.filter(Char::isDigit) },
                onEveningDiaChange = { eveningDia = it.filter(Char::isDigit) },
                onWeightChange = { weight = it.filter { ch -> ch.isDigit() || ch == '.' } },
                onSaveMorning = { vm.saveMorning(morningSys, morningDia) },
                onSaveEvening = { vm.saveEvening(eveningSys, eveningDia) },
                onSaveWeight = { vm.saveWeight(weight) }
            )
        }
    }
}
