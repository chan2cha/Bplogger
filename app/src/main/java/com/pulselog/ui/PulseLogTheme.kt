package com.pulselog.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val PulseLogLightColors = lightColorScheme(
    primary = WarmAccent,
    onPrimary = Color.White,
    primaryContainer = CardTint,
    onPrimaryContainer = PremiumInk,
    secondary = Color(0xFF8A5866),
    onSecondary = Color.White,
    secondaryContainer = PremiumPanel,
    onSecondaryContainer = PremiumInk,
    tertiary = Color(0xFF16A085),
    onTertiary = Color.White,
    background = WarmPaper,
    onBackground = PremiumInk,
    surface = WarmPaper,
    onSurface = PremiumInk,
    surfaceVariant = PremiumGlass,
    onSurfaceVariant = PremiumSubtle,
    outline = CalendarGridLine,
    error = Color(0xFFC44555),
    onError = Color.White
)

/**
 * 앱 전체 Material 기본값.
 * Material 버튼/아이콘의 기본 회색 ripple을 앱 포인트 컬러 기반의 연한 피드백으로 통일한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PulseLogTheme(
    content: @Composable () -> Unit
) {
    val rippleAlpha = if (isSystemInDarkTheme()) 0.18f else 0.12f

    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(
            color = WarmAccent,
            rippleAlpha = androidx.compose.material.ripple.RippleAlpha(
                draggedAlpha = rippleAlpha,
                focusedAlpha = rippleAlpha,
                hoveredAlpha = rippleAlpha * 0.7f,
                pressedAlpha = rippleAlpha
            )
        )
    ) {
        MaterialTheme(
            colorScheme = PulseLogLightColors,
            content = content
        )
    }
}
