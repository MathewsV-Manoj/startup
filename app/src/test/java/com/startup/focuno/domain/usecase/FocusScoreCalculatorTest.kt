package com.startup.focuno.domain.usecase

import com.startup.focuno.domain.model.UsageSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class FocusScoreCalculatorTest {

    private val minute = 60_000L

    @Test
    fun noDataGivesNullNotAPerfectScore() {
        assertNull(FocusScoreCalculator.score(0, 0, 0))
    }

    @Test
    fun noDistractingUseScoresOneHundred() {
        assertEquals(100, FocusScoreCalculator.score(120 * minute, 0, 0))
    }

    @Test
    fun scoreStaysInformativeAtExtremes() {
        val worst = FocusScoreCalculator.score(600 * minute, 600 * minute, 500)!!
        assertEquals(15, worst)
    }

    @Test
    fun manyShortPickupsScoreWorseThanOneLongSitting() {
        val oneSitting = FocusScoreCalculator.score(120 * minute, 60 * minute, 1)!!
        val fragmented = FocusScoreCalculator.score(120 * minute, 60 * minute, 50)!!
        assertTrue(fragmented < oneSitting)
    }

    @Test
    fun formulaMatchesTheSpecificationForAKnownDay() {
        // total 4h, distracting 1h (ratio .25), 20 opens:
        // 100 - 45*.25 - 25*(60/180) - 15*(20/40) = 100 - 11.25 - 8.333 - 7.5 = 72.9 -> 73
        assertEquals(73, FocusScoreCalculator.score(240 * minute, 60 * minute, 20))
    }

    @Test
    fun longestStreakResetsOnDistractingUseAndLongGaps() {
        val sessions = listOf(
            UsageSession("study", 0, 30 * minute),
            UsageSession("study", 32 * minute, 50 * minute),
            UsageSession("insta", 51 * minute, 52 * minute),
            UsageSession("study", 53 * minute, 60 * minute),
            UsageSession("study", 90 * minute, 95 * minute),
        )
        val streak = FocusMetrics.longestFocusStreakMs(sessions) { it == "insta" }
        assertEquals(48 * minute, streak)
    }

    @Test
    fun hourlySplitCrossesHourBoundaries() {
        val hours = FocusMetrics.hourlyMs(listOf(UsageSession("a", 50 * minute, 70 * minute)), ZoneOffset.UTC)
        assertEquals(10 * minute, hours[0])
        assertEquals(10 * minute, hours[1])
    }
}
