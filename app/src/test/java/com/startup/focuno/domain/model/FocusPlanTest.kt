package com.startup.focuno.domain.model

import com.startup.focuno.data.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusPlanTest {

    private val min = 60_000L
    private val start = 1_000_000L

    @Test
    fun pomodoroAlternatesFocusAndBreaks() {
        val plan = FocusPlan.pomodoro(start, roundMinutes = 25, breakMinutes = 5, rounds = 4)
        assertEquals(start + (4 * 25 + 3 * 5) * min, plan.endMs)
        assertEquals(4, plan.rounds)

        assertTrue(plan.isFocusing(start))
        assertTrue(plan.isFocusing(start + 24 * min))
        assertFalse(plan.isFocusing(start + 26 * min))
        assertTrue(plan.isFocusing(start + 31 * min))

        val brk = plan.phaseAt(start + 27 * min)!!
        assertFalse(brk.focusing)
        assertEquals(1, brk.round)
        assertEquals(start + 30 * min, brk.endsAtMs)

        val last = plan.phaseAt(start + 100 * min)!!
        assertTrue(last.focusing)
        assertEquals(4, last.round)
        assertEquals(plan.endMs, last.endsAtMs)
    }

    @Test
    fun nothingIsRunningOutsideThePlan() {
        val plan = FocusPlan.pomodoro(start, 25, 5, 2)
        assertNull(plan.phaseAt(start - 1))
        assertNull(plan.phaseAt(plan.endMs))
        assertFalse(plan.isFocusing(plan.endMs + min))
    }

    @Test
    fun timerIsOneLongFocusRound() {
        val plan = FocusPlan(FocusMode.TIMER, start, start + 45 * min)
        assertEquals(1, plan.rounds)
        val phase = plan.phaseAt(start + 30 * min)!!
        assertTrue(phase.focusing)
        assertEquals(start + 45 * min, phase.endsAtMs)
    }

    @Test
    fun settingsRebuildThePlanThatWasSaved() {
        val saved = FocusPlan.pomodoro(start, 25, 5, 3)
        val settings = AppSettings(
            quickBlockStartMs = saved.startMs,
            quickBlockUntilMs = saved.endMs,
            focusMode = FocusMode.POMODORO,
            focusRoundMs = saved.roundMs,
            focusBreakMs = saved.breakMs,
        )
        assertEquals(saved, settings.focusPlan)
        assertNull(AppSettings().focusPlan)
    }
}
