package com.startup.focuno.domain.usecase

import com.startup.focuno.domain.model.BadgeId
import com.startup.focuno.domain.model.GameStats
import com.startup.focuno.domain.model.QuestKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRulesTest {

    private val minute = 60_000L

    @Test
    fun levelsStartAtOneAndFollowTheCurve() {
        assertEquals(1, GameRules.levelFor(0).level)
        assertEquals(1, GameRules.levelFor(99).level)
        assertEquals(2, GameRules.levelFor(100).level)
        assertEquals(3, GameRules.levelFor(300).level)
        assertEquals(5, GameRules.levelFor(1000).level)
    }

    @Test
    fun progressInsideALevelIsAFraction() {
        val progress = GameRules.levelFor(200) // level 2 spans 100..300
        assertEquals(2, progress.level)
        assertEquals(100, progress.xpIntoLevel)
        assertEquals(200, progress.xpForNextLevel)
        assertEquals(0.5f, progress.fraction, 0.001f)
    }

    @Test
    fun levelNamesStopAtTheLastTitle() {
        assertEquals(9, GameRules.levelFor(GameRules.xpToReach(40)).titleIndex)
        assertEquals(0, GameRules.levelFor(0).titleIndex)
    }

    @Test
    fun actionPointsAreCappedPerDay() {
        assertEquals(30 + 10, GameRules.actionXp(1, 1))
        assertEquals(4 * 30 + 5 * 10, GameRules.actionXp(50, 50))
    }

    @Test
    fun finishedDayAddsScoreGoalBonusAndActions() {
        // score 80, within a 4h goal, one session, two urges resisted
        val xp = GameRules.dayXp(focusScore = 80, totalMs = 200 * minute, goalMinutes = 240, sessionsCompleted = 1, urgesResisted = 2)
        assertEquals(80 + 40 + 30 + 20, xp)
    }

    @Test
    fun overTheGoalLosesOnlyTheBonusNeverGoesNegative() {
        val xp = GameRules.dayXp(focusScore = 50, totalMs = 300 * minute, goalMinutes = 240, sessionsCompleted = 0, urgesResisted = 0)
        assertEquals(50, xp)
        assertEquals(0, GameRules.dayXp(null, 0, 240, 0, 0))
    }

    @Test
    fun staysUnderGoalQuestIsNeverDoneDuringTheDay() {
        val quests = Quests.today(sessionsToday = 0, urgesToday = 0, totalMsToday = 60 * minute, goalMinutes = 240)
        val goal = quests.first { it.kind == QuestKind.STAY_UNDER_GOAL }
        assertFalse(goal.done)
        assertFalse(goal.overGoal)
        assertEquals(180L, goal.minutesLeft)
        assertEquals(0.25f, goal.progress, 0.001f)
    }

    @Test
    fun overTheGoalIsFlaggedGently() {
        val goal = Quests.today(0, 0, 300 * minute, 240).first { it.kind == QuestKind.STAY_UNDER_GOAL }
        assertTrue(goal.overGoal)
        assertEquals(0L, goal.minutesLeft)
        assertEquals(1f, goal.progress, 0.001f)
    }

    @Test
    fun actionQuestsCompleteOnTheFirstAction() {
        val quests = Quests.today(sessionsToday = 1, urgesToday = 1, totalMsToday = 0, goalMinutes = 240)
        assertTrue(quests.first { it.kind == QuestKind.FOCUS_SESSION }.done)
        assertTrue(quests.first { it.kind == QuestKind.RESIST_URGE }.done)
    }

    @Test
    fun badgesUnlockAtTheirThresholds() {
        val fresh = Badges.earned(GameStats(level = 1, streak = 0, sessionsTotal = 0, urgesTotal = 0, blockHitsTotal = 0))
        assertEquals(setOf(BadgeId.FIRST_STEPS), fresh)

        val strong = Badges.earned(GameStats(level = 5, streak = 7, sessionsTotal = 10, urgesTotal = 5, blockHitsTotal = 3))
        assertTrue(strong.containsAll(listOf(BadgeId.FOCUS_STARTER, BadgeId.FOCUS_PRO, BadgeId.STREAK_3, BadgeId.STREAK_7, BadgeId.URGE_FIGHTER, BadgeId.SHIELD_UP, BadgeId.RISING_STAR)))
        assertFalse(strong.contains(BadgeId.STREAK_30))
        assertFalse(strong.contains(BadgeId.LEGEND))
    }

    @Test
    fun focusBadgesComeFromTheBestDayAndTheTotal() {
        val hour = 3_600_000L
        val base = GameStats(level = 1, streak = 0, sessionsTotal = 0, urgesTotal = 0, blockHitsTotal = 0)
        assertFalse(BadgeId.DEEP_WORK in Badges.earned(base.copy(bestDayFocusMs = 2 * hour - 1)))
        assertTrue(BadgeId.DEEP_WORK in Badges.earned(base.copy(bestDayFocusMs = 2 * hour)))
        assertTrue(BadgeId.GOAL_GETTER in Badges.earned(base.copy(bestDayFocusMs = hour, studyGoalMs = hour)))
        assertFalse(BadgeId.GOAL_GETTER in Badges.earned(base.copy(bestDayFocusMs = 0, studyGoalMs = 0)))
        assertTrue(BadgeId.FOCUS_MASTER in Badges.earned(base.copy(sessionsTotal = 50)))
    }
}
