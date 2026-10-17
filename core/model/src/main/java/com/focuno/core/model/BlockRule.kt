package com.focuno.core.model

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Opt-in restriction on an app during a schedule window.
 *
 * @param packageName target app
 * @param enabled whether the rule is currently active
 * @param startTime window start (inclusive)
 * @param endTime window end (exclusive); if before [startTime] the window crosses midnight
 * @param days days of week the rule applies to
 */
data class BlockRule(
    val packageName: String,
    val enabled: Boolean = true,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
) {
    fun isActiveAt(time: LocalTime, day: DayOfWeek): Boolean {
        if (!enabled || day !in days) return false
        return if (endTime > startTime) {
            time >= startTime && time < endTime
        } else {
            time >= startTime || time < endTime
        }
    }

    fun durationMinutes(): Long {
        val minutes = if (endTime > startTime) {
            ChronoUnit.MINUTES.between(startTime, endTime)
        } else {
            ChronoUnit.MINUTES.between(startTime, LocalTime.MAX) + ChronoUnit.MINUTES.between(LocalTime.MIN, endTime)
        }
        return minutes
    }
}
