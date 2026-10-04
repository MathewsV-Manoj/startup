package com.startup.focuno.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startup.focuno.data.repository.InstalledAppsRepository
import com.startup.focuno.data.repository.UsageTrackingRepository
import com.startup.focuno.domain.usecase.ComputeDayStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class UsageLine(val packageName: String, val label: String, val foregroundMs: Long)

data class UsageCheckUiState(
    val isLoading: Boolean = true,
    val hasAccess: Boolean = true,
    val hasError: Boolean = false,
    val totalMs: Long = 0,
    val counted: List<UsageLine> = emptyList(),
    val notCounted: List<UsageLine> = emptyList(),
    val updatedAtMs: Long = 0,
)

@HiltViewModel
class UsageCheckViewModel @Inject constructor(
    private val usageRepository: UsageTrackingRepository,
    private val computeDayStats: ComputeDayStatsUseCase,
    private val installedApps: InstalledAppsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UsageCheckUiState())
    val state: StateFlow<UsageCheckUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            try {
                if (!usageRepository.hasUsageAccess()) {
                    _state.update { it.copy(isLoading = false, hasAccess = false) }
                    return@launch
                }
                val stats = computeDayStats(LocalDate.now())
                _state.value = UsageCheckUiState(
                    isLoading = false,
                    totalMs = stats.totalMs,
                    counted = stats.apps.map { UsageLine(it.packageName, installedApps.label(it.packageName), it.foregroundMs) },
                    notCounted = stats.excludedApps.map { UsageLine(it.packageName, installedApps.label(it.packageName), it.foregroundMs) },
                    updatedAtMs = System.currentTimeMillis(),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }
}
