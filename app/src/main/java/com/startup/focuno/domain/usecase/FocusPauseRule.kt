package com.startup.focuno.domain.usecase

/** Which apps a running focus round pauses. */
object FocusPauseRule {
    /**
     * Time-eaters are always paused. In Lock mode every other app is paused too, except the ones the person
     * allowed and the phone's essentials (calls, Settings, permission screens), which must keep working.
     */
    fun pauses(
        packageName: String,
        isTimeEater: Boolean,
        lockAll: Boolean,
        allowed: Set<String>,
        essentials: Set<String>,
    ): Boolean = isTimeEater || (lockAll && packageName !in allowed && packageName !in essentials)
}
