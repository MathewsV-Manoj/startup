package com.startup.focuno.domain.usecase

import com.startup.focuno.domain.model.NudgeSensitivity
import java.time.LocalDate

enum class NudgeKind { CONTINUOUS_USE, REPEATED_OPENS, GOAL_PROGRESS, AFTER_FOCUS_SESSION }

data class Nudge(
    val kind: NudgeKind,
    val appName: String? = null,
    val count: Int = 0,
    val minutes: Int = 0,
    val percent: Int = 0,
)

data class NudgeInput(
    val nowMs: Long,
    val today: LocalDate,
    val nudgesEnabled: Boolean,
    val sensitivity: NudgeSensitivity,
    val lastNudgeAtMs: Long,
    val nudgeDay: String,
    val nudgeCountToday: Int,
    val goalThresholdsHit: Set<String>,
    val dailyGoalMinutes: Int,
    val totalTodayMs: Long,
    val appName: String?,
    val isDistractingForeground: Boolean,
    /** True only on the first check after this app came to the front. */
    val justOpened: Boolean,
    val opensOfAppLastHour: Int,
    val continuousDistractingMs: Long,
    val msSinceFocusSessionEnded: Long?,
)

/**
 * Decides whether a nudge is due. Pure on purpose: the rate limit (one per 20 minutes, six per day)
 * lives here because nudge fatigue is what kills apps like this.
 */
object NudgeEngine {

    const val MIN_GAP_MS = 20 * 60_000L
    const val MAX_PER_DAY = 6
    const val AFTER_FOCUS_WINDOW_MS = 5 * 60_000L
    val GOAL_THRESHOLDS = listOf(100, 75, 50)

    private data class Thresholds(val continuousMs: Long, val opens: Int)

    private fun thresholdsFor(sensitivity: NudgeSensitivity) = when (sensitivity) {
        NudgeSensitivity.LOW -> Thresholds(continuousMs = 20 * 60_000L, opens = 10)
        NudgeSensitivity.MEDIUM -> Thresholds(continuousMs = 15 * 60_000L, opens = 7)
        NudgeSensitivity.HIGH -> Thresholds(continuousMs = 10 * 60_000L, opens = 5)
    }

    fun goalKey(today: LocalDate, percent: Int) = "$today:$percent"

    fun evaluate(input: NudgeInput): Nudge? {
        if (!input.nudgesEnabled) return null
        val countToday = if (input.nudgeDay == input.today.toString()) input.nudgeCountToday else 0
        if (countToday >= MAX_PER_DAY) return null
        if (input.nowMs - input.lastNudgeAtMs < MIN_GAP_MS) return null

        val thresholds = thresholdsFor(input.sensitivity)
        val distracting = input.isDistractingForeground

        if (distracting && input.justOpened) {
            val sinceFocus = input.msSinceFocusSessionEnded
            if (sinceFocus != null && sinceFocus in 0..AFTER_FOCUS_WINDOW_MS) {
                return Nudge(NudgeKind.AFTER_FOCUS_SESSION, appName = input.appName)
            }
            val opens = input.opensOfAppLastHour
            if (opens >= thresholds.opens && opens % thresholds.opens == 0) {
                return Nudge(NudgeKind.REPEATED_OPENS, appName = input.appName, count = opens)
            }
        }

        if (distracting && input.continuousDistractingMs >= thresholds.continuousMs) {
            return Nudge(
                NudgeKind.CONTINUOUS_USE,
                appName = input.appName,
                minutes = (input.continuousDistractingMs / 60_000L).toInt(),
            )
        }

        if (input.dailyGoalMinutes > 0) {
            val percentUsed = input.totalTodayMs * 100 / (input.dailyGoalMinutes * 60_000L)
            val crossed = GOAL_THRESHOLDS.firstOrNull { percentUsed >= it && goalKey(input.today, it) !in input.goalThresholdsHit }
            if (crossed != null) return Nudge(NudgeKind.GOAL_PROGRESS, percent = crossed)
        }
        return null
    }
}
