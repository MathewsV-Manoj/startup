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
import com.startup.focuno.domain.model.FocusMode
import com.startup.focuno.domain.model.FocusPlan
import com.startup.focuno.domain.model.FocusSound
import com.startup.focuno.domain.usecase.ComputeDayStatsUseCase
import com.startup.focuno.domain.usecase.StreakCalculator
import com.startup.focuno.service.focus.FocusAlarms
import com.startup.focuno.service.sound.FocusSoundPlayer
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
    /** The focus plan in progress (including a Pomodoro break), or null when idle. */
    val plan: FocusPlan? = null,
    val sessionStrict: Boolean = false,
    val sessionSubject: String = "",
    val subjects: List<String> = emptyList(),
    val focusSound: FocusSound = FocusSound.OFF,
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
    private val soundPlayer: FocusSoundPlayer,
    private val focusAlarms: FocusAlarms,
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
            val plan = settings.focusPlan?.takeIf { it.isRunning(System.currentTimeMillis()) }

            _state.value = FocusUiState(
                isLoading = false,
                hasUsageAccess = hasAccess,
                today = stats,
                topDistractions = top,
                streak = settings.streakCount,
                goalMinutes = settings.dailyGoalMinutes,
                week = week,
                protection = protection,
                plan = plan,
                sessionStrict = plan != null && settings.quickBlockStrict,
                sessionSubject = session?.subject.orEmpty(),
                subjects = settings.subjects,
                focusSound = settings.focusSound,
                updatedAtMs = System.currentTimeMillis(),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _state.update { it.copy(isLoading = false, hasError = true) }
        }
    }

    /** [amount] is minutes for a timer and rounds for a Pomodoro; a stopwatch ignores it. */
    fun startFocusSession(mode: FocusMode, amount: Int, strict: Boolean, subject: String) {
        viewModelScope.launch {
            // The pause screen shows this, so "Signals" says more than "Focus session".
            val label = subject.ifBlank { context.getString(R.string.quick_block_label_focus) }
            val now = System.currentTimeMillis()
            val plan = when (mode) {
                FocusMode.TIMER -> FocusPlan(FocusMode.TIMER, now, now + amount * 60_000L)
                FocusMode.POMODORO -> FocusPlan.pomodoro(now, FocusPlan.POMODORO_FOCUS_MIN, FocusPlan.POMODORO_BREAK_MIN, amount)
                FocusMode.STOPWATCH -> FocusPlan(FocusMode.STOPWATCH, now, now + FocusSessionRepository.STOPWATCH_MAX_MS)
            }
            // A stopwatch is meant to be stopped by hand, so it is never strict.
            focusSessions.start(plan, label, strict && mode != FocusMode.STOPWATCH, subject)
            soundPlayer.play(settingsStore.settings.first().focusSound, untilMs = plan.endMs)
            focusAlarms.scheduleNext(plan, now)
            refresh()
        }
    }

    /**
     * Saves the choice. During a timer the new sound plays at once; otherwise a short preview plays so the
     * person can hear what they picked.
     */
    fun setFocusSound(sound: FocusSound) {
        viewModelScope.launch {
            settingsStore.setFocusSound(sound)
            val now = System.currentTimeMillis()
            val endsAt = settingsStore.settings.first().focusPlan?.takeIf { it.isRunning(now) }?.endMs
            soundPlayer.play(sound, untilMs = endsAt ?: (now + PREVIEW_MS))
            refresh()
        }
    }

    fun addSubject(name: String) {
        val clean = name.trim().take(MAX_SUBJECT_LENGTH)
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val current = settingsStore.settings.first().subjects
            if (current.none { it.equals(clean, ignoreCase = true) }) settingsStore.setSubjects(current + clean)
            refresh()
        }
    }

    fun removeSubject(name: String) {
        viewModelScope.launch {
            settingsStore.setSubjects(settingsStore.settings.first().subjects - name)
            refresh()
        }
    }

    /** Does nothing during a strict session: the repository refuses to end it early. */
    fun stopFocusSession() {
        viewModelScope.launch {
            if (focusSessions.stopEarly()) {
                soundPlayer.stop()
                focusAlarms.cancel()
            }
            refresh()
        }
    }

    private companion object {
        const val MAX_SUBJECT_LENGTH = 24
        const val PREVIEW_MS = 8_000L
    }
}
