package com.focuno.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focuno.core.domain.usecase.ObserveFocusScoreUseCase
import com.focuno.core.domain.usecase.ObserveRecentScoresUseCase
import com.focuno.core.model.FocusScore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class AnalyticsUiState(
    val today: FocusScore? = null,
    val recent: List<FocusScore> = emptyList(),
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    observeFocusScore: ObserveFocusScoreUseCase,
    observeRecentScores: ObserveRecentScoresUseCase,
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = combine(
        observeFocusScore(LocalDate.now()),
        observeRecentScores(30),
    ) { today, recent ->
        AnalyticsUiState(today = today, recent = recent)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState(),
    )
}
