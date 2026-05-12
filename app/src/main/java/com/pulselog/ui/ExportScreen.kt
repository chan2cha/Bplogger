package com.pulselog.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.pulselog.domain.ExportPolicy
import com.pulselog.domain.ExportRange
import com.pulselog.domain.ExportSummary
import com.pulselog.domain.ExportTrendPoint
import java.io.File
import kotlin.math.max

/**
 * CSV와 PDF 요약본 내보내기 전용 화면.
 * 헤더의 공유 아이콘에서 진입해 설정 화면과 독립적으로 관리한다.
 */
@Composable
internal fun ExportScreen(
    vm: BpViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val compactText = isCompactTextMode()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PremiumPanel)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionBadge(label = "EXPORT")
                    Text(
                        "데이터 내보내기",
                        style = if (compactText) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PremiumInk
                    )
                    if (!compactText) {
                        Text(
                            "혈압과 체중 기록을 CSV 원본 파일 또는 병원 제출용 PDF 요약본으로 공유합니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PremiumSubtle
                        )
                    }
                }
                Text(
                    if (compactText) "CSV" else "CSV 원본 데이터",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PremiumInk
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            vm.exportCsv(ExportRange.RECENT_30_DAYS) { fileName, csv ->
                                shareCsv(context, fileName, csv)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmAccent)
                    ) {
                        Text(if (compactText) "30일" else "최근 30일")
                    }
                    Button(
                        onClick = {
                            vm.exportCsv(ExportRange.ALL) { fileName, csv ->
                                shareCsv(context, fileName, csv)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumSubtle)
                    ) {
                        Text("전체")
                    }
                }
                Text(
                    if (compactText) "PDF" else "PDF 요약본",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PremiumInk
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            vm.exportPdfSummary(ExportRange.RECENT_30_DAYS) { fileName, summary ->
                                sharePdfSummary(context, fileName, summary)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmAccent)
                    ) {
                        Text(if (compactText) "30일" else "최근 30일")
                    }
                    Button(
                        onClick = {
                            vm.exportPdfSummary(ExportRange.ALL) { fileName, summary ->
                                sharePdfSummary(context, fileName, summary)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumSubtle)
                    ) {
                        Text("전체")
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PremiumGlass)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmAccent)
                ) {
                    Text(if (compactText) "닫기" else "메인으로")
                }
            }
        }
    }
}

private fun sharePdfSummary(
    context: Context,
    fileName: String,
    summary: ExportSummary
) {
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val exportFile = File(exportDir, fileName).apply {
        outputStream().use { outputStream ->
            val document = createSummaryPdf(summary)
            try {
                document.writeTo(outputStream)
            } finally {
                document.close()
            }
        }
    }
    shareFile(
        context = context,
        file = exportFile,
        mimeType = "application/pdf",
        subject = "Pulse Log PDF 요약본",
        text = "Pulse Log 혈압/체중 기록 PDF 요약본입니다.",
        chooserTitle = "PDF 요약본 공유",
        failureMessage = "PDF를 공유할 앱을 찾을 수 없습니다."
    )
}

private fun shareCsv(
    context: Context,
    fileName: String,
    csv: String
) {
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val exportFile = File(exportDir, fileName).apply {
        writeText(csv, Charsets.UTF_8)
    }
    shareFile(
        context = context,
        file = exportFile,
        mimeType = "text/csv",
        subject = "Pulse Log CSV",
        text = "Pulse Log 혈압/체중 기록 CSV입니다.",
        chooserTitle = "CSV 공유",
        failureMessage = "CSV를 공유할 앱을 찾을 수 없습니다."
    )
}

private fun shareFile(
    context: Context,
    file: File,
    mimeType: String,
    subject: String,
    text: String,
    chooserTitle: String,
    failureMessage: String
) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }.onFailure {
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
    }
}

