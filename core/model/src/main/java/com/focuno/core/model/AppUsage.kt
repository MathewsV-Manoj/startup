package com.focuno.core.model

import java.time.LocalDate

/**
 * Aggregated usage for a single app on a single day.
 *
 * @param packageName Android package id, e.g. `com.instagram.android`
 * @param appLabel human readable name, may be null if not resolved yet
 * @param date the day this usage belongs to (device local time zone)
 * @param totalTimeInForeground total foreground duration for the day
 * @param launchCount number of times the app was opened
 * @param category classification used by the focus score engine
 */
data class AppUsage(
    val packageName: String,
    val appLabel: String?,
    val date: LocalDate,
    val totalTimeInForeground: Duration,
    val launchCount: Int,
    val category: AppCategory,
)
