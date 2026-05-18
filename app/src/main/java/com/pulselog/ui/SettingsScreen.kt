package com.pulselog.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselog.BuildConfig

/**
 * 알림 설정 탭.
 */
@Composable
internal fun SettingsScreen(
    vm: BpViewModel,
    onClose: () -> Unit,
    notificationsAllowed: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val settings by vm.notificationSettings.collectAsState()

    var morningEnabled by remember { mutableStateOf(settings.morningEnabled) }
    var morningTime by remember { mutableStateOf(settings.morningTime) }
    var eveningEnabled by remember { mutableStateOf(settings.eveningEnabled) }
    var eveningTime by remember { mutableStateOf(settings.eveningTime) }
    var timePickerTarget by remember { mutableStateOf<TimePickerTarget?>(null) }

    // 저장된 설정이 바뀌면 로컬 폼 상태도 즉시 맞춘다.
    LaunchedEffect(settings) {
        morningEnabled = settings.morningEnabled
        morningTime = settings.morningTime
        eveningEnabled = settings.eveningEnabled
        eveningTime = settings.eveningTime
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
        SettingsHeader()

        SettingRow(
            label = "아침 알림",
            description = "아침 기록을 남길 시간대에 맞춥니다.",
            checked = morningEnabled,
            onCheckedChange = { morningEnabled = it }
        ) { fieldModifier ->
            TimeChoicePanel(
                selectedTime = morningTime,
                enabled = morningEnabled,
                onOpenPicker = { timePickerTarget = TimePickerTarget.Morning },
                modifier = fieldModifier,
                testTag = "settings.morning.time"
            )
        }
        SettingRow(
            label = "저녁 알림",
            description = "저녁 기록을 확인할 시간대에 맞춥니다.",
            checked = eveningEnabled,
            onCheckedChange = { eveningEnabled = it }
        ) { fieldModifier ->
            TimeChoicePanel(
                selectedTime = eveningTime,
                enabled = eveningEnabled,
                onOpenPicker = { timePickerTarget = TimePickerTarget.Evening },
                modifier = fieldModifier,
                testTag = "settings.evening.time"
            )
        }
        SettingsSaveCard {
            val notificationEnabled = morningEnabled || eveningEnabled
            if (notificationEnabled && !notificationsAllowed) {
                onRequestNotificationPermission()
                vm.showNotificationPermissionRequired()
                return@SettingsSaveCard
            }
            vm.saveNotificationSettings(
                morningEnabled = morningEnabled,
                morningTime = morningTime,
                eveningEnabled = eveningEnabled,
                eveningTime = eveningTime
            )
        }

        if (BuildConfig.DEBUG) {
            DebugDataCard(
                onSeedGraphData = vm::seedGraphDemoData
            )
        }

        SettingsFooterActions(onClose = onClose)
    }

    val activeTarget = timePickerTarget
    if (activeTarget != null) {
        ContextualTimePickerDialog(
            target = activeTarget,
            initialTime = when (activeTarget) {
                TimePickerTarget.Morning -> morningTime
                TimePickerTarget.Evening -> eveningTime
            },
            onDismiss = { timePickerTarget = null },
            onConfirm = { selectedTime ->
                when (activeTarget) {
                    TimePickerTarget.Morning -> morningTime = selectedTime
                    TimePickerTarget.Evening -> eveningTime = selectedTime
                }
                timePickerTarget = null
            }
        )
    }
}

@Composable
private fun SettingsHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val compactText = isCompactTextMode()
        if (!compactText) {
            SectionBadge(label = "SETTINGS")
        }
        Text(
            "알림 설정",
            style = if (compactText) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PremiumInk
        )
        if (!compactText) {
            Text(
                "아침과 저녁 기록 알림 시간을 조정합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = PremiumSubtle
            )
        }
    }
}

private enum class TimePickerTarget {
    Morning,
    Evening
}

private val MorningDialogHours = (5..11).toList()
private val EveningDialogHours = (17..23).toList()
private val DialogMinutes = listOf(0, 10, 20, 30, 40, 50)

@Composable
private fun TimeChoicePanel(
    selectedTime: String,
    enabled: Boolean,
    onOpenPicker: () -> Unit,
    modifier: Modifier,
    testTag: String
) {
    Column(
        modifier = modifier.testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onOpenPicker,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 58.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PremiumGlass,
                contentColor = PremiumInk,
                disabledContainerColor = PremiumGlass.copy(alpha = 0.55f),
                disabledContentColor = PremiumSubtle
            )
        ) {
            Text(
                text = selectedTime,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SettingsSaveCard(onSave: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PremiumPanel),
        border = BorderStroke(1.dp, CalendarGridLine.copy(alpha = 0.74f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompactTextMode()) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    "변경사항 저장",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PremiumInk
                )
                Text(
                    "저장하면 알림 예약도 함께 갱신됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PremiumSubtle
                )
            }
            IconActionButton(
                label = "설정 저장",
                testTag = "settings.save",
                onClick = onSave
            )
        }
    }
}

