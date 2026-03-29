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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 최근 7일/30일 추이를 보여주는 그래프 탭.
 */
@Composable
internal fun GraphScreen(vm: BpViewModel) {
    val rangeDays by vm.graphRangeDays.collectAsState()
    val points by vm.graphPoints.collectAsState()

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
                colors = CardDefaults.cardColors(containerColor = PremiumPanel)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionBadge(label = "GRAPH")
                        Text(
                            text = "건강 추이",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PremiumInk
                        )
                        Text(
                            text = "선택 기간의 혈압과 체중 변화를 확인합니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PremiumSubtle
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RangeChip(
                            label = "최근 7일",
                            selected = rangeDays == 7,
                            onClick = { vm.setGraphRangeDays(7) }
                        )
                        RangeChip(
                            label = "최근 30일",
                            selected = rangeDays == 30,
                            onClick = { vm.setGraphRangeDays(30) }
                        )
                    }
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
                        colors = listOf(Color(0xFFE05573), Color(0xFFF48FB1))
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
                        colors = listOf(Color(0xFFD81B60))
                    )
                }
            }
        }
    }
}

/**
 * 그래프용 카드 래퍼.
 */
@Composable
private fun ChartCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardTint)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = WarmAccent)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = PremiumSubtle)
            content()
        }
    }
}

@Composable
private fun RangeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) CardTint else PremiumGlass,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = if (selected) WarmAccent.copy(alpha = 0.25f) else CalendarGridLine,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            ,
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = label,
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
            .background(PremiumGlass, RoundedCornerShape(16.dp))
            .border(1.dp, CalendarGridLine, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        val usableWidth = size.width
        val usableHeight = size.height
        val stepX = usableWidth / (labels.lastIndex.coerceAtLeast(1))

        drawLine(
            color = PremiumSubtle.copy(alpha = 0.25f),
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
        Text(labels.first(), style = MaterialTheme.typography.bodySmall, color = PremiumSubtle)
        Text(labels.last(), style = MaterialTheme.typography.bodySmall, color = PremiumSubtle)
    }
}
