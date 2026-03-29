package com.example.bplogger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.bplogger.data.CalendarDayStatus
import com.example.bplogger.data.DayRecordStatus
import com.example.bplogger.data.GraphPoint
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class MainTab {
    CALENDAR,
    GRAPH,
    SETTINGS
}

@Composable
fun BloodPressureScreen(vm: BpViewModel) {
    var selectedTab by remember { mutableStateOf(MainTab.CALENDAR) }
    var detailOpen by remember { mutableStateOf(false) }
    val message by vm.message.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        if (detailOpen) {
            DayDetailScreen(
                vm = vm,
                onBack = { detailOpen = false }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Header(title = "Bplogger")

                message?.let {
                    MessageBanner(
                        message = it,
                        onDismiss = vm::clearMessage
                    )
                }

                TabRow(selectedTabIndex = selectedTab.ordinal) {
                    MainTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    when (tab) {
                                        MainTab.CALENDAR -> "캘린더"
                                        MainTab.GRAPH -> "그래프"
                                        MainTab.SETTINGS -> "설정"
                                    }
                                )
                            }
                        )
                    }
                }

                when (selectedTab) {
                    MainTab.CALENDAR -> CalendarScreen(
                        vm = vm,
                        onOpenDetail = { detailOpen = true }
                    )

                    MainTab.GRAPH -> GraphScreen(vm = vm)
                    MainTab.SETTINGS -> SettingsScreen(vm = vm)
                }
            }
        }
    }
}

@Composable
private fun Header(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MessageBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message, modifier = Modifier.width(0.dp).fillMaxWidth())
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    }
}

