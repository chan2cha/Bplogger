package com.pulselog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pulselog.data.CalendarDayStatus
import com.pulselog.data.DailyHealthRecord
import com.pulselog.data.DayRecordStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * 메인 월력 카드.
 * 날짜 상태 요약과 월 이동을 한 영역에 묶는다.
 */
@Composable
internal fun CompactCalendarCard(
    month: YearMonth,
    selectedDate: LocalDate,
    selectedRecord: DailyHealthRecord?,
    statuses: Map<String, CalendarDayStatus>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
    onOpenEntry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CalendarShell)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CalendarSectionBadge(label = "CALENDAR")
                    Text(
                        text = "${month.year}년 ${month.month.getDisplayName(TextStyle.FULL, Locale.KOREAN)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PremiumInk
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CalendarNavButton(onClick = onPrevious, isPrevious = true)
                    CalendarNavButton(onClick = onNext, isPrevious = false)
                }
            }

            SelectedDateSummaryHeader(
                selectedDate = selectedDate,
                record = selectedRecord,
                onOpenEntry = onOpenEntry
            )

            CalendarLegend()

            CalendarGrid(
                month = month,
                selectedDate = selectedDate,
                statuses = statuses,
                onDateClick = onDateClick
            )
        }
    }
}

@Composable
private fun SelectedDateSummaryHeader(
    selectedDate: LocalDate,
    record: DailyHealthRecord?,
    onOpenEntry: () -> Unit
) {
    val status = record?.status() ?: DayRecordStatus.NONE
    val compactText = isCompactTextMode()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PremiumPanel)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SelectedDateTitleRow(
                selectedDate = selectedDate,
                status = status,
                compact = compactText
            )

            RecordSummaryTiles(record = record, compact = compactText)

            if (record != null) {
                Text(
                    text = "마지막 수정 ${formatDateTime(record.updatedAtEpochMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PremiumSubtle
                )
            }

            Button(
                onClick = onOpenEntry,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmAccent)
            ) {
                Text(if (compactText) "입력" else "이 날짜 입력하기")
            }
        }
    }
}

@Composable
private fun SelectedDateTitleRow(
    selectedDate: LocalDate,
    status: DayRecordStatus,
    compact: Boolean
) {
    val titleBlock: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!compact) {
                Text(
                    text = "선택 날짜",
                    style = MaterialTheme.typography.labelSmall,
                    color = PremiumSubtle,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern(if (compact) "M/d E" else "M월 d일 E요일", Locale.KOREAN)),
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                color = PremiumInk,
                fontWeight = FontWeight.Bold
            )
        }
    }
    val statusBadge: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .background(statusColor(status).copy(alpha = 0.12f), androidx.compose.foundation.shape.RoundedCornerShape(99.dp))
                .border(1.dp, statusColor(status).copy(alpha = 0.35f), androidx.compose.foundation.shape.RoundedCornerShape(99.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusLegend(status = status)
            if (!compact) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = statusText(status),
                    style = MaterialTheme.typography.labelMedium,
                    color = PremiumInk,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            titleBlock()
            statusBadge()
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            titleBlock()
            statusBadge()
        }
    }
}