private fun createSummaryPdf(summary: ExportSummary): PdfDocument {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 44f
    val contentWidth = pageWidth - margin * 2
    val bottomMargin = 58f
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(41, 32, 37)
        textSize = 23f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(41, 32, 37)
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(188, 61, 82)
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(41, 32, 37)
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val smallBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(41, 32, 37)
        textSize = 10.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val subtlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(111, 94, 102)
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(229, 215, 219)
        strokeWidth = 1f
    }
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 247, 249)
        style = Paint.Style.FILL
    }
    val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(241, 211, 218)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(188, 61, 82)
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
    }
    val diastolicPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(54, 122, 153)
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
    }
    val weightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(71, 145, 92)
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
    }

    var pageNumber = 1
    var page = document.startPage(
        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    )
    var canvas = page.canvas
    var y = margin

    fun finishPage() {
        drawFooter(canvas = canvas, pageWidth = pageWidth, pageHeight = pageHeight, pageNumber = pageNumber, paint = subtlePaint)
        document.finishPage(page)
        pageNumber += 1
        page = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        canvas = page.canvas
        y = margin
        drawPageHeader(canvas, margin, contentWidth, titlePaint, subtlePaint)
        y = 104f
    }

    fun ensureSpace(height: Float) {
        if (y + height > pageHeight - bottomMargin) finishPage()
    }

    fun drawLine(text: String, paint: Paint = bodyPaint, spacing: Float = 20f) {
        ensureSpace(spacing)
        canvas.drawText(text, margin, y, paint)
        y += spacing
    }

    fun drawSummaryCard(x: Float, top: Float, width: Float, label: String, value: String) {
        val rect = RectF(x, top, x + width, top + 64f)
        canvas.drawRoundRect(rect, 10f, 10f, cardPaint)
        canvas.drawRoundRect(rect, 10f, 10f, cardBorderPaint)
        canvas.drawText(label, x + 12f, top + 22f, labelPaint)
        canvas.drawText(value, x + 12f, top + 46f, sectionPaint)
    }

    drawPageHeader(canvas, margin, contentWidth, titlePaint, subtlePaint)
    y = 104f

    val period = if (summary.startDateIso == null || summary.endDateIso == null) {
        "기록 없음"
    } else {
        "${summary.startDateIso} ~ ${summary.endDateIso}"
    }
    val morningAverage = formatPdfBloodPressure(summary.averageMorningSystolic, summary.averageMorningDiastolic)
    val eveningAverage = formatPdfBloodPressure(summary.averageEveningSystolic, summary.averageEveningDiastolic)
    val weightAverage = summary.averageWeightKg?.let { "${formatPdfWeight(it)} kg" } ?: "-"

    drawLine("요약 정보", sectionPaint, 24f)
    drawLine("생성일 ${summary.generatedDateIso}  |  범위 ${summary.rangeLabel}  |  기록 기간 $period", bodyPaint, 26f)
    val cardGap = 10f
    val cardWidth = (contentWidth - cardGap * 2) / 3f
    drawSummaryCard(margin, y, cardWidth, "기록 일수", "${summary.totalRecordDays}일")
    drawSummaryCard(margin + cardWidth + cardGap, y, cardWidth, "아침 평균", morningAverage)
    drawSummaryCard(margin + (cardWidth + cardGap) * 2, y, cardWidth, "저녁 평균", eveningAverage)
    y += 78f
    drawSummaryCard(margin, y, cardWidth, "체중 평균", weightAverage)
    drawSummaryCard(margin + cardWidth + cardGap, y, cardWidth, "혈압 기록", "${summary.morningCount + summary.eveningCount}건")
    drawSummaryCard(margin + (cardWidth + cardGap) * 2, y, cardWidth, "체중 기록", "${summary.weightCount}건")
    y += 88f

    ensureSpace(240f)
    drawLine("최근 추이", sectionPaint, 24f)
    drawTrendChart(
        canvas = canvas,
        bounds = RectF(margin, y, pageWidth - margin, y + 204f),
        points = summary.trendPoints,
        cardPaint = cardPaint,
        borderPaint = cardBorderPaint,
        gridPaint = linePaint,
        systolicPaint = accentPaint,
        diastolicPaint = diastolicPaint,
        weightPaint = weightPaint,
        labelPaint = subtlePaint,
        boldPaint = smallBoldPaint
    )
    y += 226f

    finishPage()
    drawLine("최근 기록", sectionPaint, 24f)
    y += drawRecordTable(
        canvas = canvas,
        top = y,
        summary = summary,
        margin = margin,
        contentWidth = contentWidth,
        headerPaint = smallBoldPaint,
        bodyPaint = subtlePaint,
        borderPaint = linePaint
    )
    y += 20f
    drawLine("이 문서는 사용자가 Pulse Log에 입력한 기록을 바탕으로 생성된 참고 자료입니다.", subtlePaint, 18f)
    drawLine("진단과 치료 판단은 의료진 상담을 기준으로 하세요.", subtlePaint, 18f)

    drawFooter(canvas = canvas, pageWidth = pageWidth, pageHeight = pageHeight, pageNumber = pageNumber, paint = subtlePaint)
    document.finishPage(page)
    return document
}

