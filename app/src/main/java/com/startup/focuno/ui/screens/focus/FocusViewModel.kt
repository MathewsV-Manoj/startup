package com.startup.focuno.ui.screens.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.repository.FocusSessionRepository
import com.startup.focuno.data.repository.InstalledApp
import com.startup.focuno.data.repository.InstalledAppsRepository
import com.startup.focuno.data.repository.ProtectionRepository
import com.startup.focuno.data.repository.ProtectionStatus
import com.startup.focuno.data.repository.UsageTrackingRepository
import com.startup.focuno.domain.model.DayStats
import com.startup.focuno.domain.model.FocusMode
import com.startup.focuno.domain.model.FocusPlan
import com.startup.focuno.domain.model.FocusSound
import com.startup.focuno.domain.usecase.ComputeDayStatsUseCase
import com.startup.focuno.domain.usecase.ExamCountdown
import com.startup.focuno.service.focus.FocusController
import com.startup.focuno.service.sound.FocusSoundPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
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
import java.time.ZoneId
import javax.inject.Inject

data class FocusUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val hasUsageAccess: Boolean = true,
    val today: DayStats? = null,
    /** Time focused today, from every timer, Pomodoro round and stopwatch. */
    val todayFocusMs: Long = 0L,
    val studyGoalMinutes: Int = 240,
    val examName: String = "",
    /** Days left until the exam, or null when no exam is set or it has passed. */
    val examDaysLeft: Long? = null,
    val protection: ProtectionStatus? = null,
    /** The focus plan in progress (including a Pomodoro break), or null when idle. */
    val plan: FocusPlan? = null,
    val sessionStrict: Boolean = false,
    val sessionSubject: String = "",
    val subjects: List<String> = emptyList(),
    val focusSound: FocusSound = FocusSound.OFF,
    /** Lock mode: pause all apps except [allowedApps] during focus, instead of only time-eaters. */
    val lockAll: Boolean = false,
    val allowedApps: Set<String> = emptySet(),
    /** Apps to choose from in the allow list; loaded only when that dialog opens. */
    val installedApps: List<InstalledApp> = emptyList(),
    val updatedAtMs: Long = 0L,
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val computeDayStats: ComputeDayStatsUseCase,
    private val usageRepository: UsageTrackingRepository,
    private val protectionRepository: ProtectionRepository,
    private val settingsStore: SettingsStore,
    private val installedApps: InstalledAppsRepository,
    private val focusSessions: FocusSessionRepository,
    private val soundPlayer: FocusSoundPlayer,
    private val focusController: FocusController,
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
            val session = focusSessions.active()
            val now = System.currentTimeMillis()
            val plan = settings.focusPlan?.takeIf { it.isRunning(now) }
            val dayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val focusedToday = focusSessions.focusSpans(dayStart, now).sumOf { (start, end) -> (minOf(end, now) - maxOf(start, dayStart)).coerceAtLeast(0L) }

            _state.value = FocusUiState(
                isLoading = false,
                hasUsageAccess = hasAccess,
                today = stats,
                todayFocusMs = focusedToday,
                studyGoalMinutes = settings.studyGoalMinutes,
                examName = settings.examName,
                examDaysLeft = ExamCountdown.daysLeft(ExamCountdown.parse(settings.examDate), today),
                protection = protection,
                plan = plan,
                sessionStrict = plan != null && settings.quickBlockStrict,
                sessionSubject = session?.subject.orEmpty(),
                subjects = settings.subjects,
                focusSound = settings.focusSound,
                lockAll = settings.focusLockAll,
                allowedApps = settings.focusAllowed,
                installedApps = _state.value.installedApps,
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
            focusController.start(mode, amount, strict, subject)
            refresh()
        }
    }

    /** Does nothing during a strict session: the repository refuses to end it early. */
    fun stopFocusSession() {
        viewModelScope.launch {
            focusController.stop()
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

    // These two update the screen at once and save in the background, so ticking boxes feels instant
    // and never waits for a full refresh of today's numbers.
    fun setLockAll(enabled: Boolean) {
        _state.update { it.copy(lockAll = enabled) }
        viewModelScope.launch { settingsStore.setFocusLockAll(enabled) }
    }

    fun toggleAllowed(packageName: String) {
        val current = _state.value.allowedApps
        val updated = if (packageName in current) current - packageName else current + packageName
        _state.update { it.copy(allowedApps = updated) }
        viewModelScope.launch { settingsStore.setFocusAllowed(updated) }
    }

    fun loadInstalledApps() {
        if (_state.value.installedApps.isNotEmpty()) return
        viewModelScope.launch {
            val apps = installedApps.installedApps()
            _state.update { it.copy(installedApps = apps) }
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

    private companion object {
        const val MAX_SUBJECT_LENGTH = 24
        const val PREVIEW_MS = 8_000L
    }
}
