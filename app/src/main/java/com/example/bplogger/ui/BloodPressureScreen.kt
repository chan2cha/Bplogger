package com.example.bplogger.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 메인 탭 구성.
 * 앱의 1차 네비게이션은 캘린더, 그래프, 설정 세 화면으로 고정한다.
 */
private enum class MainTab {
    CALENDAR,
    GRAPH,
    SETTINGS
}

/**
 * 앱 전체 UI 진입점.
 * 상단 헤더와 탭 구조를 유지하고, 각 기능 화면은 별도 파일로 위임한다.
 */
@Composable
fun BloodPressureScreen(vm: BpViewModel) {
    var selectedTab by remember { mutableStateOf(MainTab.CALENDAR) }
    var detailOpen by remember { mutableStateOf(false) }
    val message by vm.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        val currentMessage = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = currentMessage)
        vm.clearMessage()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (detailOpen) {
            DayDetailScreen(
                vm = vm,
                onBack = { detailOpen = false }
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Header(title = "Pulse Log")

                    MainSegmentedTabs(
                        selectedTab = selectedTab,
                        onSelect = { selectedTab = it }
                    )

                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (fadeIn() + scaleIn(initialScale = 0.985f)) togetherWith fadeOut() using SizeTransform(
                                clip = false
                            )
                        },
                        label = "mainTabContent"
                    ) { activeTab ->
                        when (activeTab) {
                            MainTab.CALENDAR -> CalendarScreen(
                                vm = vm,
                                onOpenDetail = { detailOpen = true }
                            )

                            MainTab.GRAPH -> GraphScreen(vm = vm)
                            MainTab.SETTINGS -> SettingsScreen(vm = vm)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 기본 TabRow 대신 쓰는 상단 세그먼트 컨트롤.
 * 화면 전체 톤과 맞추기 위해 캡슐형 배경과 떠 있는 선택 패널을 사용한다.
 */
@Composable
private fun MainSegmentedTabs(
    selectedTab: MainTab,
    onSelect: (MainTab) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(PremiumGlass, androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .border(1.dp, CalendarGridLine, androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .padding(6.dp)
    ) {
        val tabCount = MainTab.entries.size
        val gap = 8.dp
        val indicatorWidth = (maxWidth - gap * (tabCount - 1)) / tabCount
        val indicatorOffset by animateDpAsState(
            targetValue = (indicatorWidth + gap) * selectedTab.ordinal,
            animationSpec = spring(
                dampingRatio = 0.92f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "tabIndicatorOffset"
        )
        val indicatorElevation by animateDpAsState(
            targetValue = 6.dp,
            animationSpec = spring(
                dampingRatio = 0.95f,
                stiffness = Spring.StiffnessLow
            ),
            label = "tabIndicatorElevation"
        )

        Box(
            modifier = Modifier
                .padding(start = indicatorOffset)
                .width(indicatorWidth)
                .shadow(
                    elevation = indicatorElevation,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    clip = false
                )
                .background(
                    color = CardTint,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                )
                .border(
                    width = 1.dp,
                    color = WarmAccent.copy(alpha = 0.22f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                )
                .align(Alignment.CenterStart)
                .padding(vertical = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) PremiumInk else PremiumSubtle,
                    label = "tabText"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) WarmAccent else PremiumSubtle,
                    label = "tabIcon"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1f,
                    animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "tabIconScale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (tab) {
                                MainTab.CALENDAR -> Icons.Outlined.CalendarMonth
                                MainTab.GRAPH -> Icons.Outlined.Insights
                                MainTab.SETTINGS -> Icons.Outlined.Notifications
                            },
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                },
                            tint = iconTint
                        )
                        Text(
                            text = when (tab) {
                                MainTab.CALENDAR -> "캘린더"
                                MainTab.GRAPH -> "그래프"
                                MainTab.SETTINGS -> "설정"
                            },
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