private fun drawPageHeader(
    canvas: android.graphics.Canvas,
    margin: Float,
    contentWidth: Float,
    titlePaint: Paint,
    subtlePaint: Paint
) {
    canvas.drawText("Pulse Log 혈압/체중 요약본", margin, 52f, titlePaint)
    canvas.drawText("Blood pressure and weight summary", margin, 74f, subtlePaint)
    canvas.drawLine(margin, 88f, margin + contentWidth, 88f, subtlePaint)
}

private fun drawFooter(
    canvas: android.graphics.Canvas,
    pageWidth: Int,
    pageHeight: Int,
    pageNumber: Int,
    paint: Paint
) {
    canvas.drawLine(44f, pageHeight - 40f, pageWidth - 44f, pageHeight - 40f, paint)
    canvas.drawText("Pulse Log", 44f, pageHeight - 22f, paint)
    canvas.drawText("Page $pageNumber", pageWidth - 88f, pageHeight - 22f, paint)
}

private fun android.graphics.Canvas.drawRoundRect(rect: RectF, rx: Float, ry: Float, paint: Paint) {
    drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, rx, ry, paint)
}

private fun drawTrendChart(
    canvas: android.graphics.Canvas,
    bounds: RectF,
    points: List<ExportTrendPoint>,
    cardPaint: Paint,
    borderPaint: Paint,
    gridPaint: Paint,
    systolicPaint: Paint,
    diastolicPaint: Paint,
    weightPaint: Paint,
    labelPaint: Paint,
    boldPaint: Paint
) {
    canvas.drawRoundRect(bounds, 10f, 10f, cardPaint)
    canvas.drawRoundRect(bounds, 10f, 10f, borderPaint)

    if (points.isEmpty()) {
        canvas.drawText("표시할 추이 데이터가 없습니다.", bounds.left + 16f, bounds.top + 42f, labelPaint)
        return
    }

    val chartLeft = bounds.left + 44f
    val chartRight = bounds.right - 18f
    val bloodTop = bounds.top + 34f
    val bloodBottom = bounds.top + 126f
    val weightTop = bounds.top + 150f
    val weightBottom = bounds.bottom - 24f
    val xStep = if (points.size <= 1) 0f else (chartRight - chartLeft) / (points.size - 1)

    canvas.drawText("혈압 추이", bounds.left + 14f, bounds.top + 22f, boldPaint)
    canvas.drawText("수축기", bounds.right - 156f, bounds.top + 22f, systolicPaint)
    canvas.drawText("이완기", bounds.right - 104f, bounds.top + 22f, diastolicPaint)
    canvas.drawText("체중", bounds.right - 52f, bounds.top + 22f, weightPaint)

    listOf(200, 160, 120, 80).forEach { value ->
        val y = bloodBottom - ((value - 50f) / 150f) * (bloodBottom - bloodTop)
        canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
        canvas.drawText(value.toString(), bounds.left + 14f, y + 4f, labelPaint)
    }
    canvas.drawText("mmHg", bounds.left + 14f, bloodTop - 6f, labelPaint)
    drawNumericSeries(
        canvas = canvas,
        values = points.map { it.systolic?.toFloat() },
        chartLeft = chartLeft,
        chartBottom = bloodBottom,
        xStep = xStep,
        valueMin = 50f,
        valueMax = 200f,
        height = bloodBottom - bloodTop,
        paint = systolicPaint
    )
    drawNumericSeries(
        canvas = canvas,
        values = points.map { it.diastolic?.toFloat() },
        chartLeft = chartLeft,
        chartBottom = bloodBottom,
        xStep = xStep,
        valueMin = 50f,
        valueMax = 200f,
        height = bloodBottom - bloodTop,
        paint = diastolicPaint
    )

    val weightValues = points.mapNotNull { it.weightKg }
    val minWeight = max(0f, ((weightValues.minOrNull() ?: 0.0) - 2.0).toFloat())
    val maxWeight = ((weightValues.maxOrNull() ?: 100.0) + 2.0).toFloat().coerceAtLeast(minWeight + 1f)
    canvas.drawText("체중 kg", bounds.left + 14f, weightTop + 4f, labelPaint)
    canvas.drawLine(chartLeft, weightTop, chartRight, weightTop, gridPaint)
    canvas.drawLine(chartLeft, weightBottom, chartRight, weightBottom, gridPaint)
    canvas.drawText(formatPdfWeight(maxWeight.toDouble()), bounds.left + 14f, weightTop + 4f, labelPaint)
    canvas.drawText(formatPdfWeight(minWeight.toDouble()), bounds.left + 14f, weightBottom + 4f, labelPaint)
    drawNumericSeries(
        canvas = canvas,
        values = points.map { it.weightKg?.toFloat() },
        chartLeft = chartLeft,
        chartBottom = weightBottom,
        xStep = xStep,
        valueMin = minWeight,
        valueMax = maxWeight,
        height = weightBottom - weightTop,
        paint = weightPaint
    )

    val firstLabel = points.first().dateIso.drop(5)
    val lastLabel = points.last().dateIso.drop(5)
    canvas.drawText(firstLabel, chartLeft, bounds.bottom - 6f, labelPaint)
    canvas.drawText(lastLabel, chartRight - 34f, bounds.bottom - 6f, labelPaint)
}

