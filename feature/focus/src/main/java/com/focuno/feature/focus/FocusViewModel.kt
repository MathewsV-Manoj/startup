package com.focuno.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focuno.core.domain.usecase.ObserveDailyUsageUseCase
import com.focuno.core.domain.usecase.ObserveFocusScoreUseCase
import com.focuno.core.domain.usecase.ObserveProfileUseCase
import com.focuno.core.domain.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class FocusViewModel @Inject constructor(
    observeDailyUsage: ObserveDailyUsageUseCase,
    observeFocusScore: ObserveFocusScoreUseCase,
    observeProfile: ObserveProfileUseCase,
    private val usageRepository: UsageRepository,
) : ViewModel() {

    private val today = LocalDate.now()

    val uiState: StateFlow<FocusUiState> = combine(
        observeDailyUsage(today),
        observeFocusScore(today),
        observeProfile(),
    ) { usage, score, profile ->
        FocusUiState(
            loading = false,
            score = score?.score ?: 0,
            band = score?.band ?: com.focuno.core.model.FocusBand.POOR,
            goalMinutes = profile.dailyFocusGoalMinutes,
            screenMinutes = usage.sumOf { it.totalTimeInForeground.toMinutes().toInt() },
            topApps = usage.sortedByDescending { it.totalTimeInForeground }.take(5),
            hasUsageAccess = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FocusUiState(),
    )

    fun refreshPermissionState() {
        viewModelScope.launch {
            // Surfaced to the UI via a one-shot event once permission flow is wired.
            usageRepository.hasUsageAccess()
        }
    }
}
