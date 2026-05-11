package com.pulselog.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.pulselog.data.DailyHealthRecord
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class PendingDeleteTarget(
    val title: String,
    val message: String
) {
    MORNING("아침 혈압 삭제", "아침 혈압 기록을 삭제할까요?"),
    EVENING("저녁 혈압 삭제", "저녁 혈압 기록을 삭제할까요?"),
    WEIGHT("체중 삭제", "체중 기록을 삭제할까요?")
}

/**
 * 캘린더 아래의 빠른 입력 카드.
 * 사용자가 상세 화면 없이 바로 저장할 수 있는 최소 입력 UI를 담당한다.
 */
@Composable
internal fun QuickEntryCard(
    selectedDate: LocalDate,
    record: DailyHealthRecord?,
    morningSys: String,
    morningDia: String,
    eveningSys: String,
    eveningDia: String,
    weight: String,
    onMorningSysChange: (String) -> Unit,
    onMorningDiaChange: (String) -> Unit,
    onEveningSysChange: (String) -> Unit,
    onEveningDiaChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onSaveMorning: () -> Unit,
    onSaveEvening: () -> Unit,
    onSaveWeight: () -> Unit,
    onDeleteMorning: () -> Unit,
    onDeleteEvening: () -> Unit,
    onDeleteWeight: () -> Unit
) {
    var pendingDeleteTarget by remember { mutableStateOf<PendingDeleteTarget?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PremiumPanel)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionBadge(label = "QUICK ENTRY")
                Text(
                    text = "${selectedDate.month.getDisplayName(TextStyle.FULL, Locale.KOREAN)} ${selectedDate.dayOfMonth}일 입력",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PremiumInk
                )
                Text(
                    text = "선택한 날짜 기준으로 바로 추가하거나 수정합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PremiumSubtle
                )
            }

            QuickEntrySection(
                title = "아침 혈압",
                primaryValue = morningSys,
                secondaryValue = morningDia,
                primaryLabel = "수축기",
                secondaryLabel = "이완기",
                onPrimaryChange = onMorningSysChange,
                onSecondaryChange = onMorningDiaChange,
                onSave = onSaveMorning,
                onDelete = { pendingDeleteTarget = PendingDeleteTarget.MORNING },
                canDelete = record?.morningSystolic != null,
                saveLabel = if (record?.morningSystolic == null) "아침 저장" else "아침 수정"
            )

            QuickEntrySection(
                title = "저녁 혈압",
                primaryValue = eveningSys,
                secondaryValue = eveningDia,
                primaryLabel = "수축기",
                secondaryLabel = "이완기",
                onPrimaryChange = onEveningSysChange,
                onSecondaryChange = onEveningDiaChange,
                onSave = onSaveEvening,
                onDelete = { pendingDeleteTarget = PendingDeleteTarget.EVENING },
                canDelete = record?.eveningSystolic != null,
                saveLabel = if (record?.eveningSystolic == null) "저녁 저장" else "저녁 수정"
            )

            WeightQuickEntryRow(
                weight = weight,
                record = record,
                onWeightChange = onWeightChange,
                onSaveWeight = onSaveWeight,
                onDeleteWeight = { pendingDeleteTarget = PendingDeleteTarget.WEIGHT }
            )
        }
    }

    pendingDeleteTarget?.let { target ->
        DeleteConfirmDialog(
            target = target,
            onDismiss = { pendingDeleteTarget = null },
            onConfirm = {
                pendingDeleteTarget = null
                when (target) {
                    PendingDeleteTarget.MORNING -> onDeleteMorning()
                    PendingDeleteTarget.EVENING -> onDeleteEvening()
                    PendingDeleteTarget.WEIGHT -> onDeleteWeight()
                }
            }
        )
    }
}

@Composable
private fun DeleteConfirmDialog(
    target: PendingDeleteTarget,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = target.title,
                color = PremiumInk,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = target.message,
                color = PremiumSubtle
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("삭제", color = Color(0xFFC44555), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        containerColor = PremiumGlass
    )
}

