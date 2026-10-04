package com.startup.focuno.domain.usecase

import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.repository.SummaryRepository
import com.startup.focuno.data.repository.UsageTrackingRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Rolls finished days into summaries, updates the streak and prunes raw rows older than 90 days.
 * Runs at 00:05 from WorkManager and again whenever the app opens, so a skipped night is repaired.
 */
class RunDailyRollupUseCase @Inject constructor(
    private val computeDayStats: ComputeDayStatsUseCase,
    private val summaryRepository: SummaryRepository,
    private val usageRepository: UsageTrackingRepository,
    private val settingsStore: SettingsStore,
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now()) {
        if (usageRepository.hasUsageAccess()) {
            val firstDay = today.minusDays(BACKFILL_DAYS)
            val lastFinishedDay = today.minusDays(1)
            val existing = summaryRepository.existingDates(firstDay, lastFinishedDay)
            var day = firstDay
            while (!day.isAfter(lastFinishedDay)) {
                // Yesterday is always re-rolled: the first run after midnight may have seen partial events.
                if (day.toString() !in existing || day == lastFinishedDay) {
                    summaryRepository.saveDay(computeDayStats(day))
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
