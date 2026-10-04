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
import com.startup.focuno.domain.model.FocusSound
import com.startup.focuno.domain.usecase.ComputeDayStatsUseCase
import com.startup.focuno.domain.usecase.StreakCalculator
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
    val sessionStartedAtMs: Long? = null,
    val sessionEndsAtMs: Long? = null,
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

    fun startFocusSession(minutes: Int, strict: Boolean, subject: String) {
        viewModelScope.launch {
            // The pause screen shows this, so "Signals" says more than "Focus session".
            val label = subject.ifBlank { context.getString(R.string.quick_block_label_focus) }
            val startedAt = System.currentTimeMillis()
            focusSessions.start(minutes * 60_000L, label, strict, subject, startedAt)
            soundPlayer.play(settingsStore.settings.first().focusSound, untilMs = startedAt + minutes * 60_000L)
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
            val endsAt = focusSessions.active()?.endTs
            val now = System.currentTimeMillis()
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
            if (focusSessions.stopEarly()) soundPlayer.stop()
            refresh()
        }
    }

    private companion object {
        const val MAX_SUBJECT_LENGTH = 24
        const val PREVIEW_MS = 8_000L
    }
}
