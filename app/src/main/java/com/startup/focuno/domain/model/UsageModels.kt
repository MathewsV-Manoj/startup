package com.startup.focuno.domain.model

import java.time.LocalDate

enum class UsageEventType {
    ACTIVITY_RESUMED,
    ACTIVITY_PAUSED,
    ACTIVITY_STOPPED,
    SCREEN_INTERACTIVE,
    SCREEN_NON_INTERACTIVE,
}

/** Platform-independent copy of a UsageEvents.Event so the walk can be unit-tested on the JVM. */
data class RawUsageEvent(
    val timestampMs: Long,
    val type: UsageEventType,
    val packageName: String?,
    val className: String?,
)

data class UsageSession(val packageName: String, val startMs: Long, val endMs: Long) {
    val durationMs: Long get() = endMs - startMs
}

data class UsageWalkResult(
    val foregroundMsByPackage: Map<String, Long>,
    val openCountByPackage: Map<String, Int>,
    val sessions: List<UsageSession>,
    val pickupCount: Int,
    val clampedPackages: Set<String>,
)

data class AppUsageRaw(val packageName: String, val foregroundMs: Long, val openCount: Int)

data class DayUsage(
    val date: LocalDate,
    val apps: List<AppUsageRaw>,
    val sessions: List<UsageSession>,
    val pickupCount: Int,
    /** Apps seen in the events but not counted (home screen, system UI), shown in the screen time check. */
    val excludedApps: List<AppUsageRaw> = emptyList(),
)

data class AppDayUsage(
    val packageName: String,
    val foregroundMs: Long,
    val openCount: Int,
    val category: AppCategory,
)

data class DayStats(
    val date: LocalDate,
    val totalMs: Long,
    val distractingMs: Long,
    val productiveMs: Long,
    val neutralMs: Long,
    val focusScore: Int?,
    val pickupCount: Int,
    val distractingOpenCount: Int,
    val longestFocusStreakMs: Long,
    val apps: List<AppDayUsage>,
    val hourlyDistractingMs: List<Long>,
    val excludedApps: List<AppUsageRaw> = emptyList(),
)