@Composable
private fun RecordSummaryTiles(record: DailyHealthRecord?, compact: Boolean) {
    val items = listOf(
        "아침 혈압" to formatBloodPressure(record?.morningSystolic, record?.morningDiastolic),
        "저녁 혈압" to formatBloodPressure(record?.eveningSystolic, record?.eveningDiastolic),
        "체중" to (record?.weightKg?.let { "${formatWeight(it)}kg" } ?: "기록 없음")
    )

    if (compact) {
        Column(
            modifier = Modifier.widthIn(max = 260.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { (label, value) ->
                RecordSummaryTile(
                    label = compactRecordLabel(label),
                    value = value,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { (label, value) ->
                RecordSummaryTile(
                    label = label,
                    value = value,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun compactRecordLabel(label: String): String {
    return when (label) {
        "아침 혈압" -> "아침"
        "저녁 혈압" -> "저녁"
        "체중" -> "kg"
        else -> label
    }
}

@Composable
private fun RecordSummaryTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(PremiumGlass, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .border(1.dp, CalendarGridLine, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = WarmAccent,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = PremiumInk,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatBloodPressure(systolic: Int?, diastolic: Int?): String {
    if (systolic == null && diastolic == null) return "기록 없음"
    return "${systolic?.toString() ?: "-"} / ${diastolic?.toString() ?: "-"}"
}

/**
 * 선택 날짜 상태를 작은 배지 형태로 보여준다.
 */
@Composable
private fun StatusLegend(status: DayRecordStatus) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(statusColor(status), androidx.compose.foundation.shape.RoundedCornerShape(99.dp))
        )
    }
}

/**
 * 월 이동 버튼.
 */
@Composable
private fun CalendarNavButton(onClick: () -> Unit, isPrevious: Boolean) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(PremiumGlass, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .border(1.dp, CalendarGridLine, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .softClickable(androidx.compose.foundation.shape.RoundedCornerShape(12.dp), onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isPrevious) "‹" else "›",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PremiumInk
        )
    }
}

/**
 * 캘린더 상태 레전드.
 */
@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LegendItem("없음", DayRecordStatus.NONE)
        LegendItem("아침", DayRecordStatus.MORNING_ONLY)
        LegendItem("저녁", DayRecordStatus.EVENING_ONLY)
        LegendItem("완료", DayRecordStatus.BOTH)
    }
}

/**
 * 레전드 한 칸.
 */
@Composable
private fun LegendItem(label: String, status: DayRecordStatus) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.45f), androidx.compose.foundation.shape.RoundedCornerShape(99.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(statusColor(status), androidx.compose.foundation.shape.RoundedCornerShape(99.dp))
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * 월력 그리드 전체.
 */
@Composable
private fun CalendarGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    statuses: Map<String, CalendarDayStatus>,
    onDateClick: (LocalDate) -> Unit
) {
    val cells = remember(month) { buildCalendarCells(month) }
    val weekdayLabels = listOf("월", "화", "수", "목", "금", "토", "일")
    val compactText = isCompactTextMode()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.68f), androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .border(1.dp, CalendarGridLine, androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            weekdayLabels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = weekdayColor(index)
                )
            }
        }

        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                week.forEach { date ->
                    val calendarStatus = statuses[date.toString()]
                    val status = calendarStatus?.status ?: DayRecordStatus.NONE
                    CalendarDayCell(
                        date = date,
                        inCurrentMonth = date.month == month.month,
                        status = status,
                        isSelected = date == selectedDate,
                        compact = compactText,
                        modifier = Modifier.weight(1f),
                        onClick = { onDateClick(date) }
                    )
                }
            }
        }
    }
}

/**
 * 월력의 하루 셀.
 */
@Composable
private fun CalendarDayCell(
    date: LocalDate,
    inCurrentMonth: Boolean,
    status: DayRecordStatus,
    isSelected: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isToday = date == LocalDate.now()
    val dayTextColor = when {
        !inCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
        date.dayOfWeek.value == 6 -> Color(0xFF4E88D9)
        date.dayOfWeek.value == 7 -> Color(0xFFD96459)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = if (compact) 40.dp else 44.dp)
            .softClickable(androidx.compose.foundation.shape.RoundedCornerShape(14.dp), onClick = onClick)
            .semantics {
                contentDescription = "${date.dayOfMonth}일 ${statusText(status)}"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 28.dp else 32.dp)
                .background(
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        isToday -> Color.White.copy(alpha = 0.98f)
                        else -> Color.Transparent
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                .border(
                    width = if (isSelected || isToday) 1.5.dp else 0.dp,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> WarmAccent.copy(alpha = 0.75f)
                        else -> Color.Transparent
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = dayTextColor,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium
            )
        }

        if (inCurrentMonth) {
            StatusIndicatorRow(status = status)
        } else {
            Spacer(Modifier.height(4.dp))
        }
    }
}

/**
 * 하루 상태를 점 또는 선으로 표현한다.
 */
@Composable
private fun StatusIndicatorRow(status: DayRecordStatus) {
    Row(
        modifier = Modifier.height(6.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (status) {
            DayRecordStatus.NONE -> {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(2.dp)
                        .background(statusColor(status), androidx.compose.foundation.shape.RoundedCornerShape(99.dp))
                )
            }

            DayRecordStatus.MORNING_ONLY -> StatusDot(Color(0xFFF2A65A))
            DayRecordStatus.EVENING_ONLY -> StatusDot(Color(0xFF5C9EED))
            DayRecordStatus.BOTH -> StatusDot(statusColor(DayRecordStatus.BOTH))
        }
    }
}

/**
 * 상태 점 하나.
 */
@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(color, androidx.compose.foundation.shape.RoundedCornerShape(99.dp))
    )
}

/**
 * 캘린더 섹션 상단 배지.
 */
@Composable
private fun CalendarSectionBadge(label: String) {
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
