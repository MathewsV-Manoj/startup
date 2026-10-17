package com.focuno.core.model

/**
 * The single source of truth for the feature vector used by the distraction
 * classifier. Training (V2) and inference (V1 heuristics + later TFLite) both
 * consume this so the two can never drift.
 *
 * Order of [toVector] is part of the contract: append only, never reorder.
 */
data class DistractionFeatures(
    /** App switches per hour of screen-on time. */
    val switchRatePerHour: Float,
    /** Device unlocks during the observed window. */
    val unlockCount: Int,
    /** Average continuous session length in minutes. */
    val avgSessionMinutes: Float,
    /** Hour of day, 0..23. */
    val hourOfDay: Int,
    /** Total screen-on minutes in the window. */
    val screenOnMinutes: Float,
    /** Fraction of activity happening in the late-night window (22:00-05:00). */
    val lateNightRatio: Float,
) {
    fun toVector(): FloatArray = floatArrayOf(
        switchRatePerHour,
        unlockCount.toFloat(),
        avgSessionMinutes,
        hourOfDay.toFloat(),
        screenOnMinutes,
        lateNightRatio,
    )

    companion object {
        val FEATURE_NAMES = listOf(
            "switch_rate_per_hour",
            "unlock_count",
            "avg_session_minutes",
            "hour_of_day",
            "screen_on_minutes",
            "late_night_ratio",
        )

        val VECTOR_SIZE = FEATURE_NAMES.size
    }
}
