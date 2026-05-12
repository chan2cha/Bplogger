package com.pulselog.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pulselog.BuildConfig

/**
 * 알림 설정 탭.
 */
@Composable
internal fun SettingsScreen(
    vm: BpViewModel,
    onClose: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val settings by vm.notificationSettings.collectAsState()

    var morningEnabled by remember { mutableStateOf(settings.morningEnabled) }
    var morningTime by remember { mutableStateOf(settings.morningTime) }
    var eveningEnabled by remember { mutableStateOf(settings.eveningEnabled) }
    var eveningTime by remember { mutableStateOf(settings.eveningTime) }
    var repeatEnabled by remember { mutableStateOf(settings.repeatEnabled) }
    var repeatCount by remember { mutableStateOf(settings.repeatCount.toString()) }

    // 저장된 설정이 바뀌면 로컬 폼 상태도 즉시 맞춘다.
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
            .animateContentSize()
            .imePadding()
            .clearFocusOnTap(focusManager)
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
                    val compactText = isCompactTextMode()
                    SectionBadge(label = "SETTINGS")
                    Text("알림 설정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PremiumInk)
                    if (!compactText) {
                        Text(
                            "빠른 입력과 동일한 톤으로 알림 옵션을 관리합니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PremiumSubtle
                        )
                    }
                }
                SettingRow(
                    label = "아침 알림 사용",
                    checked = morningEnabled,
                    onCheckedChange = { morningEnabled = it }
                ) { fieldModifier ->
                    PremiumInputField(
                        value = morningTime,
                        onValueChange = { morningTime = it },
                        label = "아침",
                        suffix = "HH:mm",
                        keyboardType = KeyboardType.Text,
                        modifier = fieldModifier
                    )
                }
                SettingRow(
                    label = "저녁 알림 사용",
                    checked = eveningEnabled,
                    onCheckedChange = { eveningEnabled = it }
                ) { fieldModifier ->
                    PremiumInputField(
                        value = eveningTime,
                        onValueChange = { eveningTime = it },
                        label = "저녁",
                        suffix = "HH:mm",
                        keyboardType = KeyboardType.Text,
                        modifier = fieldModifier
                    )
                }
                SettingRow(
                    label = "재알림 사용",
                    checked = repeatEnabled,
                    onCheckedChange = { repeatEnabled = it }
                ) { fieldModifier ->
                    PremiumInputField(
                        value = repeatCount,
                        onValueChange = { repeatCount = it.filter(Char::isDigit) },
                        label = "재알림",
                        suffix = "회",
                        keyboardType = KeyboardType.Number,
                        modifier = fieldModifier
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconActionButton(
                        label = "설정 저장",
                        onClick = {
                            vm.saveNotificationSettings(
                                morningEnabled = morningEnabled,
                                morningTime = morningTime,
                                eveningEnabled = eveningEnabled,
                                eveningTime = eveningTime,
                                repeatEnabled = repeatEnabled,
                                repeatCountInput = repeatCount
                            )
                        }
                    )
                }
            }
        }

        if (BuildConfig.DEBUG) {
            DebugDataCard(
                onSeedGraphData = vm::seedGraphDemoData
            )
        }

        SettingsFooterActions(onClose = onClose)
    }
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
                    onClick = onSeedGraphData
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
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = CardTint)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            val compactText = isCompactTextMode()
            val useStackedLayout = maxWidth < 520.dp || compactText
            val toggleRow: @Composable (Modifier) -> Unit = { rowModifier ->
                Row(
                    modifier = rowModifier,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        color = WarmAccent,
                        fontWeight = FontWeight.SemiBold
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
            }

            if (useStackedLayout) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    toggleRow(Modifier.fillMaxWidth())
                    content(Modifier.widthIn(max = if (compactText) 160.dp else 220.dp))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    toggleRow(Modifier.weight(1f))
                    content(Modifier.width(142.dp))
                }
            }
        }
    }
}
