package com.startup.focuno.domain.usecase

import com.startup.focuno.domain.model.BlockSchedule
import java.time.ZonedDateTime

/**
 * Strict mode "cannot be undone until the window ends". If it could be edited or switched off mid-window it
 * would be no stricter than a normal schedule, so editing is locked while a strict window is open.
 */
object StrictLock {
    /**
     * A block that runs all day on every day never has a gap in which it could be edited, so making it strict
     * would lock it for good. Strict needs at least one break.
     */
    fun allowsStrict(startMinuteOfDay: Int, endMinuteOfDay: Int, daysOfWeekMask: Int): Boolean =
        !(startMinuteOfDay == endMinuteOfDay && daysOfWeekMask == BlockSchedule.ALL_DAYS)

    /** End of the open strict window in epoch ms, or null if the schedule may be changed now. */
    fun lockedUntilMs(schedule: BlockSchedule, now: ZonedDateTime = ZonedDateTime.now()): Long? {
        if (!schedule.strictMode || !schedule.enabled || schedule.id == 0L) return null
        // Older versions allowed this combination. Never lock it, or the person could not remove it again.
        if (!allowsStrict(schedule.startMinuteOfDay, schedule.endMinuteOfDay, schedule.daysOfWeekMask)) return null
        return ScheduleEvaluator.activeWindow(listOf(schedule), schedule.packageName, now, schedule.scope)?.endsAtMs
    }
}
