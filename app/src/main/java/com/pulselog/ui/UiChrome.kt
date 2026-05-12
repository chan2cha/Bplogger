package com.pulselog.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 앱 공통 상단 제목 바.
 */
@Composable
internal fun Header(
    title: String,
    onExportClick: () -> Unit,
    exportActive: Boolean,
    onSettingsClick: () -> Unit,
    settingsActive: Boolean
) {
    val compactText = isCompactTextMode()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (compactText) 14.dp else 20.dp, vertical = if (compactText) 10.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compactText) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Favorite,
            contentDescription = null,
            modifier = Modifier.size(if (compactText) 22.dp else 26.dp),
            tint = WarmAccent
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (compactText) "Pulse" else title,
                style = if (compactText) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = PremiumInk
            )
            if (!compactText) {
                Text(
                    text = "Daily blood pressure tracker",
                    style = MaterialTheme.typography.labelMedium,
                    color = PremiumSubtle
                )
            }
        }
        IconButton(onClick = onExportClick) {
            Icon(
                imageVector = if (exportActive) Icons.Outlined.Close else Icons.Outlined.IosShare,
                contentDescription = if (exportActive) "내보내기 닫기" else "내보내기 열기",
                modifier = Modifier.size(if (compactText) 22.dp else 24.dp),
                tint = if (exportActive) WarmAccent else PremiumSubtle
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = if (settingsActive) Icons.Outlined.Close else Icons.Outlined.Settings,
                contentDescription = if (settingsActive) "설정 닫기" else "설정 열기",
                modifier = Modifier.size(if (compactText) 22.dp else 24.dp),
                tint = if (settingsActive) WarmAccent else PremiumSubtle
            )
        }
    }
}

/**
 * 입력 필드 바깥을 탭하면 현재 포커스를 해제한다.
 * 포커스 해제 시 시스템 키보드도 함께 내려간다.
 */
internal fun Modifier.clearFocusOnTap(focusManager: FocusManager): Modifier = composed {
    val currentFocusManager = remember(focusManager) { focusManager }
    pointerInput(currentFocusManager) {
        detectTapGestures(
            onTap = { currentFocusManager.clearFocus(force = true) }
        )
    }
}

/**
 * 커스텀 탭/칩/셀에서 Material 기본 회색 ripple 대신 화면 자체의 선택 상태만 사용한다.
 */
internal fun Modifier.softClickable(
    shape: Shape,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    clip(shape)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * ViewModel에서 전달한 일회성 메시지를 화면 상단에 노출한다.
 */
@Composable
internal fun MessageBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("닫기")
                }
            }
        }
    }
}