/**
 * 아침/저녁 혈압 입력 공통 UI.
 */
@Composable
private fun QuickEntrySection(
    title: String,
    primaryValue: String,
    secondaryValue: String,
    primaryLabel: String,
    secondaryLabel: String,
    onPrimaryChange: (String) -> Unit,
    onSecondaryChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean,
    saveLabel: String
) {
    Card(colors = CardDefaults.cardColors(containerColor = CardTint)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.SemiBold, color = WarmAccent)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PremiumInputField(
                    value = primaryValue,
                    onValueChange = onPrimaryChange,
                    label = primaryLabel.take(1),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                PremiumInputField(
                    value = secondaryValue,
                    onValueChange = onSecondaryChange,
                    label = secondaryLabel.take(1),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                IconActionButton(
                    label = saveLabel,
                    onClick = onSave
                )
                if (canDelete) {
                    IconActionButton(
                        label = "$title 삭제",
                        onClick = onDelete,
                        icon = Icons.Outlined.Delete,
                        containerColor = PremiumSubtle
                    )
                }
            }
        }
    }
}

/**
 * 체중은 헤더 라인에서 바로 입력하고 저장할 수 있게 별도 행으로 둔다.
 */
@Composable
private fun WeightQuickEntryRow(
    weight: String,
    record: DailyHealthRecord?,
    onWeightChange: (String) -> Unit,
    onSaveWeight: () -> Unit,
    onDeleteWeight: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = CardTint)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("체중", fontWeight = FontWeight.SemiBold, color = WarmAccent)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PremiumInputField(
                    value = weight,
                    onValueChange = onWeightChange,
                    label = "체중",
                    suffix = "kg",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                IconActionButton(
                    label = if (record?.weightKg == null) "체중 저장" else "체중 수정",
                    onClick = onSaveWeight
                )
                if (record?.weightKg != null) {
                    IconActionButton(
                        label = "체중 삭제",
                        onClick = onDeleteWeight,
                        icon = Icons.Outlined.Delete,
                        containerColor = PremiumSubtle
                    )
                }
            }
        }
    }
}

/**
 * 빠른 입력용 공통 필드 스타일.
 */
@Composable
internal fun PremiumInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    suffix: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = if (suffix == null) label else "$label ($suffix)",
                style = MaterialTheme.typography.labelMedium
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PremiumField,
            unfocusedContainerColor = PremiumField,
            focusedBorderColor = WarmAccent,
            unfocusedBorderColor = CalendarGridLine,
            focusedLabelColor = WarmAccent,
            unfocusedLabelColor = PremiumSubtle,
            cursorColor = WarmAccent
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = PremiumInk),
        modifier = modifier.height(56.dp)
    )
}

/**
 * 저장 전용 아이콘 버튼.
 */
@Composable
internal fun IconActionButton(
    label: String,
    onClick: () -> Unit,
    icon: ImageVector = Icons.Outlined.Check,
    containerColor: Color = WarmAccent
) {
    val focusManager: FocusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var justSubmitted by remember { mutableStateOf(false) }
    val animatedContainerColor by animateColorAsState(
        targetValue = when {
            isPressed -> PremiumInk
            justSubmitted -> containerColor.copy(alpha = 0.88f)
            else -> containerColor
        },
        label = "saveButtonColor"
    )
    val buttonScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.92f
            justSubmitted -> 1.06f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.72f),
        label = "saveButtonScale"
    )

    Button(
        onClick = {
            focusManager.clearFocus(force = true)
            scope.launch {
                justSubmitted = true
                onClick()
                delay(220)
                justSubmitted = false
            }
        },
        modifier = Modifier
            .size(38.dp)
            .scale(buttonScale)
            .semantics { contentDescription = label },
        interactionSource = interactionSource,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = animatedContainerColor,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * 섹션 상단의 작은 배지.
 */
@Composable
internal fun SectionBadge(label: String) {
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
