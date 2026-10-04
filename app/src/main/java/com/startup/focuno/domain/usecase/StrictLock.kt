package com.startup.focuno.domain.usecase

import com.startup.focuno.domain.model.BlockSchedule
import java.time.ZonedDateTime

/**
 * Strict mode "cannot be undone until the window ends". If it could be edited or switched off mid-window it
 * would be no stricter than a normal schedule, so editing is locked while a strict window is open.
 */
object StrictLock {
    /** End of the open strict window in epoch ms, or null if the schedule may be changed now. */
    fun lockedUntilMs(schedule: BlockSchedule, now: ZonedDateTime = ZonedDateTime.now()): Long? {
        if (!schedule.strictMode || !schedule.enabled || schedule.id == 0L) return null
        return ScheduleEvaluator.activeWindow(listOf(schedule), schedule.packageName, now, schedule.scope)?.endsAtMs
    }
}
