package com.startup.focuno.domain.usecase

import com.startup.focuno.domain.model.RawUsageEvent
import com.startup.focuno.domain.model.UsageEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageEventWalkerTest {

    private val minute = 60_000L
    private val hour = 60 * minute
    private val dayStart = 0L
    private val dayEnd = 24 * hour

    private fun resumed(at: Long, pkg: String, cls: String = "Main") =
        RawUsageEvent(at, UsageEventType.ACTIVITY_RESUMED, pkg, cls)

    private fun paused(at: Long, pkg: String, cls: String = "Main") =
        RawUsageEvent(at, UsageEventType.ACTIVITY_PAUSED, pkg, cls)

    private fun stopped(at: Long, pkg: String, cls: String = "Main") =
        RawUsageEvent(at, UsageEventType.ACTIVITY_STOPPED, pkg, cls)

    private fun screenOff(at: Long) = RawUsageEvent(at, UsageEventType.SCREEN_NON_INTERACTIVE, null, null)
    private fun screenOn(at: Long) = RawUsageEvent(at, UsageEventType.SCREEN_INTERACTIVE, null, null)

    @Test
    fun simpleSessionIsCounted() {
        val result = UsageEventWalker.walk(
            listOf(resumed(10 * hour, "a"), paused(10 * hour + 10 * minute, "a")),
            dayStart,
            dayEnd,
        )
        assertEquals(10 * minute, result.foregroundMsByPackage["a"])
        assertEquals(1, result.openCountByPackage["a"])
    }

    @Test
    fun screenOffMidSessionStopsTheClock() {
        // App foregrounded at 10:00, phone locked at 10:05 with no PAUSED event, unlocked at 14:00.
        val result = UsageEventWalker.walk(
            listOf(
                resumed(10 * hour, "a"),
                screenOff(10 * hour + 5 * minute),
                screenOn(14 * hour),
            ),
            dayStart,
            dayEnd,
        )
        assertEquals(5 * minute, result.foregroundMsByPackage["a"])
        assertEquals(1, result.pickupCount)
    }

    @Test
    fun screenOffClosesEveryOpenSession() {
        val result = UsageEventWalker.walk(
            listOf(
                resumed(10 * hour, "a"),
                resumed(10 * hour, "b"),
                screenOff(10 * hour + 2 * minute),
            ),
            dayStart,
            dayEnd,
        )
        assertEquals(2 * minute, result.foregroundMsByPackage["a"])
        assertEquals(2 * minute, result.foregroundMsByPackage["b"])
    }

    @Test
    fun resumedWhileScreenOffIsIgnored() {
        val result = UsageEventWalker.walk(
            listOf(
                screenOff(9 * hour),
                resumed(9 * hour + minute, "a"),
                screenOn(12 * hour),
            ),
            dayStart,
            dayEnd,
        )
        assertTrue(result.foregroundMsByPackage["a"] == null)
    }

    @Test
    fun appStillOpenWhenScreenIsOnIsClosedAtWindowEnd() {
        val now = 11 * hour
        val result = UsageEventWalker.walk(listOf(resumed(10 * hour + 30 * minute, "a")), dayStart, now)
        assertEquals(30 * minute, result.foregroundMsByPackage["a"])
    }

    @Test
    fun moveBetweenScreensOfOneAppKeepsTimeAndCountsOneOpen() {
        // Typical order: old screen paused, new screen resumed a few ms later.
        val result = UsageEventWalker.walk(
            listOf(
                resumed(10 * hour, "a", "Feed"),
                paused(10 * hour + 5 * minute, "a", "Feed"),
                resumed(10 * hour + 5 * minute + 40, "a", "Profile"),
                paused(10 * hour + 8 * minute, "a", "Profile"),
            ),
            dayStart,
            dayEnd,
        )
        assertEquals(1, result.openCountByPackage["a"])
        assertEquals(8 * minute - 40, result.foregroundMsByPackage["a"]!! - 0)
    }

    @Test
    fun newScreenResumedBeforeOldOnePausedDoesNotCloseTheSession() {
        val result = UsageEventWalker.walk(
            listOf(
                resumed(10 * hour, "a", "Feed"),
                resumed(10 * hour + 5 * minute, "a", "Profile"),
                paused(10 * hour + 5 * minute + 10, "a", "Feed"),
                paused(10 * hour + 9 * minute, "a", "Profile"),
            ),
            dayStart,
            dayEnd,
        )
        assertEquals(9 * minute, result.foregroundMsByPackage["a"])
        assertEquals(1, result.openCountByPackage["a"])
    }

    @Test
    fun reopeningAnAppLaterCountsAnotherOpen() {
        val result = UsageEventWalker.walk(
            listOf(
                resumed(10 * hour, "a"), paused(10 * hour + minute, "a"),
                resumed(11 * hour, "a"), paused(11 * hour + minute, "a"),
            ),
            dayStart,
            dayEnd,
        )
        assertEquals(2, result.openCountByPackage["a"])
        assertEquals(2 * minute, result.foregroundMsByPackage["a"])
    }

    @Test
    fun stoppedActsAsSafetyNetWhenPausedIsMissing() {
        val result = UsageEventWalker.walk(
            listOf(resumed(10 * hour, "a"), stopped(10 * hour + 4 * minute, "a")),
            dayStart,
            dayEnd,
        )
        assertEquals(4 * minute, result.foregroundMsByPackage["a"])
    }

    @Test
    fun sessionOpenAtMidnightIsClippedToTheWindow() {
        // Resumed 23:30 the day before (event fetched from the lookback), paused 00:20 today.
        val result = UsageEventWalker.walk(
            listOf(resumed(-30 * minute, "a"), paused(20 * minute, "a")),
            dayStart,
            dayEnd,
        )
        assertEquals(20 * minute, result.foregroundMsByPackage["a"])
        assertEquals(null, result.openCountByPackage["a"])
    }

    @Test
    fun totalNeverExceedsTheElapsedWindow() {
        // Two overlapping resumes of different apps for the full window is fine, but one app can never exceed it.
        val windowEnd = 2 * hour
        val result = UsageEventWalker.walk(
            listOf(resumed(0, "a", "X"), resumed(0, "a", "Y")),
            dayStart,
            windowEnd,
        )
        assertEquals(windowEnd, result.foregroundMsByPackage["a"])
        assertTrue(result.clampedPackages.isEmpty())
    }

    @Test
    fun eventsAfterTheWindowAreIgnored() {
        val result = UsageEventWalker.walk(
            listOf(resumed(5 * hour, "a"), paused(7 * hour, "a")),
            dayStart,
            6 * hour,
        )
        assertEquals(hour, result.foregroundMsByPackage["a"])
    }

    @Test
    fun pickupsAreCountedFromScreenOnEvents() {
        val result = UsageEventWalker.walk(
            listOf(screenOn(8 * hour), screenOff(8 * hour + minute), screenOn(9 * hour), screenOff(9 * hour + minute)),
            dayStart,
            dayEnd,
        )
        assertEquals(2, result.pickupCount)
    }
}
