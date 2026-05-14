package com.pulselog.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselog.domain.GraphPolicy
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

private val SystolicChartColor = Color(0xFFE05573)
private val DiastolicChartColor = Color(0xFF2F80ED)
private val WeightChartColor = Color(0xFF16A085)

/**
 * 최근 7일/30일 추이를 보여주는 그래프 탭.
 */
@Composable
internal fun GraphScreen(
    vm: BpViewModel,
    onOpenCalendar: () -> Unit
) {
    val rangeDays by vm.graphRangeDays.collectAsState()
    val points by vm.graphPoints.collectAsState()
    val compactText = isCompactTextMode()

    val systolicSeries = points.map { averageOfNotNull(it.morningSystolic, it.eveningSystolic) }
    val diastolicSeries = points.map { averageOfNotNull(it.morningDiastolic, it.eveningDiastolic) }
    val weightSeries = points.map { it.weightKg }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PremiumPanel),
                border = BorderStroke(1.dp, CalendarGridLine.copy(alpha = 0.74f)),
                elevation = CardDefaults.cardElevation(defaultElevation = if (compactText) 2.dp else 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(if (compactText) 14.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compactText) 10.dp else 12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!compactText) {
                            SectionBadge(label = "GRAPH")
                        }
                        Text(
                            text = if (compactText) "추이" else "건강 추이",
                            style = if (compactText) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PremiumInk
                        )
                        if (!compactText) {
                            Text(
                                text = "선택 기간의 혈압과 체중 변화를 확인합니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PremiumSubtle
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RangeChip(
                            label = if (compactText) "7일" else "최근 7일",
                            selected = rangeDays == 7,
                            onClick = { vm.setGraphRangeDays(7) },
                            testTag = "graph.range.7"
                        )
                        RangeChip(
                            label = if (compactText) "30일" else "최근 30일",
                            selected = rangeDays == 30,
                            onClick = { vm.setGraphRangeDays(30) },
                            testTag = "graph.range.30"
                        )
                    }
                }
            }
        }

        item {
            ChartCard(
                title = if (compactText) "혈압" else "혈압 그래프",
                subtitle = "수축기와 이완기의 최근 변화"
            ) {
                if (systolicSeries.all { it == null } && diastolicSeries.all { it == null }) {
                    Text("표시할 혈압 데이터가 없습니다.")
                } else {
                    ChartLegend(
                        items = listOf(
                            LegendEntry(if (compactText) "수" else "수축기", SystolicChartColor),
                            LegendEntry(if (compactText) "이" else "이완기", DiastolicChartColor)
                        )
                    )
                    ChartGapHint(series = listOf(systolicSeries, diastolicSeries))
                    MultiLineChart(
                        labels = points.map { it.dateIso.substring(5) },
                        dateIsoValues = points.map { it.dateIso },
                        series = listOf(systolicSeries, diastolicSeries),
                        seriesLabels = listOf("수축기", "이완기"),
                        colors = listOf(SystolicChartColor, DiastolicChartColor),
                        referenceLines = listOf(
                            ChartReferenceLine(if (compactText) "수 120" else "수축기 120", 120.0, SystolicChartColor),
                            ChartReferenceLine(if (compactText) "이 80" else "이완기 80", 80.0, DiastolicChartColor)
                        ),
                        chartTestTag = "graph.blood_pressure.chart",
                        onOpenCalendar = { dateIso ->
                            openCalendarForGraphDate(
                                dateIso = dateIso,
                                vm = vm,
                                onOpenCalendar = onOpenCalendar
                            )
                        }
                    )
                }
            }
        }

        item {
            ChartCard(
                title = if (compactText) "체중" else "체중 그래프",
                subtitle = "최근 체중 기록 변화"
            ) {
                if (weightSeries.all { it == null }) {
                    Text("표시할 체중 데이터가 없습니다.")
                } else {
                    ChartLegend(
                        items = listOf(
                            LegendEntry("체중", WeightChartColor)
                        )
                    )
                    ChartGapHint(series = listOf(weightSeries))
                    MultiLineChart(
                        labels = points.map { it.dateIso.substring(5) },
                        dateIsoValues = points.map { it.dateIso },
                        series = listOf(weightSeries),
                        seriesLabels = listOf("체중"),
                        valueSuffix = "kg",
                        colors = listOf(WeightChartColor),
                        chartTestTag = "graph.weight.chart",
                        onOpenCalendar = { dateIso ->
                            openCalendarForGraphDate(
                                dateIso = dateIso,
                                vm = vm,
                                onOpenCalendar = onOpenCalendar
                            )
                        }
                    )
                }
            }
        }
    }
}

