package com.startup.focuno.domain.model

enum class FocusMode { TIMER, POMODORO, STOPWATCH }

/** Where a running plan is right now. [round] is 1-based. */
data class FocusPhase(
    val focusing: Boolean,
    val round: Int,
    val rounds: Int,
    val startsAtMs: Long,
    val endsAtMs: Long,
)

/**
 * A focus timer that is running, worked out from a few stored numbers so it survives restarts.
 *
 * - TIMER and STOPWATCH: one focus stretch from [startMs] to [endMs] (a stopwatch's end is only a safety cap).
 * - POMODORO: [rounds] focus rounds of [roundMs], with a [breakMs] break between them; apps are free during
 *   breaks.
 */
data class FocusPlan(
    val mode: FocusMode,
    val startMs: Long,
    val endMs: Long,
    val roundMs: Long = endMs - startMs,
    val breakMs: Long = 0L,
) {
    val rounds: Int
        get() = if (mode != FocusMode.POMODORO) 1 else (((endMs - startMs) + breakMs) / (roundMs + breakMs)).toInt().coerceAtLeast(1)

    fun isRunning(nowMs: Long): Boolean = nowMs in startMs until endMs

    /** True when apps should be paused right now. */
    fun isFocusing(nowMs: Long): Boolean = phaseAt(nowMs)?.focusing == true

    fun phaseAt(nowMs: Long): FocusPhase? {
        if (!isRunning(nowMs)) return null
        if (mode != FocusMode.POMODORO || breakMs <= 0L) return FocusPhase(true, 1, 1, startMs, endMs)
        val cycle = roundMs + breakMs
        val index = ((nowMs - startMs) / cycle).toInt()
        val cycleStart = startMs + index * cycle
        val focusEnd = cycleStart + roundMs
        return if (nowMs < focusEnd) {
            FocusPhase(true, index + 1, rounds, cycleStart, focusEnd)
        } else {
            FocusPhase(false, index + 1, rounds, focusEnd, minOf(focusEnd + breakMs, endMs))
        }
    }

    companion object {
        /** The classic Pomodoro: 25 minutes of focus, then a 5-minute break. */
        const val POMODORO_FOCUS_MIN = 25
        const val POMODORO_BREAK_MIN = 5

        fun pomodoro(startMs: Long, roundMinutes: Int, breakMinutes: Int, rounds: Int): FocusPlan {
            val roundMs = roundMinutes * 60_000L
            val breakMs = breakMinutes * 60_000L
            return FocusPlan(FocusMode.POMODORO, startMs, startMs + rounds * roundMs + (rounds - 1) * breakMs, roundMs, breakMs)
        }
    }
}
