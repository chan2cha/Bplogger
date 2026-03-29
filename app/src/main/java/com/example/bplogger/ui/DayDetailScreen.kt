package com.example.bplogger.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.bplogger.data.DayRecordStatus
import java.time.format.DateTimeFormatter

/**
 * 날짜별 세부 관리 화면.
 * 빠른 입력에서 생략한 삭제와 메모 편집을 담당한다.
 */
@Composable
internal fun DayDetailScreen(vm: BpViewModel, onBack: () -> Unit) {
    val focusManager = LocalFocusManager.current
    val selectedDate by vm.selectedDate.collectAsState()
    val record by vm.selectedRecord.collectAsState()
    val note by vm.selectedNote.collectAsState()

    var morningSys by remember { mutableStateOf("") }
    var morningDia by remember { mutableStateOf("") }
    var eveningSys by remember { mutableStateOf("") }
    var eveningDia by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    // 선택 날짜가 바뀌면 상세 화면의 로컬 입력 상태를 저장값으로 초기화한다.
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
    LaunchedEffect(selectedDate, note?.note) {
        noteText = note?.note.orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize()
            .imePadding()
            .clearFocusOnTap(focusManager)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onBack) { Text("뒤로") }
                DetailSectionBadge(label = "DAY DETAIL")
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PremiumInk
                )
            }
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.7f), androidx.compose.foundation.shape.RoundedCornerShape(99.dp))
                    .border(1.dp, CalendarGridLine, androidx.compose.foundation.shape.RoundedCornerShape(99.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = statusText(record?.status() ?: DayRecordStatus.NONE),
                    style = MaterialTheme.typography.labelMedium,
                    color = PremiumInk
                )
            }
        }

        MeasurementCard(
            title = "아침 혈압",
            currentValue = record?.morningSystolic?.let { "${it} / ${record?.morningDiastolic}" } ?: "기록 없음",
            measuredAt = record?.morningMeasuredAtEpochMs?.let(::formatDateTime),
            primaryInput = morningSys,
            secondaryInput = morningDia,
            onPrimaryInputChange = { morningSys = it.filter(Char::isDigit) },
            onSecondaryInputChange = { morningDia = it.filter(Char::isDigit) },
            primaryLabel = "수축기",
            secondaryLabel = "이완기",
            onSave = { vm.saveMorning(morningSys, morningDia) },
            onDelete = if (record?.morningSystolic != null) vm::deleteMorning else null
        )

        MeasurementCard(
            title = "저녁 혈압",
            currentValue = record?.eveningSystolic?.let { "${it} / ${record?.eveningDiastolic}" } ?: "기록 없음",
            measuredAt = record?.eveningMeasuredAtEpochMs?.let(::formatDateTime),
            primaryInput = eveningSys,
            secondaryInput = eveningDia,
            onPrimaryInputChange = { eveningSys = it.filter(Char::isDigit) },
            onSecondaryInputChange = { eveningDia = it.filter(Char::isDigit) },
            primaryLabel = "수축기",
            secondaryLabel = "이완기",
            onSave = { vm.saveEvening(eveningSys, eveningDia) },
            onDelete = if (record?.eveningSystolic != null) vm::deleteEvening else null
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardTint)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("체중", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = WarmAccent)
                Text(
                    text = "현재 값: ${record?.weightKg?.let(::formatWeight)?.plus("kg") ?: "기록 없음"}",
                    color = PremiumInk
                )
                record?.weightMeasuredAtEpochMs?.let {
                    Text("측정 시각: ${formatDateTime(it)}", color = PremiumSubtle)
                }
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("체중(kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.saveWeight(weight) },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmAccent)
                    ) {
                        Text(if (record?.weightKg == null) "체중 저장" else "체중 수정")
                    }
                    if (record?.weightKg != null) {
                        TextButton(onClick = vm::deleteWeight) {
                            Text("삭제")
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PremiumPanel)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("메모", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = WarmAccent)
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    label = { Text("날짜 메모") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.saveNote(noteText) },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmAccent)
                    ) {
                        Text("메모 저장")
                    }
                    if (note?.note?.isNotBlank() == true) {
                        TextButton(onClick = vm::deleteNote) {
                            Text("삭제")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 상세 화면의 측정 카드 공통 UI.
 */
@Composable
private fun MeasurementCard(
    title: String,
    currentValue: String,
    measuredAt: String?,
    primaryInput: String,
    secondaryInput: String,
    onPrimaryInputChange: (String) -> Unit,
    onSecondaryInputChange: (String) -> Unit,
    primaryLabel: String,
    secondaryLabel: String,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardTint)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = WarmAccent)
            Text("현재 값: $currentValue", color = PremiumInk)
            measuredAt?.let { Text("측정 시각: $it", color = PremiumSubtle) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = primaryInput,
                    onValueChange = onPrimaryInputChange,
                    label = { Text(primaryLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = secondaryInput,
                    onValueChange = onSecondaryInputChange,
                    label = { Text(secondaryLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = WarmAccent)
                ) {
                    Text(if (currentValue == "기록 없음") "저장" else "수정")
                }
                onDelete?.let {
                    TextButton(onClick = it) {
                        Text("삭제")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSectionBadge(label: String) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.7f), androidx.compose.foundation.shape.RoundedCornerShape(99.dp))
            .border(1.dp, CalendarGridLine, androidx.compose.foundation.shape.RoundedCornerShape(99.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = WarmAccent
        )
    }
}