private data class LegendEntry(
    val label: String,
    val color: Color
)

private data class SelectedChartColumn(
    val label: String,
    val dateIso: String,
    val valueIndex: Int,
    val entries: List<SelectedChartEntry>
)

private data class SelectedChartEntry(
    val seriesLabel: String,
    val value: Double,
    val color: Color
)

private data class ChartReferenceLine(
    val label: String,
    val value: Double,
    val color: Color
)

private fun openCalendarForGraphDate(
    dateIso: String,
    vm: BpViewModel,
    onOpenCalendar: () -> Unit
) {
    val date = runCatching { LocalDate.parse(dateIso) }.getOrNull() ?: return
    vm.setSelectedDate(date)
    onOpenCalendar()
}

@Composable
private fun ChartGapHint(series: List<List<Double?>>) {
    if (!hasMissingSeriesGap(series)) return
    val compactText = isCompactTextMode()

    Text(
        text = if (compactText) "점선은 미기록 구간입니다." else "점선은 미기록 날짜를 건너 연결한 구간입니다.",
        style = MaterialTheme.typography.labelSmall,
        color = PremiumSubtle
    )
}

/**
 * 그래프용 카드 래퍼.
 */
@Composable
private fun ChartCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    val compactText = isCompactTextMode()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (compactText) Color.White.copy(alpha = 0.95f) else CardTint
        ),
        border = BorderStroke(1.dp, if (compactText) CalendarGridLine.copy(alpha = 0.9f) else Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (compactText) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(if (compactText) 14.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactText) 12.dp else 10.dp)
        ) {
            Text(
                title,
                style = if (compactText) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = WarmAccent
            )
            if (!compactText) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = PremiumSubtle)
            }
            content()
        }
    }
}

