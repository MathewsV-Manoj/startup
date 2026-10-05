package com.startup.focuno.domain.model

enum class BadgeId {
    FIRST_STEPS,
    FOCUS_STARTER,
    FOCUS_PRO,
    STREAK_3,
    STREAK_7,
    STREAK_30,
    URGE_FIGHTER,
    URGE_MASTER,
    SHIELD_UP,
    RISING_STAR,
    LEGEND,
    DEEP_WORK,
    GOAL_GETTER,
    FOCUS_MASTER,
}

enum class QuestKind { FOCUS_SESSION, STAY_UNDER_GOAL, RESIST_URGE }

/**
 * One of today's missions. STAY_UNDER_GOAL never shows as "done" during the day, because the day is not
 * over: it shows how much of the goal is used, and pays out when the day closes.
 */
data class Quest(
    val kind: QuestKind,
    val progress: Float,
    val done: Boolean,
    val rewardXp: Int,
    val overGoal: Boolean = false,
    /** For STAY_UNDER_GOAL: minutes left before the goal, or 0 when over it. */
    val minutesLeft: Long = 0,
)

data class LevelProgress(
    val level: Int,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int,
    /** Index 0..9 into the list of level names. */
    val titleIndex: Int,
) {
    val fraction: Float get() = if (xpForNextLevel <= 0) 0f else (xpIntoLevel.toFloat() / xpForNextLevel).coerceIn(0f, 1f)
}

data class GameStats(
    val level: Int,
    val streak: Int,
    val sessionsTotal: Int,
    val urgesTotal: Int,
    val blockHitsTotal: Int,
    /** Most focus time in any one day of the last weeks. */
    val bestDayFocusMs: Long = 0L,
    /** The daily study goal; 0 when none is set. */
    val studyGoalMs: Long = 0L,
)
