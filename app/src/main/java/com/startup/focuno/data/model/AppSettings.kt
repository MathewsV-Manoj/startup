package com.startup.focuno.data.model

import com.startup.focuno.domain.model.AppLimit
import com.startup.focuno.domain.model.FocusMode
import com.startup.focuno.domain.model.FocusPlan
import com.startup.focuno.domain.model.FocusSound
import com.startup.focuno.domain.model.NudgeSensitivity

data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val onboardingStep: Int = 0,
    val nudgesEnabled: Boolean = true,
    val nudgeSensitivity: NudgeSensitivity = NudgeSensitivity.MEDIUM,
    /** Reserved. The app is dark-only by design, so this is stored but never switches the theme. */
    val theme: String = "dark",
    val dailyGoalMinutes: Int = 240,
    /** How long the person wants to focus each day. */
    val studyGoalMinutes: Int = 240,
    /** The exam the person is preparing for, e.g. "GATE 2027", and its date as "2027-02-06". */
    val examName: String = "",
    val examDate: String? = null,
    val streakCount: Int = 0,
    val lastActiveDate: String? = null,
    /** Epoch ms until which every distracting app is blocked ("Block now" or a focus session). 0 = off. */
    val quickBlockUntilMs: Long = 0L,
    val quickBlockLabel: String = "",
    /** A strict focus session cannot be ended early and its pause screen has no unlock. */
    val quickBlockStrict: Boolean = false,
    /** Start of the running focus plan, and its shape. 0 start means a plan saved by an older version. */
    val quickBlockStartMs: Long = 0L,
    val focusMode: FocusMode = FocusMode.TIMER,
    val focusRoundMs: Long = 0L,
    val focusBreakMs: Long = 0L,
    val lastNudgeAtMs: Long = 0L,
    val nudgeDay: String = "",
    val nudgeCountToday: Int = 0,
    /** Goal thresholds already announced today, e.g. "2026-10-04:50". */
    val goalThresholdsHit: Set<String> = emptySet(),
    /** Points from every finished day, added once when the day is rolled up. */
    val xpBanked: Int = 0,
    val xpBankedThrough: String? = null,
    val badgesUnlocked: Set<String> = emptySet(),
    /** Highest level whose celebration has already been shown. */
    val celebratedLevel: Int = 1,
    /** The person's own study subjects, in the order they added them. */
    val subjects: List<String> = emptyList(),
    /** Background sound that plays during a focus timer. */
    val focusSound: FocusSound = FocusSound.OFF,
    /** Monday of the last week a weekly report was sent for, as "2026-09-28". */
    val weeklyReportWeek: String? = null,
    /** One daily budget for all time-eater apps together, in minutes. 0 means off. */
    val budgetMinutes: Int = 0,
    val budgetStrict: Boolean = false,
    /** Lock mode: during focus, pause every app except [focusAllowed] (and the always-allowed essentials). */
    val focusLockAll: Boolean = false,
    val focusAllowed: Set<String> = emptySet(),
) {
    /** The time-eater budget as a limit, so it follows exactly the same rules as a per-app limit. */
    val timeEaterBudget: AppLimit?
        get() = if (budgetMinutes > 0) AppLimit(BUDGET_KEY, budgetMinutes, budgetStrict) else null

    companion object {
        const val BUDGET_KEY = "*time-eaters*"
    }

    /** The focus timer in progress, or null. Pomodoro breaks are part of the plan. */
    val focusPlan: FocusPlan?
        get() {
            if (quickBlockUntilMs <= 0L) return null
            return when (focusMode) {
                FocusMode.POMODORO -> FocusPlan(FocusMode.POMODORO, quickBlockStartMs, quickBlockUntilMs, focusRoundMs, focusBreakMs)
                else -> FocusPlan(focusMode, quickBlockStartMs, quickBlockUntilMs)
            }
        }
}
