package com.startup.focuno.data.model

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
    val streakCount: Int = 0,
    val lastActiveDate: String? = null,
    /** Epoch ms until which every distracting app is blocked ("Block now" or a focus session). 0 = off. */
    val quickBlockUntilMs: Long = 0L,
    val quickBlockLabel: String = "",
    /** A strict focus session cannot be ended early and its pause screen has no unlock. */
    val quickBlockStrict: Boolean = false,
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
)
