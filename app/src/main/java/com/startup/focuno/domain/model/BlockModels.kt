package com.startup.focuno.domain.model

/** daysOfWeekMask: bit 0 = Monday ... bit 6 = Sunday. The day is the one the window STARTS on. */
data class BlockSchedule(
    val id: Long = 0,
    val packageName: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val daysOfWeekMask: Int,
    val enabled: Boolean = true,
    val strictMode: Boolean = false,
) {
    fun isDayEnabled(dayIndexFromMonday: Int): Boolean = daysOfWeekMask and (1 shl dayIndexFromMonday) != 0

    companion object {
        const val ALL_DAYS = 0b1111111
    }
}

data class ActiveBlockWindow(
    val endsAtMs: Long,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val strict: Boolean,
)
