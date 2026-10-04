package com.startup.focuno.domain.usecase

import com.startup.focuno.data.room.DailySummaryEntity
import com.startup.focuno.domain.model.BlockSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleEvaluatorTest {

    private val zone = ZoneId.of("UTC")

    private fun at(date: String, hour: Int, minute: Int = 0): ZonedDateTime =
        LocalDate.parse(date).atTime(hour, minute).atZone(zone)

    private fun schedule(start: Int, end: Int, mask: Int = BlockSchedule.ALL_DAYS, strict: Boolean = false, pkg: String = "ig") =
        BlockSchedule(id = 1, packageName = pkg, startMinuteOfDay = start, endMinuteOfDay = end, daysOfWeekMask = mask, strictMode = strict)

    @Test
    fun daytimeWindowIsActiveInsideAndNotOutside() {
        val s = listOf(schedule(9 * 60, 17 * 60))
        assertNotNull(ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-05", 12)))
        assertNull(ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-05", 8, 59)))
        assertNull(ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-05", 17)))
    }

    @Test
    fun overnightWindowCoversBothSidesOfMidnight() {
        val s = listOf(schedule(22 * 60, 6 * 60))
        assertNotNull(ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-05", 23)))
        assertNotNull(ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-06", 2)))
        assertNull(ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-06", 6)))
        assertNull(ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-06", 12)))
    }

    @Test
    fun overnightWindowEndsAtTheNextMorning() {
        val window = ScheduleEvaluator.activeWindow(listOf(schedule(22 * 60, 6 * 60)), "ig", at("2026-10-05", 23))!!
        assertEquals(at("2026-10-06", 6).toInstant().toEpochMilli(), window.endsAtMs)
    }

    @Test
    fun overnightWindowBelongsToTheDayItStartsOn() {
        // 2026-10-05 is a Monday (bit 0). Only Monday is enabled.
        val s = listOf(schedule(22 * 60, 6 * 60, mask = 0b0000001))
        assertNotNull(ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-05", 23)))
        assertNotNull("Tuesday 02:00 is still Monday's window", ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-06", 2)))
        assertNull("Tuesday 23:00 is Tuesday's window, which is off", ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-06", 23)))
    }

    @Test
    fun disabledDaysAndOtherAppsAreIgnored() {
        val s = listOf(schedule(9 * 60, 17 * 60, mask = 0b0000010))
        assertNull(ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-05", 12)))
        assertNull(ScheduleEvaluator.activeWindow(s, "yt", at("2026-10-06", 12)))
    }

    @Test
    fun multipleWindowsPerDayWork() {
        val s = listOf(schedule(9 * 60, 11 * 60), schedule(14 * 60, 16 * 60))
        assertNotNull(ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-05", 10)))
        assertNull(ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-05", 12)))
        assertNotNull(ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-05", 15)))
    }

    @Test
    fun strictAppliesWhenAnyOverlappingWindowIsStrict() {
        val s = listOf(schedule(9 * 60, 12 * 60), schedule(10 * 60, 11 * 60, strict = true))
        assertTrue(ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-05", 10, 30))!!.strict)
        assertFalse(ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-05", 9, 30))!!.strict)
    }

    @Test
    fun startEqualsEndMeansAllDay() {
        val s = listOf(schedule(8 * 60, 8 * 60))
        assertNotNull(ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-05", 20)))
        assertNotNull(ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-06", 7)))
    }

    @Test
    fun disabledScheduleNeverBlocks() {
        val s = listOf(schedule(0, 23 * 60).copy(enabled = false))
        assertNull(ScheduleEvaluator.activeWindow(s, "ig", at("2026-10-05", 12)))
    }
}

class BypassPolicyTest {

    private val minute = 60_000L

    @Test
    fun unlockedForFiveMinutesOnly() {
        val granted = 1_000_000L
        assertTrue(BypassPolicy.isUnlocked(granted, granted + 4 * minute))
        assertFalse(BypassPolicy.isUnlocked(granted, granted + 5 * minute))
        assertFalse(BypassPolicy.isUnlocked(null, granted))
    }

    @Test
    fun cooldownRunsThirtyMinutesAfterTheUnlockEnds() {
        val granted = 1_000_000L
        assertFalse(BypassPolicy.canRequest(granted, granted + 34 * minute))
        assertTrue(BypassPolicy.canRequest(granted, granted + 35 * minute))
        assertTrue(BypassPolicy.canRequest(null, granted))
    }

    @Test
    fun reasonNeedsFifteenRealCharacters() {
        assertFalse(BypassPolicy.isReasonValid("too short"))
        assertFalse(BypassPolicy.isReasonValid("               x"))
        assertTrue(BypassPolicy.isReasonValid("need to reply to my mentor"))
    }
}

class StreakCalculatorTest {

    private fun summary(date: String, totalMin: Long) = DailySummaryEntity(date, totalMin * 60_000L, 0, 0, 80, 0, 0, 0)

    @Test
    fun streakCountsBackFromTheLastFinishedDayAndStopsAtAGap() {
        val data = listOf(
            summary("2026-10-04", 120),
            summary("2026-10-03", 200),
            summary("2026-10-02", 300),
            summary("2026-10-01", 100),
        ).associateBy { it.date }
        assertEquals(2, StreakCalculator.currentStreak(LocalDate.parse("2026-10-04"), 240, data))
    }

    @Test
    fun missingOrEmptyDaysDoNotCount() {
        val data = listOf(summary("2026-10-04", 0)).associateBy { it.date }
        assertEquals(0, StreakCalculator.currentStreak(LocalDate.parse("2026-10-04"), 240, data))
        assertEquals(0, StreakCalculator.currentStreak(LocalDate.parse("2026-10-05"), 240, data))
    }
}
