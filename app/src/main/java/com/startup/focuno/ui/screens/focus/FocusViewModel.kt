package com.startup.focuno.ui.screens.focus

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startup.focuno.R
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.repository.FocusSessionRepository
import com.startup.focuno.data.repository.InstalledAppsRepository
import com.startup.focuno.data.repository.ProtectionRepository
import com.startup.focuno.data.repository.ProtectionStatus
import com.startup.focuno.data.repository.SummaryRepository
import com.startup.focuno.data.repository.UsageTrackingRepository
import com.startup.focuno.domain.model.AppDayUsage
import com.startup.focuno.domain.model.DayStats
import com.startup.focuno.domain.usecase.ComputeDayStatsUseCase
import com.startup.focuno.domain.usecase.StreakCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

enum class DayOutcome { MET_GOAL, OVER_GOAL, NO_DATA, TODAY }

data class WeekDay(val date: LocalDate, val outcome: DayOutcome)

data class TopDistraction(val usage: AppDayUsage, val label: String)

data class FocusUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val hasUsageAccess: Boolean = true,
    val today: DayStats? = null,
    val topDistractions: List<TopDistraction> = emptyList(),
    val streak: Int = 0,
    val goalMinutes: Int = 240,
    val week: List<WeekDay> = emptyList(),
    val protection: ProtectionStatus? = null,
    val sessionStartedAtMs: Long? = null,
    val sessionEndsAtMs: Long? = null,
    val sessionStrict: Boolean = false,
    val updatedAtMs: Long = 0L,
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val computeDayStats: ComputeDayStatsUseCase,
    private val usageRepository: UsageTrackingRepository,
    private val protectionRepository: ProtectionRepository,
    private val summaryRepository: SummaryRepository,
    private val settingsStore: SettingsStore,
    private val installedApps: InstalledAppsRepository,
    private val focusSessions: FocusSessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FocusUiState())
    val state: StateFlow<FocusUiState> = _state.asStateFlow()
    private val refreshLock = Mutex()

    fun refresh() {
        viewModelScope.launch {
            if (!refreshLock.tryLock()) return@launch
            try {
                load()
            } finally {
                refreshLock.unlock()
            }
        }
    }

    private suspend fun load() {
        try {
            val settings = settingsStore.settings.first()
            val today = LocalDate.now()
            val hasAccess = withContext(Dispatchers.IO) { usageRepository.hasUsageAccess() }
            val protection = withContext(Dispatchers.IO) { protectionRepository.status() }
            val stats = if (hasAccess) computeDayStats(today) else null
            val summaries = summaryRepository.summaries(today.minusDays(6), today.minusDays(1)).associateBy { it.date }
            val week = (6 downTo 1).map { back ->
                val date = today.minusDays(back.toLong())
                val summary = summaries[date.toString()]
                WeekDay(
                    date,
                    when {
                        summary == null || summary.totalMs <= 0 -> DayOutcome.NO_DATA
                        StreakCalculator.metGoal(summary, settings.dailyGoalMinutes) -> DayOutcome.MET_GOAL
                        else -> DayOutcome.OVER_GOAL
                    },
                )
            } + WeekDay(today, DayOutcome.TODAY)
            val top = stats?.apps
                ?.filter { it.category == com.startup.focuno.domain.model.AppCategory.DISTRACTING }
                ?.take(3)
                ?.map { TopDistraction(it, installedApps.label(it.packageName)) }
                .orEmpty()
            val session = focusSessions.active()

            _state.value = FocusUiState(
                isLoading = false,
                hasUsageAccess = hasAccess,
                today = stats,
                topDistractions = top,
                streak = settings.streakCount,
                goalMinutes = settings.dailyGoalMinutes,
                week = week,
                protection = protection,
                sessionStartedAtMs = session?.startTs,
                sessionEndsAtMs = session?.endTs,
                sessionStrict = session != null && settings.quickBlockStrict,
                updatedAtMs = System.currentTimeMillis(),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _state.update { it.copy(isLoading = false, hasError = true) }
        }
    }

    fun startFocusSession(minutes: Int, strict: Boolean) {
        viewModelScope.launch {
            val label = context.getString(R.string.quick_block_label_focus)
            focusSessions.start(minutes * 60_000L, label, strict)
            refresh()
        }
    }

    /** Does nothing during a strict session: the repository refuses to end it early. */
    fun stopFocusSession() {
        viewModelScope.launch {
            focusSessions.stopEarly()
            refresh()
        }
    }
}
