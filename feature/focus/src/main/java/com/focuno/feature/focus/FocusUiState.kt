package com.focuno.feature.focus

import com.focuno.core.model.AppUsage
import com.focuno.core.model.FocusBand

data class FocusUiState(
    val loading: Boolean = true,
    val score: Int = 0,
    val band: FocusBand = FocusBand.POOR,
    val goalMinutes: Int = 120,
    val screenMinutes: Int = 0,
    val topApps: List<AppUsage> = emptyList(),
    val hasUsageAccess: Boolean = false,
)
