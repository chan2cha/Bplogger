package com.pulselog.ui

import androidx.compose.ui.graphics.Color
import com.pulselog.data.DayRecordStatus
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 화면 전반에서 재사용하는 컬러 토큰.
 * UI 파일을 나눠도 같은 톤을 유지하기 위해 별도 파일로 분리한다.
 */
internal val WarmPaper = Color(0xFFFFF2F5)
internal val WarmAccent = Color(0xFFE05573)
internal val CardTint = Color(0xFFFFF9FB)
internal val CalendarShell = Color(0xFFFFF5F7)
internal val CalendarGridLine = Color(0xFFF0D7DE)
internal val PremiumInk = Color(0xFF5A2432)
internal val PremiumSubtle = Color(0xFF9B6C79)
internal val PremiumGlass = Color(0xFFFFFCFD)
internal val PremiumPanel = Color(0xFFFFF6F8)
internal val PremiumField = Color(0xFFFFFBFC)

/**
 * 월력 셀은 항상 6주를 고정 노출한다.
 * 이전/다음 달 날짜를 함께 넣어 레이아웃 점프를 막는다.
 */
internal fun buildCalendarCells(month: YearMonth): List<LocalDate> {
    val firstDay = month.atDay(1)
    val offset = firstDay.dayOfWeek.value - 1
    val startDate = firstDay.minusDays(offset.toLong())
    return List(42) { index -> startDate.plusDays(index.toLong()) }
}

/**
 * 기록 상태를 사용자 문구로 변환한다.
 */
internal fun statusText(status: DayRecordStatus): String = when (status) {
    DayRecordStatus.NONE -> "기록 없음"
    DayRecordStatus.MORNING_ONLY -> "아침만 기록"
    DayRecordStatus.EVENING_ONLY -> "저녁만 기록"
    DayRecordStatus.BOTH -> "아침/저녁 모두 기록"
}

/**
 * 기록 상태별 대표색.
 */
internal fun statusColor(status: DayRecordStatus): Color = when (status) {
    DayRecordStatus.NONE -> Color(0xFFDCCFD5)
    DayRecordStatus.MORNING_ONLY -> Color(0xFFFF8A80)
    DayRecordStatus.EVENING_ONLY -> Color(0xFFF06292)
    DayRecordStatus.BOTH -> Color(0xFFD81B60)
}

/**
 * 캘린더 요일 텍스트 색상.
 */
internal fun weekdayColor(index: Int): Color = when (index) {
    5 -> Color(0xFFCC6F8C)
    6 -> Color(0xFFD64D6F)
    else -> Color(0xFF8C6A74)
}

/**
 * 저장된 epoch milli 값을 사용자가 읽을 수 있는 시각 문자열로 변환한다.
 */
internal fun formatDateTime(epochMs: Long): String {
    val zoned = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
    return zoned.format(DateTimeFormatter.ofPattern("MM.dd HH:mm"))
}

/**
 * 체중은 정수면 소수점을 제거하고, 소수면 두 자리까지 유지한다.
 */
internal fun formatWeight(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
}

/**
 * 아침/저녁 값이 둘 다 있으면 평균, 하나만 있으면 그 값을 쓴다.
 */
internal fun averageOfNotNull(first: Int?, second: Int?): Double? {
    val values = listOfNotNull(first, second)
    if (values.isEmpty()) return null
    return values.average()
}
