package com.startup.focuno.domain.usecase

import com.startup.focuno.domain.model.ActiveBlockWindow
import com.startup.focuno.domain.model.BlockSchedule
import java.time.ZonedDateTime

object ScheduleEvaluator {

    private const val MINUTES_PER_DAY = 24 * 60

    /**
     * Returns the block window that is open right now for [packageName], or null.
     *
     * A window belongs to the day it STARTS on, and may end the next morning (22:00-06:00).
     * So both today's windows and yesterday's windows are checked. start == end means a full 24 hours.
     * If several windows overlap, the latest end wins and strict mode applies if any of them is strict.
     */
    fun activeWindow(schedules: List<BlockSchedule>, packageName: String, now: ZonedDateTime): ActiveBlockWindow? {
        var latestEnd: ZonedDateTime? = null
        var best: BlockSchedule? = null
        var anyStrict = false

        for (schedule in schedules) {
            if (!schedule.enabled || schedule.packageName != packageName) continue
            val rawDuration = (schedule.endMinuteOfDay - schedule.startMinuteOfDay + MINUTES_PER_DAY) % MINUTES_PER_DAY
            val durationMinutes = if (rawDuration == 0) MINUTES_PER_DAY else rawDuration

            for (dayOffset in 0L downTo -1L) {
                val startDate = now.toLocalDate().plusDays(dayOffset)
                if (!schedule.isDayEnabled(startDate.dayOfWeek.value - 1)) continue
                val windowStart = startDate
                    .atTime(schedule.startMinuteOfDay / 60, schedule.startMinuteOfDay % 60)
                    .atZone(now.zone)
                val windowEnd = windowStart.plusMinutes(durationMinutes.toLong())
                if (!now.isBefore(windowStart) && now.isBefore(windowEnd)) {
                    if (schedule.strictMode) anyStrict = true
                    if (latestEnd == null || windowEnd.isAfter(latestEnd)) {
                        latestEnd = windowEnd
                        best = schedule
                    }
                }
            }
        }

        val end = latestEnd ?: return null
        val chosen = best ?: return null
        return ActiveBlockWindow(
            endsAtMs = end.toInstant().toEpochMilli(),
            startMinuteOfDay = chosen.startMinuteOfDay,
            endMinuteOfDay = chosen.endMinuteOfDay,
            strict = anyStrict,
        )
    }
}
