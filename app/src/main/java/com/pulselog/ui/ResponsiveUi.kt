package com.pulselog.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

@Composable
internal fun isCompactTextMode(): Boolean {
    return LocalDensity.current.fontScale >= 1.15f
}
