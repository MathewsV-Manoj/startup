package com.startup.focuno.domain.usecase

import com.startup.focuno.data.room.DailySummaryEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

/** Last week in a few numbers, for the Monday notification. */
data class WeeklyReport(
    val weekStart: LocalDate,
    val averageDailyMs: Long,
    /** Change in the daily average against the week before, in percent; null without data for that week. */
    val changePercent: Int?,
    val focusMs: Long,
    val daysUnderGoal: Int,
)

object WeeklyReportRules {
    /** The report goes out from Monday at this hour, so it does not arrive in the middle of the night. */
    const val SEND_FROM_HOUR = 8

    fun thisWeekStart(today: LocalDate): LocalDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /**
     * The Monday of the week to report on, if a report is due now: once per week, from Monday morning on,
     * and never for a week that was already reported.
     */
    fun weekDue(now: LocalDateTime, lastReportedWeekStart: LocalDate?): LocalDate? {
        val thisMonday = thisWeekStart(now.toLocalDate())
        if (now.toLocalDate() == thisMonday && now.hour < SEND_FROM_HOUR) return null
        val lastWeek = thisMonday.minusWeeks(1)
        return lastWeek.takeIf { lastReportedWeekStart == null || lastReportedWeekStart.isBefore(lastWeek) }
    }

    /** Null when the week has no recorded days, so nobody gets a report of zeros. */
    fun build(
        weekStart: LocalDate,
        week: List<DailySummaryEntity>,
        weekBefore: List<DailySummaryEntity>,
        focusMs: Long,
        goalMinutes: Int,
    ): WeeklyReport? {
        val days = week.filter { it.totalMs > 0 }
        if (days.isEmpty()) return null
        val average = days.sumOf { it.totalMs } / days.size
        val before = weekBefore.filter { it.totalMs > 0 }
        val change = if (before.isEmpty()) {
            null
        } else {
            val previous = before.sumOf { it.totalMs } / before.size
            if (previous <= 0L) null else ((average - previous) * 100.0 / previous).roundToInt()
        }
        return WeeklyReport(
            weekStart = weekStart,
            averageDailyMs = average,
            changePercent = change,
            focusMs = focusMs,
            daysUnderGoal = days.count { StreakCalculator.metGoal(it, goalMinutes) },
        )
    }
}
