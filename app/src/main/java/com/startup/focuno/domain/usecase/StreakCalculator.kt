package com.startup.focuno.domain.usecase

import com.startup.focuno.data.room.DailySummaryEntity
import java.time.LocalDate

/**
 * A day counts toward the streak when total screen time stayed within the daily goal.
 * A day with no recorded usage does not count: empty data is a gap, not a success.
 */
object StreakCalculator {

    fun metGoal(summary: DailySummaryEntity?, goalMinutes: Int): Boolean =
        summary != null && summary.totalMs > 0 && summary.totalMs <= goalMinutes * 60_000L

    /** Consecutive qualifying days ending at [lastFinishedDay] (normally yesterday). */
    fun currentStreak(lastFinishedDay: LocalDate, goalMinutes: Int, summariesByDate: Map<String, DailySummaryEntity>): Int {
        var streak = 0
        var day = lastFinishedDay
        while (metGoal(summariesByDate[day.toString()], goalMinutes)) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }
}
