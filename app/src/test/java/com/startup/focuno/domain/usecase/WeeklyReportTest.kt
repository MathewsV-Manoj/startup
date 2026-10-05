package com.startup.focuno.domain.usecase

import com.startup.focuno.data.room.DailySummaryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WeeklyReportTest {

    private val hour = 3_600_000L
    private val monday = LocalDate.parse("2026-10-05")

    private fun day(date: LocalDate, totalMs: Long) = DailySummaryEntity(
        date = date.toString(),
        totalMs = totalMs,
        distractingMs = 0,
        productiveMs = 0,
        focusScore = null,
        longestFocusStreakMs = 0,
        pickupCount = 0,
        distractingOpenCount = 0,
    )

    @Test
    fun dueOnMondayMorningForTheWeekBefore() {
        assertNull(WeeklyReportRules.weekDue(monday.atTime(7, 59), null))
        assertEquals(monday.minusWeeks(1), WeeklyReportRules.weekDue(monday.atTime(8, 0), null))
        // Missed on Monday: still sent later in the week.
        assertEquals(monday.minusWeeks(1), WeeklyReportRules.weekDue(LocalDateTime.of(2026, 10, 8, 1, 0), null))
    }

    @Test
    fun neverTwiceForTheSameWeek() {
        assertNull(WeeklyReportRules.weekDue(monday.atTime(9, 0), monday.minusWeeks(1)))
        assertEquals(monday.minusWeeks(1), WeeklyReportRules.weekDue(monday.atTime(9, 0), monday.minusWeeks(2)))
    }

    @Test
    fun averagesOnlyRecordedDaysAndComparesWithTheWeekBefore() {
        val lastWeek = monday.minusWeeks(1)
        val week = listOf(day(lastWeek, 2 * hour), day(lastWeek.plusDays(1), 4 * hour))
        val before = listOf(day(lastWeek.minusDays(3), 4 * hour))
        val report = WeeklyReportRules.build(lastWeek, week, before, focusMs = 5 * hour, goalMinutes = 180)!!
        assertEquals(3 * hour, report.averageDailyMs)
        assertEquals(-25, report.changePercent)
        assertEquals(1, report.daysUnderGoal)
        assertEquals(5 * hour, report.focusMs)
    }

    @Test
    fun noReportForAnEmptyWeek() {
        assertNull(WeeklyReportRules.build(monday, emptyList(), emptyList(), 0L, 240))
    }
}
