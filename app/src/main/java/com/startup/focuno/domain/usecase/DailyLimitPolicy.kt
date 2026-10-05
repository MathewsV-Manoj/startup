package com.startup.focuno.domain.usecase

import com.startup.focuno.domain.model.AppLimit
import java.time.ZonedDateTime

/**
 * Rules for daily time limits. Today's use is measured from usage events now and then; between
 * measurements, time keeps counting while the app is in front, so the limit hits on time.
 */
object DailyLimitPolicy {
    /** How close to the limit the one-time heads-up appears. */
    const val WARN_BEFORE_MS = 5 * 60_000L

    /**
     * Use so far today. A measurement from before midnight belongs to yesterday and counts as nothing.
     */
    fun usedNowMs(measuredMs: Long, measuredAtMs: Long, nowMs: Long, inFront: Boolean, dayStartMs: Long): Long {
        if (measuredAtMs < dayStartMs) return if (inFront) (nowMs - dayStartMs).coerceAtLeast(0) else 0L
        return measuredMs + if (inFront) (nowMs - measuredAtMs).coerceAtLeast(0) else 0L
    }

    fun isReached(limit: AppLimit, usedMs: Long): Boolean = limit.enabled && limit.dailyMinutes > 0 && usedMs >= limit.dailyMs

    /** "At most 5 opens" lets the fifth open run and pauses the sixth. */
    fun opensReached(limit: AppLimit, opensToday: Int): Boolean = limit.enabled && limit.maxOpens > 0 && opensToday > limit.maxOpens

    fun remainingMs(limit: AppLimit, usedMs: Long): Long = (limit.dailyMs - usedMs).coerceAtLeast(0)

    /** Still under the limit, but within the last few minutes of it. */
    fun shouldWarn(limit: AppLimit, usedMs: Long): Boolean {
        val remaining = remainingMs(limit, usedMs)
        return limit.enabled && limit.dailyMinutes > 0 && remaining in 1..WARN_BEFORE_MS
    }

    fun nextMidnightMs(now: ZonedDateTime): Long =
        now.toLocalDate().plusDays(1).atStartOfDay(now.zone).toInstant().toEpochMilli()

    /** A strict limit that is used up (time or opens) cannot be raised, paused or deleted until midnight. */
    fun lockedUntilMs(limit: AppLimit, usedMs: Long, now: ZonedDateTime, opensToday: Int = 0): Long? =
        if (limit.strict && (isReached(limit, usedMs) || opensReached(limit, opensToday))) nextMidnightMs(now) else null
}
