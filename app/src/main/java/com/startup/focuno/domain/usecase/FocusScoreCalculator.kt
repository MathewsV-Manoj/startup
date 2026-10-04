package com.startup.focuno.domain.usecase

import com.startup.focuno.domain.model.UsageSession
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

object FocusScoreCalculator {

    private const val RATIO_WEIGHT = 45.0
    private const val VOLUME_WEIGHT = 25.0
    private const val FRAGMENT_WEIGHT = 15.0
    private const val VOLUME_CAP_MINUTES = 180.0
    private const val FRAGMENT_CAP_OPENS = 40.0

    /** Null when there is no data at all: a fresh install must not show a perfect score. */
    fun score(totalMs: Long, distractingMs: Long, distractingOpenCount: Int): Int? {
        if (totalMs <= 0L) return null
        val ratioPenalty = RATIO_WEIGHT * (distractingMs.toDouble() / maxOf(totalMs, 1L))
        val distractingMinutes = distractingMs / 60_000.0
        val volumePenalty = VOLUME_WEIGHT * minOf(1.0, distractingMinutes / VOLUME_CAP_MINUTES)
        val fragmentPenalty = FRAGMENT_WEIGHT * minOf(1.0, distractingOpenCount / FRAGMENT_CAP_OPENS)
        return (100.0 - ratioPenalty - volumePenalty - fragmentPenalty).roundToInt().coerceIn(0, 100)
    }
}

object FocusMetrics {

    private const val STREAK_BREAK_GAP_MS = 5 * 60_000L

    /**
     * Longest run of non-distracting phone use with no distracting app and no break longer than 5 minutes.
     * Sessions must be in chronological order.
     */
    fun longestFocusStreakMs(sessions: List<UsageSession>, isDistracting: (String) -> Boolean): Long {
        var best = 0L
        var run = 0L
        var lastEnd: Long? = null
        for (session in sessions) {
            if (isDistracting(session.packageName)) {
                run = 0L
                lastEnd = null
                continue
            }
            val previousEnd = lastEnd
            if (previousEnd != null && session.startMs - previousEnd > STREAK_BREAK_GAP_MS) run = 0L
            run += session.durationMs
            best = maxOf(best, run)
            lastEnd = session.endMs
        }
        return best
    }

    /** Splits sessions across local clock hours. Index 0..23. */
    fun hourlyMs(sessions: List<UsageSession>, zone: ZoneId): List<Long> {
        val hours = LongArray(24)
        for (session in sessions) {
            var cursor = session.startMs
            while (cursor < session.endMs) {
                val zoned = Instant.ofEpochMilli(cursor).atZone(zone)
                val nextHourMs = zoned.truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant().toEpochMilli()
                val segmentEnd = minOf(nextHourMs, session.endMs)
                hours[zoned.hour] += segmentEnd - cursor
                cursor = segmentEnd
            }
        }
        return hours.toList()
    }
}
