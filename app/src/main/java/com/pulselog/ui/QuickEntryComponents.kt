package com.pulselog.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.testTag
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
    val compactText = isCompactTextMode()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PremiumPanel),
        border = BorderStroke(1.dp, CalendarGridLine.copy(alpha = 0.74f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (compactText) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(if (compactText) 14.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactText) 14.dp else 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!compactText) {
                    SectionBadge(label = "QUICK ENTRY")
                }
                Text(
                    text = if (compactText) {
                        "${selectedDate.monthValue}/${selectedDate.dayOfMonth}"
                    } else {
                        "${selectedDate.month.getDisplayName(TextStyle.FULL, Locale.KOREAN)} ${selectedDate.dayOfMonth}일 입력"
                    },
                    style = if (compactText) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PremiumInk
                )
                if (!compactText) {
                    Text(
                        text = "선택한 날짜 기준으로 바로 추가하거나 수정합니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PremiumSubtle
                    )
                }
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
                saveLabel = if (record?.morningSystolic == null) "아침 저장" else "아침 수정",
                primaryTestTag = "quick.morning.systolic",
                secondaryTestTag = "quick.morning.diastolic",
                saveTestTag = "quick.morning.save",
                deleteTestTag = "quick.morning.delete"
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
                saveLabel = if (record?.eveningSystolic == null) "저녁 저장" else "저녁 수정",
                primaryTestTag = "quick.evening.systolic",
                secondaryTestTag = "quick.evening.diastolic",
                saveTestTag = "quick.evening.save",
                deleteTestTag = "quick.evening.delete"
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
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("quick.delete.confirm")
            ) {
                Text("삭제", color = Color(0xFFC44555), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("quick.delete.cancel")
            ) {
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
    saveLabel: String,
    primaryTestTag: String,
    secondaryTestTag: String,
    saveTestTag: String,
    deleteTestTag: String
) {
    val compactText = isCompactTextMode()
    val compactTitle = compactSectionTitle(title, compactText)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (compactText) Color.White.copy(alpha = 0.95f) else CardTint
        ),
        border = BorderStroke(1.dp, if (compactText) CalendarGridLine.copy(alpha = 0.9f) else Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (compactText) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(if (compactText) 10.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactText) 8.dp else 6.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val useStackedControls = maxWidth < 480.dp || compactText

                if (useStackedControls) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                compactTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = WarmAccent
                            )
                            QuickEntryActions(
                                saveLabel = saveLabel,
                                onSave = onSave,
                                deleteLabel = "$compactTitle 삭제",
                                onDelete = onDelete,
                                canDelete = canDelete,
                                saveTestTag = saveTestTag,
                                deleteTestTag = deleteTestTag
                            )
                        }
                        BloodPressureInputPair(
                            primaryValue = primaryValue,
                            secondaryValue = secondaryValue,
                            primaryLabel = primaryLabel,
                            secondaryLabel = secondaryLabel,
                            onPrimaryChange = onPrimaryChange,
                            onSecondaryChange = onSecondaryChange,
                            compact = compactText,
                            primaryTestTag = primaryTestTag,
                            secondaryTestTag = secondaryTestTag
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(compactTitle, fontWeight = FontWeight.SemiBold, color = WarmAccent)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PremiumInputField(
                            value = primaryValue,
                            onValueChange = onPrimaryChange,
                            label = compactPressureLabel(primaryLabel, compactText),
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                            testTag = primaryTestTag
                        )
                        PremiumInputField(
                            value = secondaryValue,
                            onValueChange = onSecondaryChange,
                            label = compactPressureLabel(secondaryLabel, compactText),
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                            testTag = secondaryTestTag
                        )
                        QuickEntryActions(
                            saveLabel = saveLabel,
                            onSave = onSave,
                            deleteLabel = "$compactTitle 삭제",
                            onDelete = onDelete,
                            canDelete = canDelete,
                            saveTestTag = saveTestTag,
                            deleteTestTag = deleteTestTag
                        )
                    }
                }
            }
        }
    }
}

private fun compactSectionTitle(title: String, compact: Boolean): String {
    if (!compact) return title
    return when (title) {
        "아침 혈압" -> "아침"
        "저녁 혈압" -> "저녁"
        "체중" -> "kg"
        else -> title
    }
}

