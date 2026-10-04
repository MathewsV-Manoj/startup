package com.startup.focuno.domain.usecase

import com.startup.focuno.domain.model.AppLimit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DailyLimitPolicyTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val day = LocalDate.parse("2026-10-05")
    private val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
    private val min = 60_000L

    @Test
    fun timeKeepsCountingWhileTheAppIsInFront() {
        val used = DailyLimitPolicy.usedNowMs(measuredMs = 10 * min, measuredAtMs = dayStart + 60 * min, nowMs = dayStart + 63 * min, inFront = true, dayStartMs = dayStart)
        assertEquals(13 * min, used)
    }

    @Test
    fun timeStopsCountingInTheBackground() {
        val used = DailyLimitPolicy.usedNowMs(10 * min, dayStart + 60 * min, dayStart + 90 * min, inFront = false, dayStartMs = dayStart)
        assertEquals(10 * min, used)
    }

    @Test
    fun yesterdaysMeasurementCountsAsNothingAfterMidnight() {
        val beforeMidnight = dayStart - 5 * min
        assertEquals(0L, DailyLimitPolicy.usedNowMs(45 * min, beforeMidnight, dayStart + 2 * min, inFront = false, dayStartMs = dayStart))
        assertEquals(2 * min, DailyLimitPolicy.usedNowMs(45 * min, beforeMidnight, dayStart + 2 * min, inFront = true, dayStartMs = dayStart))
    }

    @Test
    fun reachedOnlyAtOrPastTheBudgetAndOnlyWhenEnabled() {
        val limit = AppLimit("ig", dailyMinutes = 30)
        assertFalse(DailyLimitPolicy.isReached(limit, 29 * min))
        assertTrue(DailyLimitPolicy.isReached(limit, 30 * min))
        assertFalse(DailyLimitPolicy.isReached(limit.copy(enabled = false), 90 * min))
    }

    @Test
    fun warnsInTheLastFiveMinutesOnly() {
        val limit = AppLimit("ig", dailyMinutes = 30)
        assertFalse(DailyLimitPolicy.shouldWarn(limit, 20 * min))
        assertTrue(DailyLimitPolicy.shouldWarn(limit, 26 * min))
        assertFalse(DailyLimitPolicy.shouldWarn(limit, 30 * min))
    }

    @Test
    fun strictLimitIsLockedUntilMidnightOnceUsedUp() {
        val now = day.atTime(20, 0).atZone(zone)
        val midnight = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val strict = AppLimit("ig", dailyMinutes = 30, strict = true)
        assertEquals(midnight, DailyLimitPolicy.lockedUntilMs(strict, 31 * min, now))
        assertNull(DailyLimitPolicy.lockedUntilMs(strict, 10 * min, now))
        assertNull(DailyLimitPolicy.lockedUntilMs(strict.copy(strict = false), 31 * min, now))
    }
}
