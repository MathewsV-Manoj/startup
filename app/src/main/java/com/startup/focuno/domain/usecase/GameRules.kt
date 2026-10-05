package com.startup.focuno.domain.usecase

import com.startup.focuno.domain.model.BadgeId
import com.startup.focuno.domain.model.GameStats
import com.startup.focuno.domain.model.LevelProgress
import com.startup.focuno.domain.model.Quest
import com.startup.focuno.domain.model.QuestKind

/**
 * Points never go down. Today's live points come only from things you DO (finish a focus timer, say no to
 * an urge), so a bad scrolling hour can never make the number drop in front of you. The day's focus score
 * and the "stayed under goal" bonus are added when the day is over.
 */
object GameRules {
    const val SESSION_XP = 30
    const val URGE_XP = 10
    const val GOAL_BONUS_XP = 40
    const val MAX_SESSIONS_PER_DAY = 4
    const val MAX_URGES_PER_DAY = 5
    const val LEVEL_TITLE_COUNT = 10

    /** Points from actions in one day, capped so they cannot be farmed. */
    fun actionXp(sessionsCompleted: Int, urgesResisted: Int): Int =
        minOf(sessionsCompleted, MAX_SESSIONS_PER_DAY) * SESSION_XP + minOf(urgesResisted, MAX_URGES_PER_DAY) * URGE_XP

    /** The full value of a finished day. */
    fun dayXp(focusScore: Int?, totalMs: Long, goalMinutes: Int, sessionsCompleted: Int, urgesResisted: Int): Int {
        val scoreXp = focusScore ?: 0
        val goalXp = if (totalMs > 0 && totalMs <= goalMinutes * 60_000L) GOAL_BONUS_XP else 0
        return scoreXp + goalXp + actionXp(sessionsCompleted, urgesResisted)
    }

    /** Total points needed to reach [level]: 0, 100, 300, 600, 1000, 1500 ... */
    fun xpToReach(level: Int): Int = 50 * (level - 1) * level

    fun levelFor(totalXp: Int): LevelProgress {
        var level = 1
        while (totalXp >= xpToReach(level + 1)) level++
        val floor = xpToReach(level)
        val next = xpToReach(level + 1)
        return LevelProgress(
            level = level,
            xpIntoLevel = totalXp - floor,
            xpForNextLevel = next - floor,
            titleIndex = (level - 1).coerceAtMost(LEVEL_TITLE_COUNT - 1),
        )
    }
}

object Quests {
    fun today(sessionsToday: Int, urgesToday: Int, totalMsToday: Long, goalMinutes: Int): List<Quest> {
        val goalMs = (goalMinutes * 60_000L).coerceAtLeast(1L)
        val usedFraction = (totalMsToday.toFloat() / goalMs).coerceIn(0f, 1f)
        val over = totalMsToday > goalMs
        return listOf(
            Quest(QuestKind.FOCUS_SESSION, progress = if (sessionsToday >= 1) 1f else 0f, done = sessionsToday >= 1, rewardXp = GameRules.SESSION_XP),
            Quest(
                QuestKind.STAY_UNDER_GOAL,
                progress = usedFraction,
                done = false,
                rewardXp = GameRules.GOAL_BONUS_XP,
                overGoal = over,
                minutesLeft = if (over) 0 else (goalMs - totalMsToday) / 60_000L,
            ),
            Quest(QuestKind.RESIST_URGE, progress = if (urgesToday >= 1) 1f else 0f, done = urgesToday >= 1, rewardXp = GameRules.URGE_XP),
        )
    }
}

object Badges {
    fun earned(stats: GameStats): Set<BadgeId> = buildSet {
        add(BadgeId.FIRST_STEPS)
        if (stats.sessionsTotal >= 1) add(BadgeId.FOCUS_STARTER)
        if (stats.sessionsTotal >= 10) add(BadgeId.FOCUS_PRO)
        if (stats.streak >= 3) add(BadgeId.STREAK_3)
        if (stats.streak >= 7) add(BadgeId.STREAK_7)
        if (stats.streak >= 30) add(BadgeId.STREAK_30)
        if (stats.urgesTotal >= 5) add(BadgeId.URGE_FIGHTER)
        if (stats.urgesTotal >= 25) add(BadgeId.URGE_MASTER)
        if (stats.blockHitsTotal >= 1) add(BadgeId.SHIELD_UP)
        if (stats.level >= 5) add(BadgeId.RISING_STAR)
        if (stats.level >= 10) add(BadgeId.LEGEND)
        if (stats.bestDayFocusMs >= DEEP_WORK_MS) add(BadgeId.DEEP_WORK)
        if (stats.studyGoalMs > 0 && stats.bestDayFocusMs >= stats.studyGoalMs) add(BadgeId.GOAL_GETTER)
        if (stats.sessionsTotal >= 50) add(BadgeId.FOCUS_MASTER)
    }

    /** Two hours of focus in one day. */
    const val DEEP_WORK_MS = 2 * 60 * 60_000L
}