@Composable
private fun BloodPressureInputPair(
    primaryValue: String,
    secondaryValue: String,
    primaryLabel: String,
    secondaryLabel: String,
    onPrimaryChange: (String) -> Unit,
    onSecondaryChange: (String) -> Unit,
    compact: Boolean,
    primaryTestTag: String,
    secondaryTestTag: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = if (compact) Modifier.widthIn(max = 220.dp) else Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PremiumInputField(
            value = primaryValue,
            onValueChange = onPrimaryChange,
            label = if (compact) "" else compactPressureLabel(primaryLabel, compact),
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
            testTag = primaryTestTag
        )
        PremiumInputField(
            value = secondaryValue,
            onValueChange = onSecondaryChange,
            label = if (compact) "" else compactPressureLabel(secondaryLabel, compact),
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
            testTag = secondaryTestTag
        )
    }
}

private fun compactPressureLabel(label: String, compact: Boolean): String {
    if (!compact) return label.take(1)
    return when (label) {
        "수축기" -> "수"
        "이완기" -> "이"
        else -> label.take(1)
    }
}

@Composable
private fun QuickEntryActions(
    saveLabel: String,
    onSave: () -> Unit,
    deleteLabel: String,
    onDelete: () -> Unit,
    canDelete: Boolean,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
    saveTestTag: String? = null,
    deleteTestTag: String? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, horizontalAlignment),
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconActionButton(
            label = saveLabel,
            onClick = onSave,
            testTag = saveTestTag
        )
        if (canDelete) {
            IconActionButton(
                label = deleteLabel,
                onClick = onDelete,
                icon = Icons.Outlined.Delete,
                containerColor = PremiumSubtle,
                testTag = deleteTestTag
            )
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
    val compactText = isCompactTextMode()
    val title = "체중"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (compactText) Color.White.copy(alpha = 0.95f) else CardTint
        ),
        border = BorderStroke(1.dp, if (compactText) CalendarGridLine.copy(alpha = 0.9f) else Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (compactText) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(if (compactText) 10.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactText) 8.dp else 6.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val useStackedControls = maxWidth < 480.dp || compactText
                val saveLabel = if (record?.weightKg == null) "체중 저장" else "체중 수정"

                if (useStackedControls) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = WarmAccent
                            )
                            QuickEntryActions(
                                saveLabel = saveLabel,
                                onSave = onSaveWeight,
                                deleteLabel = "체중 삭제",
                                onDelete = onDeleteWeight,
                                canDelete = record?.weightKg != null,
                                saveTestTag = "quick.weight.save",
                                deleteTestTag = "quick.weight.delete"
                            )
                        }
                        PremiumInputField(
                            value = weight,
                            onValueChange = onWeightChange,
                            label = if (compactText) "" else "체중",
                            suffix = if (compactText) null else "kg",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.widthIn(max = 140.dp),
                            testTag = "quick.weight.value"
                        )
                    }
                } else {
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
                            value = weight,
                            onValueChange = onWeightChange,
                            label = "체중",
                            suffix = "kg",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f),
                            testTag = "quick.weight.value"
                        )
                        QuickEntryActions(
                            saveLabel = saveLabel,
                            onSave = onSaveWeight,
                            deleteLabel = "체중 삭제",
                            onDelete = onDeleteWeight,
                            canDelete = record?.weightKg != null,
                            saveTestTag = "quick.weight.save",
                            deleteTestTag = "quick.weight.delete"
                        )
                    }
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
    suffix: String? = null,
    testTag: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = if (label.isBlank()) null else ({
            Text(
                text = if (suffix == null) label else "$label ($suffix)",
                style = MaterialTheme.typography.labelMedium
            )
        }),
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
        modifier = modifier
            .then(if (testTag == null) Modifier else Modifier.testTag(testTag))
            .defaultMinSize(minHeight = 56.dp)
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
    containerColor: Color = WarmAccent,
    testTag: String? = null
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
            .size(42.dp)
            .then(if (testTag == null) Modifier else Modifier.testTag(testTag))
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