@Composable
private fun ChartLegend(items: List<LegendEntry>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(item.color, RoundedCornerShape(99.dp))
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = PremiumSubtle,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RangeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val compactText = isCompactTextMode()

    Box(
        modifier = Modifier
            .testTag(testTag)
            .background(
                color = if (selected) CardTint else PremiumGlass,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = if (selected) WarmAccent.copy(alpha = 0.25f) else CalendarGridLine,
                shape = RoundedCornerShape(18.dp)
            )
            .softClickable(RoundedCornerShape(18.dp), onClick = onClick)
            .defaultMinSize(minHeight = if (compactText) 38.dp else 42.dp)
            .padding(horizontal = if (compactText) 10.dp else 14.dp, vertical = if (compactText) 6.dp else 10.dp)
            ,
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) PremiumInk else PremiumSubtle,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * 여러 시계열을 한 번에 그리는 단순 라인 차트.
 * 외부 차트 라이브러리 없이 Compose Canvas로만 그린다.
 */
@Composable
private fun MultiLineChart(
    labels: List<String>,
    dateIsoValues: List<String>,
    series: List<List<Double?>>,
    seriesLabels: List<String>,
    valueSuffix: String = "",
    colors: List<Color>,
    referenceLines: List<ChartReferenceLine> = emptyList(),
    chartTestTag: String,
    onOpenCalendar: (String) -> Unit
) {
    val valueRange = GraphPolicy.valueRange(series + listOf(referenceLines.map { it.value }))
    if (valueRange == null || labels.size < 2) {
        Text("데이터가 부족합니다.")
        return
    }
    var selectedColumn by remember(labels, series) { mutableStateOf<SelectedChartColumn?>(null) }

    ChartSelectionSummary(
        selectedColumn = selectedColumn,
        valueSuffix = valueSuffix,
        onOpenCalendar = onOpenCalendar
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .testTag(chartTestTag)
            .background(PremiumGlass, RoundedCornerShape(16.dp))
            .border(1.dp, CalendarGridLine, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .pointerInput(labels, series, valueRange) {
                detectTapGestures { tapOffset ->
                    val leftInset = 54.dp.toPx()
                    val rightInset = 8.dp.toPx()
                    val topInset = 8.dp.toPx()
                    val bottomInset = 34.dp.toPx()
                    val chartLeft = leftInset
                    val chartRight = size.width - rightInset
                    val chartTop = topInset
                    val chartBottom = size.height - bottomInset
                    val chartWidth = (chartRight - chartLeft).coerceAtLeast(1f)
                    val stepX = chartWidth / (labels.lastIndex.coerceAtLeast(1))
                    val clampedX = tapOffset.x.coerceIn(chartLeft, chartRight)
                    val valueIndex = ((clampedX - chartLeft) / stepX)
                        .roundToInt()
                        .coerceIn(labels.indices)
                    val entries = series.mapIndexedNotNull { seriesIndex, values ->
                        val value = values.getOrNull(valueIndex) ?: return@mapIndexedNotNull null
                        SelectedChartEntry(
                            seriesLabel = seriesLabels.getOrElse(seriesIndex) { "값" },
                            value = value,
                            color = colors[seriesIndex]
                        )
                    }

                    selectedColumn = SelectedChartColumn(
                        label = labels[valueIndex],
                        dateIso = dateIsoValues.getOrElse(valueIndex) { labels[valueIndex] },
                        valueIndex = valueIndex,
                        entries = entries
                    )
                }
            }
    ) {
        val labelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.rgb(155, 108, 121)
            textSize = 24f
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        val xLabelPaint = android.graphics.Paint(labelPaint).apply {
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val axisPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.rgb(240, 215, 222)
            strokeWidth = 2f
        }
        val leftInset = 54.dp.toPx()
        val rightInset = 8.dp.toPx()
        val topInset = 8.dp.toPx()
        val bottomInset = 34.dp.toPx()
        val chartLeft = leftInset
        val chartRight = size.width - rightInset
        val chartTop = topInset
        val chartBottom = size.height - bottomInset
        val chartWidth = (chartRight - chartLeft).coerceAtLeast(1f)
        val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)
        val stepX = chartWidth / (labels.lastIndex.coerceAtLeast(1))
        val yTicks = List(4) { index ->
            valueRange.min + ((valueRange.max - valueRange.min) * index / 3.0)
        }
        val xTickIndexes = buildXAxisTickIndexes(labels.size)
        val selectedX = selectedColumn?.let { chartLeft + (stepX * it.valueIndex) }

        referenceLines.forEach { reference ->
            val y = chartBottom - (((reference.value - valueRange.min) / valueRange.span).toFloat() * chartHeight)
            drawDashedHorizontalLine(
                startX = chartLeft,
                endX = chartRight,
                y = y,
                color = reference.color.copy(alpha = 0.34f)
            )
            drawContext.canvas.nativeCanvas.drawText(
                reference.label,
                chartRight - 4.dp.toPx(),
                y - 5.dp.toPx(),
                android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = reference.color.copy(alpha = 0.82f).toArgb()
                    textSize = 22f
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }

        yTicks.forEach { tick ->
            val y = chartBottom - (((tick - valueRange.min) / valueRange.span).toFloat() * chartHeight)
            drawLine(
                color = PremiumSubtle.copy(alpha = 0.16f),
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 1.5f
            )
            drawContext.canvas.nativeCanvas.drawText(
                formatAxisValue(tick),
                chartLeft - 8.dp.toPx(),
                y + 8.dp.toPx(),
                labelPaint
            )
        }

        drawContext.canvas.nativeCanvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, axisPaint)
        drawContext.canvas.nativeCanvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint)

        selectedX?.let { x ->
            var dashTop = chartTop
            val dashHeight = 8.dp.toPx()
            val dashGap = 6.dp.toPx()
            while (dashTop < chartBottom) {
                val dashBottom = (dashTop + dashHeight).coerceAtMost(chartBottom)
                drawLine(
                    color = PremiumInk.copy(alpha = 0.32f),
                    start = Offset(x, dashTop),
                    end = Offset(x, dashBottom),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
                dashTop += dashHeight + dashGap
            }
        }

        series.forEachIndexed { index, values ->
            val pointOffsets = values.mapIndexedNotNull { valueIndex, value ->
                value ?: return@mapIndexedNotNull null
                val x = chartLeft + (stepX * valueIndex)
                val normalized = ((value - valueRange.min) / valueRange.span).toFloat()
                val y = chartBottom - (normalized * chartHeight)
                valueIndex to Offset(x, y)
            }

            pointOffsets.zipWithNext().forEach { (from, to) ->
                if (to.first - from.first > 1) {
                    drawDashedLine(
                        start = from.second,
                        end = to.second,
                        color = colors[index].copy(alpha = 0.38f),
                        strokeWidth = 4f
                    )
                }
            }

            val path = Path()
            var started = false
            pointOffsets.forEachIndexed { pointIndex, indexedPoint ->
                val previousIndex = pointOffsets.getOrNull(pointIndex - 1)?.first
                val isContinuous = previousIndex != null && indexedPoint.first - previousIndex == 1
                if (!isContinuous) {
                    if (started) {
                        drawPath(
                            path = path,
                            color = colors[index],
                            style = Stroke(width = 5f, cap = StrokeCap.Round)
                        )
                    }
                    path.reset()
                    path.moveTo(indexedPoint.second.x, indexedPoint.second.y)
                    started = true
                } else {
                    path.lineTo(indexedPoint.second.x, indexedPoint.second.y)
                }
            }
            if (started) {
                drawPath(
                    path = path,
                    color = colors[index],
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
            }

            pointOffsets.forEach { (valueIndex, point) ->
                val isSelected = selectedColumn?.valueIndex == valueIndex
                drawCircle(
                    color = PremiumGlass,
                    radius = if (isSelected) 9.5f else 6.5f,
                    center = point
                )
                drawCircle(
                    color = colors[index],
                    radius = if (isSelected) 6.2f else 4.2f,
                    center = point
                )
            }
        }

        xTickIndexes.forEach { index ->
            val x = chartLeft + (stepX * index)
            drawLine(
                color = CalendarGridLine,
                start = Offset(x, chartBottom),
                end = Offset(x, chartBottom + 5.dp.toPx()),
                strokeWidth = 1.5f
            )
            drawContext.canvas.nativeCanvas.drawText(
                labels[index],
                x,
                chartBottom + 26.dp.toPx(),
                xLabelPaint
            )
        }
    }
}

@Composable
private fun ChartSelectionSummary(
    selectedColumn: SelectedChartColumn?,
    valueSuffix: String,
    onOpenCalendar: (String) -> Unit
) {
    val compactText = isCompactTextMode()

    if (selectedColumn == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PremiumGlass)
        ) {
            Text(
                text = if (compactText) "탭하여 값 확인" else "그래프를 탭해 날짜별 값을 확인하세요.",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = PremiumSubtle
            )
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PremiumGlass)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (!compactText) {
                        Text(
                            text = "선택 날짜",
                            style = MaterialTheme.typography.labelSmall,
                            color = PremiumSubtle,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = selectedColumn.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = PremiumInk,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (selectedColumn.entries.isNotEmpty()) {
                    TextButton(
                        onClick = { onOpenCalendar(selectedColumn.dateIso) },
                        modifier = Modifier.testTag("graph.selected.open_calendar")
                    ) {
                        Text(if (compactText) "보기" else "캘린더에서 보기")
                    }
                }
            }

            if (selectedColumn.entries.isEmpty()) {
                Text(
                    text = "해당 날짜에 표시할 값이 없습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PremiumSubtle
                )
            } else {
                val entriesContent: @Composable () -> Unit = {
                    selectedColumn.entries.forEach { entry ->
                        ChartSelectedEntryRow(entry = entry, valueSuffix = valueSuffix)
                    }
                }
                if (compactText) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        entriesContent()
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        entriesContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartSelectedEntryRow(
    entry: SelectedChartEntry,
    valueSuffix: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(entry.color, RoundedCornerShape(99.dp))
        )
        Text(
            text = "${entry.seriesLabel} ${formatAxisValue(entry.value)}$valueSuffix",
            style = MaterialTheme.typography.bodySmall,
            color = PremiumInk,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun buildXAxisTickIndexes(labelCount: Int): List<Int> {
    if (labelCount <= 0) return emptyList()
    if (labelCount <= 3) return (0 until labelCount).toList()
    val middle = (labelCount - 1) / 2
    return listOf(0, middle, labelCount - 1).distinct()
}

private fun hasMissingSeriesGap(series: List<List<Double?>>): Boolean {
    return series.any { values ->
        val valueIndexes = values.mapIndexedNotNull { index, value -> if (value == null) null else index }
        valueIndexes.zipWithNext().any { (from, to) -> to - from > 1 }
    }
}

private fun DrawScope.drawDashedLine(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float
) {
    val delta = end - start
    val distance = delta.getDistance()
    if (distance <= 0f) return

    val dashWidth = 8.dp.toPx()
    val dashGap = 6.dp.toPx()
    val direction = delta / distance
    var current = 0f

    while (current < distance) {
        val next = (current + dashWidth).coerceAtMost(distance)
        drawLine(
            color = color,
            start = start + direction * current,
            end = start + direction * next,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        current += dashWidth + dashGap
    }
}

private fun DrawScope.drawDashedHorizontalLine(
    startX: Float,
    endX: Float,
    y: Float,
    color: Color
) {
    var dashStart = startX
    val dashWidth = 8.dp.toPx()
    val dashGap = 6.dp.toPx()
    while (dashStart < endX) {
        drawLine(
            color = color,
            start = Offset(dashStart, y),
            end = Offset((dashStart + dashWidth).coerceAtMost(endX), y),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
        dashStart += dashWidth + dashGap
    }
}

private fun formatAxisValue(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}
