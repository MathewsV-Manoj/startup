package com.focuno.core.usage

/** Raw per-app usage straight from the platform, before categorization. */
data class PlatformUsage(
    val packageName: String,
    val totalTimeInForegroundMs: Long,
    val launchCount: Int,
)

/**
 * Thin, testable seam over `android.app.usage.UsageStatsManager`. The data layer
 * depends on this interface, never on the platform class directly.
 */
interface UsageStatsReader {
    fun hasUsageAccess(): Boolean
    fun usageForDay(epochDayStartMs: Long, epochDayEndMs: Long): List<PlatformUsage>
}