private fun drawNumericSeries(
    canvas: android.graphics.Canvas,
    values: List<Float?>,
    chartLeft: Float,
    chartBottom: Float,
    xStep: Float,
    valueMin: Float,
    valueMax: Float,
    height: Float,
    paint: Paint
) {
    var previousX: Float? = null
    var previousY: Float? = null
    val pointPaint = Paint(paint).apply {
        style = Paint.Style.FILL
        strokeWidth = 1f
    }

    values.forEachIndexed { index, value ->
        if (value == null) {
            previousX = null
            previousY = null
            return@forEachIndexed
        }
        val bounded = value.coerceIn(valueMin, valueMax)
        val x = chartLeft + xStep * index
        val y = chartBottom - ((bounded - valueMin) / (valueMax - valueMin)) * height
        if (previousX != null && previousY != null) {
            canvas.drawLine(previousX ?: x, previousY ?: y, x, y, paint)
        }
        canvas.drawCircle(x, y, 2.8f, pointPaint)
        previousX = x
        previousY = y
    }
}

private fun drawRecordTable(
    canvas: android.graphics.Canvas,
    top: Float,
    summary: ExportSummary,
    margin: Float,
    contentWidth: Float,
    headerPaint: Paint,
    bodyPaint: Paint,
    borderPaint: Paint
): Float {
    val rowHeight = 24f
    val colDate = margin
    val colMorning = margin + 126f
    val colEvening = margin + 250f
    val colWeight = margin + 374f
    var y = top

    fun drawHeader() {
        canvas.drawLine(margin, y, margin + contentWidth, y, borderPaint)
        y += 17f
        canvas.drawText("날짜", colDate, y, headerPaint)
        canvas.drawText("아침 혈압", colMorning, y, headerPaint)
        canvas.drawText("저녁 혈압", colEvening, y, headerPaint)
        canvas.drawText("체중", colWeight, y, headerPaint)
        y += 7f
        canvas.drawLine(margin, y, margin + contentWidth, y, borderPaint)
    }

    drawHeader()
    summary.latestRows.forEach { row ->
        y += 17f
        canvas.drawText(row.dateIso, colDate, y, bodyPaint)
        canvas.drawText(row.morningBloodPressure.ifBlank { "-" }, colMorning, y, bodyPaint)
        canvas.drawText(row.eveningBloodPressure.ifBlank { "-" }, colEvening, y, bodyPaint)
        canvas.drawText(row.weightKg.ifBlank { "-" }, colWeight, y, bodyPaint)
        y += 7f
        canvas.drawLine(margin, y, margin + contentWidth, y, borderPaint)
    }
    return max(rowHeight, y - top)
}

private fun formatPdfBloodPressure(systolic: Int?, diastolic: Int?): String {
    return if (systolic == null || diastolic == null) "-" else "$systolic/$diastolic"
}

private fun formatPdfWeight(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.1f", value)
    }
}
