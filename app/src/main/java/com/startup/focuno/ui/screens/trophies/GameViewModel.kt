package com.startup.focuno.ui.screens.trophies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.repository.BypassRepository
import com.startup.focuno.data.repository.FocusSessionRepository
import com.startup.focuno.data.repository.UsageTrackingRepository
import com.startup.focuno.domain.model.BadgeId
import com.startup.focuno.domain.model.BypassOutcome
import com.startup.focuno.domain.model.GameStats
import com.startup.focuno.domain.model.LevelProgress
import com.startup.focuno.domain.model.Quest
import com.startup.focuno.domain.usecase.Badges
import com.startup.focuno.domain.usecase.ComputeDayStatsUseCase
import com.startup.focuno.domain.usecase.FocusCalendar
import com.startup.focuno.domain.usecase.GameRules
import com.startup.focuno.domain.usecase.Quests
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class BadgeUi(val id: BadgeId, val unlocked: Boolean)

/** A pop-up to show once: a new level, new badges, or both. */
data class Celebration(val newLevel: LevelProgress?, val newBadges: List<BadgeId>)

data class GameUiState(
    val isLoading: Boolean = true,
    val totalXp: Int = 0,
    val level: LevelProgress = GameRules.levelFor(0),
    val todayActionXp: Int = 0,
    val quests: List<Quest> = emptyList(),
    val badges: List<BadgeUi> = BadgeId.entries.map { BadgeUi(it, false) },
    val streak: Int = 0,
    val celebration: Celebration? = null,
    /** FocusCalendar.WEEKS columns of 7 days; null marks days still to come. */
    val calendar: List<List<Long?>> = emptyList(),
    val calendarTotalMs: Long = 0L,
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val focusSessions: FocusSessionRepository,
    private val bypassRepository: BypassRepository,
    private val usageRepository: UsageTrackingRepository,
    private val computeDayStats: ComputeDayStatsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var latestLevel = 1
    private var latestEarned: Set<BadgeId> = emptySet()

    fun refresh() {
        viewModelScope.launch {
            try {
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun load() {
        val settings = settingsStore.settings.first()
        focusSessions.finalizeDue()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val now = System.currentTimeMillis()
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = now + 1

        val sessionsToday = focusSessions.completedBetween(dayStart, end)
        val urgesToday = bypassRepository.countBetween(BypassOutcome.ABANDONED, dayStart, end)
        val sessionsTotal = focusSessions.completedBetween(0, end)
        val urgesTotal = bypassRepository.countBetween(BypassOutcome.ABANDONED, 0, end)
        val hitsTotal = bypassRepository.blockHitsTotal()
        val todayMs = if (usageRepository.hasUsageAccess()) computeDayStats(today).totalMs else 0L

        val calendarStart = FocusCalendar.firstDay(today).atStartOfDay(zone).toInstant().toEpochMilli()
        val focusByDay = FocusCalendar.msByDay(focusSessions.focusSpans(calendarStart, now), zone)

        val actionXp = GameRules.actionXp(sessionsToday, urgesToday)
        val totalXp = settings.xpBanked + actionXp
        val level = GameRules.levelFor(totalXp)
        val earned = Badges.earned(
            GameStats(
                level = level.level,
                streak = settings.streakCount,
                sessionsTotal = sessionsTotal,
                urgesTotal = urgesTotal,
                blockHitsTotal = hitsTotal,
                bestDayFocusMs = focusByDay.values.maxOrNull() ?: 0L,
                studyGoalMs = settings.studyGoalMinutes * 60_000L,
            ),
        )
        val unlocked = settings.badgesUnlocked
        val newBadges = BadgeId.entries.filter { it in earned && it.name !in unlocked }

        latestLevel = level.level
        latestEarned = earned

        val celebration = if (level.level > settings.celebratedLevel || newBadges.isNotEmpty()) {
            Celebration(newLevel = level.takeIf { it.level > settings.celebratedLevel }, newBadges = newBadges)
        } else {
            null
        }

        _state.value = GameUiState(
            isLoading = false,
            totalXp = totalXp,
            level = level,
            todayActionXp = actionXp,
            quests = Quests.today(sessionsToday, urgesToday, todayMs, settings.dailyGoalMinutes),
            badges = BadgeId.entries.map { BadgeUi(it, it in earned || it.name in unlocked) },
            streak = settings.streakCount,
            celebration = celebration,
            calendar = FocusCalendar.grid(focusByDay, today),
            calendarTotalMs = focusByDay.values.sum(),
        )
    }

    /** Remember that the pop-up was seen, so a level or badge is celebrated exactly once. */
    fun dismissCelebration() {
        _state.update { it.copy(celebration = null) }
        viewModelScope.launch {
            val settings = settingsStore.settings.first()
            settingsStore.setCelebrated(
                level = maxOf(settings.celebratedLevel, latestLevel),
                badges = settings.badgesUnlocked + latestEarned.map { it.name },
            )
        }
    }
}
