package com.startup.focuno.service.monitor

import com.startup.focuno.domain.model.RawUsageEvent
import com.startup.focuno.domain.model.UsageEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundTrackerTest {

    private fun resumed(t: Long, pkg: String, cls: String = "Main") = RawUsageEvent(t, UsageEventType.ACTIVITY_RESUMED, pkg, cls)
    private fun paused(t: Long, pkg: String, cls: String = "Main") = RawUsageEvent(t, UsageEventType.ACTIVITY_PAUSED, pkg, cls)
    private fun screenOff(t: Long) = RawUsageEvent(t, UsageEventType.SCREEN_NON_INTERACTIVE, null, null)

    @Test
    fun resumedAppBecomesForeground() {
        val tracker = ForegroundTracker()
        assertTrue(tracker.apply(listOf(resumed(1, "insta"))))
        assertEquals("insta", tracker.current)
    }

    @Test
    fun switchingAppsFollowsTheNewestResume() {
        val tracker = ForegroundTracker()
        tracker.apply(listOf(resumed(1, "insta"), paused(2, "insta"), resumed(3, "launcher")))
        assertEquals("launcher", tracker.current)
    }

    @Test
    fun holdsTheAppWhenNothingNewHappens() {
        val tracker = ForegroundTracker()
        tracker.apply(listOf(resumed(1, "insta")))
        assertFalse(tracker.apply(emptyList()))
        assertEquals("insta", tracker.current)
    }

    @Test
    fun closingTheTopAppFallsBackToTheOneUnderneath() {
        val tracker = ForegroundTracker()
        tracker.apply(listOf(resumed(1, "a"), resumed(2, "b")))
        tracker.apply(listOf(paused(3, "b")))
        assertEquals("a", tracker.current)
    }

    @Test
    fun screenOffClearsEverything() {
        val tracker = ForegroundTracker()
        tracker.apply(listOf(resumed(1, "insta")))
        assertTrue(tracker.apply(listOf(screenOff(2))))
        assertNull(tracker.current)
    }

    @Test
    fun replayingOverlappingEventsGivesTheSameAnswer() {
        val tracker = ForegroundTracker()
        val events = listOf(resumed(1, "a"), paused(2, "a"), resumed(3, "b"))
        tracker.apply(events)
        assertFalse(tracker.apply(events))
        assertEquals("b", tracker.current)
    }

    @Test
    fun moveBetweenScreensOfOneAppKeepsItForeground() {
        val tracker = ForegroundTracker()
        tracker.apply(listOf(resumed(1, "insta", "Feed"), resumed(2, "insta", "Profile"), paused(3, "insta", "Feed")))
        assertEquals("insta", tracker.current)
    }

    @Test
    fun aSeededAppIsClosedByItsFirstPause() {
        val tracker = ForegroundTracker()
        tracker.reset("insta")
        assertEquals("insta", tracker.current)
        tracker.apply(listOf(paused(5, "insta", "SomeActivity")))
        assertNull(tracker.current)
    }
}
