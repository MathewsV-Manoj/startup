package com.startup.focuno.ui.screens.apps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startup.focuno.data.repository.AppCategoryRepository
import com.startup.focuno.data.repository.InstalledAppsRepository
import com.startup.focuno.data.repository.UsageTrackingRepository
import com.startup.focuno.domain.model.AppCategory
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

data class AppRow(
    val packageName: String,
    val label: String,
    val foregroundMs: Long,
    val openCount: Int,
    val category: AppCategory,
    /** 0..1 against the busiest app of the day, for the proportional bar. */
    val fraction: Float,
)

data class AppsUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val hasUsageAccess: Boolean = true,
    val dayOffset: Int = 0,
    val days: List<LocalDate> = emptyList(),
    val rows: List<AppRow> = emptyList(),
    val totalMs: Long = 0,
    val productiveMs: Long = 0,
    val neutralMs: Long = 0,
    val distractingMs: Long = 0,
    val focusScore: Int? = null,
)

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val computeDayStats: ComputeDayStatsUseCase,
    private val usageRepository: UsageTrackingRepository,
    private val categoryRepository: AppCategoryRepository,
    private val installedApps: InstalledAppsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AppsUiState(dayOffset = savedState.get<Int>(KEY_DAY_OFFSET) ?: 0))
    val state: StateFlow<AppsUiState> = _state.asStateFlow()

    fun selectDay(offset: Int) {
        savedState[KEY_DAY_OFFSET] = offset
        _state.update { it.copy(dayOffset = offset, isLoading = true) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val offset = _state.value.dayOffset
            val today = LocalDate.now()
            val days = (0..6).map { today.minusDays(it.toLong()) }
            try {
                if (!usageRepository.hasUsageAccess()) {
                    _state.update { it.copy(isLoading = false, hasUsageAccess = false, days = days, rows = emptyList()) }
                    return@launch
                }
                val stats = computeDayStats(today.minusDays(offset.toLong()))
                val max = stats.apps.maxOfOrNull { it.foregroundMs }?.coerceAtLeast(1L) ?: 1L
                val rows = stats.apps.map {
                    AppRow(it.packageName, installedApps.label(it.packageName), it.foregroundMs, it.openCount, it.category, it.foregroundMs.toFloat() / max)
                }
                _state.update {
                    // Ignore a result for a day the person has already moved away from.
                    if (it.dayOffset != offset) {
                        it
                    } else {
                        it.copy(
                            isLoading = false,
                            hasError = false,
                            hasUsageAccess = true,
                            days = days,
                            rows = rows,
                            totalMs = stats.totalMs,
                            productiveMs = stats.productiveMs,
                            neutralMs = stats.neutralMs,
                            distractingMs = stats.distractingMs,
                            focusScore = stats.focusScore,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, hasError = true, days = days) }
            }
        }
    }

    fun setCategory(packageName: String, category: AppCategory) {
        viewModelScope.launch {
            categoryRepository.setCategory(packageName, category)
            refresh()
        }
    }

    private companion object {
        const val KEY_DAY_OFFSET = "day_offset"
    }
}
