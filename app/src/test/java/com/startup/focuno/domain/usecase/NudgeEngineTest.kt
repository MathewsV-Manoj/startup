package com.startup.focuno.domain.usecase

import com.startup.focuno.domain.model.NudgeSensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class NudgeEngineTest {

    private val minute = 60_000L
    private val today = LocalDate.parse("2026-10-05")
    private val now = 10_000_000_000L

    private fun input(
        enabled: Boolean = true,
        lastNudgeAt: Long = 0,
        nudgeDay: String = "",
        countToday: Int = 0,
        hit: Set<String> = emptySet(),
        goalMin: Int = 240,
        totalMs: Long = 0,
        distracting: Boolean = true,
        justOpened: Boolean = false,
        opens: Int = 0,
        continuousMs: Long = 0,
        sinceFocus: Long? = null,
        sensitivity: NudgeSensitivity = NudgeSensitivity.MEDIUM,
    ) = NudgeInput(
        nowMs = now, today = today, nudgesEnabled = enabled, sensitivity = sensitivity,
        lastNudgeAtMs = lastNudgeAt, nudgeDay = nudgeDay, nudgeCountToday = countToday,
        goalThresholdsHit = hit, dailyGoalMinutes = goalMin, totalTodayMs = totalMs,
        appName = "Instagram", isDistractingForeground = distracting, justOpened = justOpened,
        opensOfAppLastHour = opens, continuousDistractingMs = continuousMs, msSinceFocusSessionEnded = sinceFocus,
    )

    @Test
    fun disabledNeverNudges() {
        assertNull(NudgeEngine.evaluate(input(enabled = false, continuousMs = 60 * minute)))
    }

    @Test
    fun continuousUseTriggersAtTheSensitivityThreshold() {
        assertNull(NudgeEngine.evaluate(input(continuousMs = 14 * minute)))
        assertEquals(NudgeKind.CONTINUOUS_USE, NudgeEngine.evaluate(input(continuousMs = 15 * minute))?.kind)
        assertEquals(NudgeKind.CONTINUOUS_USE, NudgeEngine.evaluate(input(continuousMs = 10 * minute, sensitivity = NudgeSensitivity.HIGH))?.kind)
    }

    @Test
    fun seventhOpenInAnHourTriggersAtMedium() {
        val nudge = NudgeEngine.evaluate(input(justOpened = true, opens = 7))
        assertEquals(NudgeKind.REPEATED_OPENS, nudge?.kind)
        assertEquals(7, nudge?.count)
        assertNull(NudgeEngine.evaluate(input(justOpened = true, opens = 6)))
    }

    @Test
    fun openingSoonAfterAFocusSessionIsCalledOut() {
        assertEquals(NudgeKind.AFTER_FOCUS_SESSION, NudgeEngine.evaluate(input(justOpened = true, sinceFocus = 3 * minute))?.kind)
        assertNull(NudgeEngine.evaluate(input(justOpened = true, sinceFocus = 6 * minute)))
    }

    @Test
    fun goalThresholdsFireOnceEachAndHighestFirst() {
        val seventyFivePercent = 180 * minute
        assertEquals(75, NudgeEngine.evaluate(input(distracting = false, totalMs = seventyFivePercent))?.percent)
        val afterHit = setOf(NudgeEngine.goalKey(today, 75), NudgeEngine.goalKey(today, 50))
        assertNull(NudgeEngine.evaluate(input(distracting = false, totalMs = seventyFivePercent, hit = afterHit)))
        assertEquals(100, NudgeEngine.evaluate(input(distracting = false, totalMs = 241 * minute, hit = afterHit))?.percent)
    }

    @Test
    fun rateLimitIsTwentyMinutesAndSixPerDay() {
        val recent = now - 19 * minute
        assertNull(NudgeEngine.evaluate(input(continuousMs = 30 * minute, lastNudgeAt = recent)))
        val allowed = now - 21 * minute
        assertNotNull(NudgeEngine.evaluate(input(continuousMs = 30 * minute, lastNudgeAt = allowed)))
        assertNull(NudgeEngine.evaluate(input(continuousMs = 30 * minute, lastNudgeAt = allowed, nudgeDay = today.toString(), countToday = 6)))
    }

    @Test
    fun yesterdaysCountDoesNotCarryOver() {
        val nudge = NudgeEngine.evaluate(input(continuousMs = 30 * minute, nudgeDay = "2026-10-04", countToday = 6))
        assertNotNull(nudge)
    }
}
