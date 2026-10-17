package com.focuno.core.domain.score

import com.focuno.core.model.AppCategory
import com.focuno.core.model.AppUsage
import com.focuno.core.model.FocusScore
import com.focuno.core.model.FocusSession
import com.focuno.core.model.UserFocusProfile
import java.time.LocalDate

/**
 * Pure, deterministic Focus Score engine. No Android, no I/O — trivially testable.
 *
 * See `docs/architecture.md` section 5 for the formula and rationale.
 */
object FocusScoreEngine {

    /** Switch rate at which the switch-stability component bottoms out. */
    const val SWITCH_CAP = 40f

    fun compute(
        date: LocalDate,
        usages: List<AppUsage>,
        sessions: List<FocusSession>,
        consistency: Float,
        profile: UserFocusProfile,
    ): FocusScore {
        val totalMinutes = usages.sumOf { it.totalTimeInForeground.toMinutes() }.coerceAtLeast(0L)

        val deepMinutes = if (sessions.isNotEmpty()) {
            sessions.sumOf { session ->
                val minutes = session.durationOrNull?.toMinutes() ?: 0L
                if (minutes >= profile.deepWorkThresholdMinutes) minutes else 0L
            }
        } else {
            usages.filter { it.category == AppCategory.PRODUCTIVE }
                .sumOf { it.totalTimeInForeground.toMinutes() }
        }

        val deepWorkRatio =
            if (totalMinutes == 0L) 0f else (deepMinutes.toFloat() / totalMinutes).coerceIn(0f, 1f)

        val screenHours = totalMinutes / 60f
        val launches = usages.sumOf { it.launchCount }
        val switchRate = if (screenHours <= 0f) 0f else launches / screenHours
        val switchStability = (1f - (switchRate / SWITCH_CAP)).coerceIn(0f, 1f)

        val distractionResistance = if (sessions.isEmpty()) {
            1f
        } else {
            1f - (sessions.count { it.interrupted }.toFloat() / sessions.size)
        }

        return FocusScore.from(
            date = date,
            deepWorkRatio = deepWorkRatio,
            switchStability = switchStability,
            distractionResistance = distractionResistance,
            consistency = consistency,
        )
    }
}
