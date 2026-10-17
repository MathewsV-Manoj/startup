package com.focuno.core.model

/**
 * Personalized thresholds learned over time. V1 keeps it simple and serializable
 * so it can live in DataStore.
 */
data class UserFocusProfile(
    val onboardingCompleted: Boolean = false,
    val dailyFocusGoalMinutes: Int = 120,
    val nudgesEnabled: Boolean = true,
    /** Minutes of uninterrupted time before a session counts as "deep work". */
    val deepWorkThresholdMinutes: Int = 25,
    /** Rolling window used by the consistency component of the score. */
    val consistencyWindowDays: Int = 7,
)