@Composable
private fun CalendarScreen(vm: BpViewModel, onOpenDetail: () -> Unit) {
    val currentMonth by vm.currentMonth.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val statuses by vm.monthStatuses.collectAsState()
    val statusMap = remember(statuses) { statuses.associateBy { it.dateIso } }
    val selectedStatus = statusMap[selectedDate.toString()]

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            MonthHeader(
                month = currentMonth,
                onPrevious = vm::previousMonth,
                onNext = vm::nextMonth
            )
        }

        item {
            CalendarGrid(
                month = currentMonth,
                selectedDate = selectedDate,
                statuses = statusMap,
                onDateClick = vm::setSelectedDate
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${selectedDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))} 선택됨",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("상태: ${statusText(selectedStatus?.status ?: DayRecordStatus.NONE)}")
                    Text("체중 기록: ${if (selectedStatus?.hasWeight == true) "있음" else "없음"}")
                    Text("메모: ${if (selectedStatus?.hasNote == true) "있음" else "없음"}")
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onOpenDetail,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("선택 날짜 상세 보기")
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPrevious) { Text("이전") }
            Text(
                text = "${month.year}년 ${month.month.getDisplayName(TextStyle.FULL, Locale.KOREAN)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onNext) { Text("다음") }
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    statuses: Map<String, CalendarDayStatus>,
    onDateClick: (LocalDate) -> Unit
) {
    val cells = remember(month) { buildCalendarCells(month) }
    val weekdayLabels = listOf("월", "화", "수", "목", "금", "토", "일")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.width(40.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                week.forEach { date ->
                    val status = statuses[date.toString()]?.status ?: DayRecordStatus.NONE
                    val isSelected = date == selectedDate
                    CalendarDayCell(
                        date = date,
                        inCurrentMonth = date.month == month.month,
                        status = status,
                        isSelected = isSelected,
                        onClick = { onDateClick(date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    inCurrentMonth: Boolean,
    status: DayRecordStatus,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when (status) {
        DayRecordStatus.NONE -> MaterialTheme.colorScheme.surfaceVariant
        DayRecordStatus.MORNING_ONLY -> Color(0xFFFFE0B2)
        DayRecordStatus.EVENING_ONLY -> Color(0xFFBBDEFB)
        DayRecordStatus.BOTH -> Color(0xFFC8E6C9)
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(10.dp)
            )
            .background(containerColor.copy(alpha = if (inCurrentMonth) 1f else 0.4f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${date.dayOfMonth}일 ${statusText(status)}"
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = if (inCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun DayDetailScreen(vm: BpViewModel, onBack: () -> Unit) {
    val selectedDate by vm.selectedDate.collectAsState()
    val record by vm.selectedRecord.collectAsState()
    val note by vm.selectedNote.collectAsState()

    var morningSys by remember { mutableStateOf("") }
    var morningDia by remember { mutableStateOf("") }
    var eveningSys by remember { mutableStateOf("") }
    var eveningDia by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    LaunchedEffect(selectedDate, record?.morningSystolic, record?.morningDiastolic) {
        morningSys = record?.morningSystolic?.toString().orEmpty()
        morningDia = record?.morningDiastolic?.toString().orEmpty()
    }
    LaunchedEffect(selectedDate, record?.eveningSystolic, record?.eveningDiastolic) {
        eveningSys = record?.eveningSystolic?.toString().orEmpty()
        eveningDia = record?.eveningDiastolic?.toString().orEmpty()
    }
    LaunchedEffect(selectedDate, record?.weightKg) {
        weight = record?.weightKg?.let { formatWeight(it) }.orEmpty()
    }
    LaunchedEffect(selectedDate, note?.note) {
        noteText = note?.note.orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                TextButton(onClick = onBack) { Text("뒤로") }
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(statusText(record?.status() ?: DayRecordStatus.NONE))
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

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("체중", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("현재 값: ${record?.weightKg?.let(::formatWeight)?.plus("kg") ?: "기록 없음"}")
                record?.weightMeasuredAtEpochMs?.let { Text("측정 시각: ${formatDateTime(it)}") }
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("체중(kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.saveWeight(weight) }) {
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

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("메모", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    label = { Text("날짜 메모") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.saveNote(noteText) }) {
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("현재 값: $currentValue")
            measuredAt?.let { Text("측정 시각: $it") }
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
                Button(onClick = onSave) {
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
private fun GraphScreen(vm: BpViewModel) {
    val rangeDays by vm.graphRangeDays.collectAsState()
    val points by vm.graphPoints.collectAsState()

    val systolicSeries = points.map { averageOfNotNull(it.morningSystolic, it.eveningSystolic) }
    val diastolicSeries = points.map { averageOfNotNull(it.morningDiastolic, it.eveningDiastolic) }
    val weightSeries = points.map { it.weightKg }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.setGraphRangeDays(7) }, enabled = rangeDays != 7) {
                    Text("최근 7일")
                }
                Button(onClick = { vm.setGraphRangeDays(30) }, enabled = rangeDays != 30) {
                    Text("최근 30일")
                }
            }
        }

        item {
            ChartCard(
                title = "혈압 그래프",
                subtitle = "수축기와 이완기의 최근 변화"
            ) {
                if (points.isEmpty()) {
                    Text("표시할 혈압 데이터가 없습니다.")
                } else {
                    MultiLineChart(
                        labels = points.map { it.dateIso.substring(5) },
                        series = listOf(systolicSeries, diastolicSeries),
                        colors = listOf(Color(0xFFE67E22), Color(0xFF3498DB))
                    )
                }
            }
        }

        item {
            ChartCard(
                title = "체중 그래프",
                subtitle = "최근 체중 기록 변화"
            ) {
                if (weightSeries.all { it == null }) {
                    Text("표시할 체중 데이터가 없습니다.")
                } else {
                    MultiLineChart(
                        labels = points.map { it.dateIso.substring(5) },
                        series = listOf(weightSeries),
                        colors = listOf(Color(0xFF2E7D32))
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun MultiLineChart(
    labels: List<String>,
    series: List<List<Double?>>,
    colors: List<Color>
) {
    val allValues = series.flatten().filterNotNull()
    if (allValues.isEmpty() || labels.size < 2) {
        Text("데이터가 부족합니다.")
        return
    }

    val minValue = allValues.minOrNull() ?: return
    val maxValue = allValues.maxOrNull() ?: return
    val range = (maxValue - minValue).takeIf { it > 0 } ?: 1.0

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        val usableWidth = size.width
        val usableHeight = size.height
        val stepX = usableWidth / (labels.lastIndex.coerceAtLeast(1))

        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(0f, usableHeight),
            end = Offset(usableWidth, usableHeight),
            strokeWidth = 2f
        )

        series.forEachIndexed { index, values ->
            val path = Path()
            var started = false
            values.forEachIndexed { valueIndex, value ->
                if (value == null) return@forEachIndexed
                val x = stepX * valueIndex
                val normalized = ((value - minValue) / range).toFloat()
                val y = usableHeight - (normalized * usableHeight)
                if (!started) {
                    path.moveTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y)
                }
            }
            if (started) {
                drawPath(
                    path = path,
                    color = colors[index],
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(labels.first(), style = MaterialTheme.typography.bodySmall)
        Text(labels.last(), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingsScreen(vm: BpViewModel) {
    val settings by vm.notificationSettings.collectAsState()

    var morningEnabled by remember { mutableStateOf(settings.morningEnabled) }
    var morningTime by remember { mutableStateOf(settings.morningTime) }
    var eveningEnabled by remember { mutableStateOf(settings.eveningEnabled) }
    var eveningTime by remember { mutableStateOf(settings.eveningTime) }
    var repeatEnabled by remember { mutableStateOf(settings.repeatEnabled) }
    var repeatCount by remember { mutableStateOf(settings.repeatCount.toString()) }

    LaunchedEffect(settings) {
        morningEnabled = settings.morningEnabled
        morningTime = settings.morningTime
        eveningEnabled = settings.eveningEnabled
        eveningTime = settings.eveningTime
        repeatEnabled = settings.repeatEnabled
        repeatCount = settings.repeatCount.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("알림 설정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                SettingSwitchRow(
                    label = "아침 알림 사용",
                    checked = morningEnabled,
                    onCheckedChange = { morningEnabled = it }
                )
                OutlinedTextField(
                    value = morningTime,
                    onValueChange = { morningTime = it },
                    label = { Text("아침 알림 시간 (HH:mm)") },
                    modifier = Modifier.fillMaxWidth()
                )
                SettingSwitchRow(
                    label = "저녁 알림 사용",
                    checked = eveningEnabled,
                    onCheckedChange = { eveningEnabled = it }
                )
                OutlinedTextField(
                    value = eveningTime,
                    onValueChange = { eveningTime = it },
                    label = { Text("저녁 알림 시간 (HH:mm)") },
                    modifier = Modifier.fillMaxWidth()
                )
                SettingSwitchRow(
                    label = "재알림 사용",
                    checked = repeatEnabled,
                    onCheckedChange = { repeatEnabled = it }
                )
                OutlinedTextField(
                    value = repeatCount,
                    onValueChange = { repeatCount = it.filter(Char::isDigit) },
                    label = { Text("재알림 횟수") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        vm.saveNotificationSettings(
                            morningEnabled = morningEnabled,
                            morningTime = morningTime,
                            eveningEnabled = eveningEnabled,
                            eveningTime = eveningTime,
                            repeatEnabled = repeatEnabled,
                            repeatCountInput = repeatCount
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("설정 저장")
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun buildCalendarCells(month: YearMonth): List<LocalDate> {
    val firstDay = month.atDay(1)
    val offset = firstDay.dayOfWeek.value - 1
    val startDate = firstDay.minusDays(offset.toLong())
    return List(42) { index -> startDate.plusDays(index.toLong()) }
}

private fun statusText(status: DayRecordStatus): String = when (status) {
    DayRecordStatus.NONE -> "기록 없음"
    DayRecordStatus.MORNING_ONLY -> "아침만 기록"
    DayRecordStatus.EVENING_ONLY -> "저녁만 기록"
    DayRecordStatus.BOTH -> "아침/저녁 모두 기록"
}

private fun formatDateTime(epochMs: Long): String {
    val zoned = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
    return zoned.format(DateTimeFormatter.ofPattern("MM.dd HH:mm"))
}

private fun formatWeight(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
}

private fun averageOfNotNull(first: Int?, second: Int?): Double? {
    val values = listOfNotNull(first, second)
    if (values.isEmpty()) return null
    return values.average()
}
