package com.startup.focuno.domain.usecase

import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.repository.BypassRepository
import com.startup.focuno.data.repository.FocusSessionRepository
import com.startup.focuno.data.repository.SummaryRepository
import com.startup.focuno.data.repository.UsageTrackingRepository
import com.startup.focuno.domain.model.BypassOutcome
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Rolls finished days into summaries, banks each finished day's points once, updates the streak and prunes
 * raw rows older than 90 days. Runs at 00:05 from WorkManager and again whenever the app opens, so a
 * skipped night is repaired.
 */
class RunDailyRollupUseCase @Inject constructor(
    private val computeDayStats: ComputeDayStatsUseCase,
    private val summaryRepository: SummaryRepository,
    private val usageRepository: UsageTrackingRepository,
    private val settingsStore: SettingsStore,
    private val focusSessions: FocusSessionRepository,
    private val bypassRepository: BypassRepository,
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now()) {
        focusSessions.finalizeDue()
        if (usageRepository.hasUsageAccess()) {
            val settings = settingsStore.settings.first()
            val bankedThrough = settings.xpBankedThrough?.let { LocalDate.parse(it) }
            val firstDay = today.minusDays(BACKFILL_DAYS)
            val lastFinishedDay = today.minusDays(1)
            val existing = summaryRepository.existingDates(firstDay, lastFinishedDay)
            var day = firstDay
            while (!day.isAfter(lastFinishedDay)) {
                // Yesterday is always re-rolled: the first run after midnight may have seen partial events.
                val needsSave = day.toString() !in existing || day == lastFinishedDay
                val needsBank = bankedThrough == null || day.isAfter(bankedThrough)
                if (needsSave || needsBank) {
                    val stats = computeDayStats(day)
                    if (needsSave) summaryRepository.saveDay(stats)
                    if (needsBank) {
                        val zone = ZoneId.systemDefault()
                        val from = day.atStartOfDay(zone).toInstant().toEpochMilli()
                        val to = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                        val xp = GameRules.dayXp(
                            focusScore = stats.focusScore,
                            totalMs = stats.totalMs,
                            goalMinutes = settings.dailyGoalMinutes,
                            sessionsCompleted = focusSessions.completedBetween(from, to),
                            urgesResisted = bypassRepository.countBetween(BypassOutcome.ABANDONED, from, to),
                        )
                        settingsStore.bankXp(xp, day.toString())
                    }
                }
                day = day.plusDays(1)
            }
        }
        updateStreak(today)
        summaryRepository.pruneBefore(today.minusDays(RETENTION_DAYS))
    }

    private suspend fun updateStreak(today: LocalDate) {
        val goalMinutes = settingsStore.settings.first().dailyGoalMinutes
        val lastFinishedDay = today.minusDays(1)
        val summaries = summaryRepository.summaries(lastFinishedDay.minusDays(RETENTION_DAYS), lastFinishedDay)
            .associateBy { it.date }
        val streak = StreakCalculator.currentStreak(lastFinishedDay, goalMinutes, summaries)
        settingsStore.setStreak(streak, if (streak > 0) lastFinishedDay.toString() else null)
    }

    private companion object {
        const val BACKFILL_DAYS = 7L
        const val RETENTION_DAYS = 90L
    }
}