@Composable
private fun ContextualTimePickerDialog(
    target: TimePickerTarget,
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val initialParts = initialTime.split(":")
    val allowedHours = when (target) {
        TimePickerTarget.Morning -> MorningDialogHours
        TimePickerTarget.Evening -> EveningDialogHours
    }
    var selectedHour by remember(initialTime, target) {
        mutableStateOf((initialParts.getOrNull(0)?.toIntOrNull() ?: allowedHours.first()).coerceIn(allowedHours))
    }
    var selectedMinute by remember(initialTime, target) {
        mutableStateOf(nearestDialogMinute(initialParts.getOrNull(1)?.toIntOrNull() ?: 0))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (target == TimePickerTarget.Morning) "아침 알림 시간" else "저녁 알림 시간",
                color = PremiumInk,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(formatTime(selectedHour, selectedMinute), style = MaterialTheme.typography.headlineSmall, color = WarmAccent)
                TimePickerChipSection(
                    title = "시간",
                    values = allowedHours,
                    selectedValue = selectedHour,
                    label = { "%02d시".format(it) },
                    onSelected = { selectedHour = it }
                )
                TimePickerChipSection(
                    title = "분",
                    values = DialogMinutes,
                    selectedValue = selectedMinute,
                    label = { "%02d분".format(it) },
                    onSelected = { selectedMinute = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(formatTime(selectedHour, selectedMinute)) }) {
                Text("적용")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        containerColor = PremiumPanel
    )
}

@Composable
private fun TimePickerChipSection(
    title: String,
    values: List<Int>,
    selectedValue: Int,
    label: (Int) -> String,
    onSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = PremiumSubtle)
        ChunkedRows(items = values, rowSize = 4) { value ->
            TimeChip(
                label = label(value),
                selected = value == selectedValue,
                enabled = true,
                onClick = { onSelected(value) }
            )
        }
    }
}

@Composable
private fun TimeChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        modifier = Modifier.defaultMinSize(minHeight = 36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) WarmAccent else PremiumGlass,
            contentColor = if (selected) androidx.compose.ui.graphics.Color.White else PremiumInk,
            disabledContainerColor = PremiumGlass.copy(alpha = 0.55f),
            disabledContentColor = PremiumSubtle
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun <T> ChunkedRows(
    items: List<T>,
    rowSize: Int,
    itemContent: @Composable (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(rowSize).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowItems.forEach { item ->
                    itemContent(item)
                }
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

private fun nearestDialogMinute(value: Int): Int {
    return DialogMinutes.minBy { kotlin.math.abs(it - value) }
}

private fun Int.coerceIn(values: List<Int>): Int {
    return values.firstOrNull { it == this } ?: values.minBy { kotlin.math.abs(it - this) }
}

@Composable
private fun SettingsFooterActions(onClose: () -> Unit) {
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
                    .testTag("settings.close")
                    .defaultMinSize(minHeight = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmAccent)
            ) {
                Text("메인으로")
            }
        }
    }
}

@Composable
private fun DebugDataCard(onSeedGraphData: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PremiumPanel)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionBadge(label = "DEBUG")
                Text(
                    "테스트 데이터",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PremiumInk
                )
                Text(
                    "최근 30일 그래프 확인용 더미 기록을 추가합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PremiumSubtle
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconActionButton(
                    label = "그래프 더미 데이터 추가",
                    onClick = onSeedGraphData,
                    testTag = "settings.debug.seed_graph"
                )
            }
        }
    }
}

/**
 * 설정 화면의 공통 행.
 * 좌측에는 옵션명과 스위치, 우측에는 해당 옵션의 입력 필드를 둔다.
 */
@Composable
private fun SettingRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PremiumPanel),
        border = BorderStroke(1.dp, CalendarGridLine.copy(alpha = 0.74f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompactTextMode()) 2.dp else 0.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            val compactText = isCompactTextMode()
            val useStackedLayout = maxWidth < 560.dp || compactText
            val toggleRow: @Composable (Modifier) -> Unit = { rowModifier ->
                Column(
                    modifier = rowModifier,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
                            modifier = Modifier.weight(1f),
                            color = PremiumInk,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = checked,
                            onCheckedChange = onCheckedChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PremiumGlass,
                                checkedTrackColor = WarmAccent,
                                uncheckedThumbColor = PremiumSubtle,
                                uncheckedTrackColor = CalendarGridLine
                            )
                        )
                    }
                    if (!compactText) {
                        Text(
                            description,
                            style = MaterialTheme.typography.bodySmall,
                            color = PremiumSubtle
                        )
                    }
                }
            }

            if (useStackedLayout) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    toggleRow(Modifier.fillMaxWidth())
                    content(Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    toggleRow(Modifier.weight(1f))
                    content(Modifier.width(168.dp))
                }
            }
        }
    }
}
