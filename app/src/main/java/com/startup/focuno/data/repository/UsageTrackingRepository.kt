package com.startup.focuno.data.repository

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import com.startup.focuno.data.local.homePackages
import com.startup.focuno.domain.model.AppUsageRaw
import com.startup.focuno.domain.model.DayUsage
import com.startup.focuno.domain.model.RawUsageEvent
import com.startup.focuno.domain.model.UsageEventType
import com.startup.focuno.domain.usecase.UsageEventWalker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Screen time comes ONLY from UsageStatsManager.queryEvents. queryUsageStats is never used: it returns
 * overlapping buckets per package and summing them double-counts (the "17 hours on a 2 hour day" bug).
 */
@Singleton
class UsageTrackingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val usageStatsManager: UsageStatsManager = context.getSystemService(UsageStatsManager::class.java)

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val uid = Process.myUid()
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
        } else {
            legacyCheckOp(appOps, uid)
        }
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    @Suppress("DEPRECATION")
    private fun legacyCheckOp(appOps: AppOpsManager, uid: Int): Int =
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)

    suspend fun getUsageForDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): DayUsage =
        withContext(Dispatchers.IO) {
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val windowEnd = minOf(System.currentTimeMillis(), dayEnd)
            if (!hasUsageAccess() || windowEnd <= dayStart) {
                return@withContext DayUsage(date, emptyList(), emptyList(), 0)
            }

            val events = readEvents(dayStart - LOOKBACK_MS, windowEnd)
            val walk = UsageEventWalker.walk(events, dayStart, windowEnd)
            walk.clampedPackages.forEach { pkg ->
                Log.w(TAG, "Usage for $pkg exceeded the elapsed day and was clamped. The event walk has a bug.")
            }

            val excluded = excludedPackages()
            fun raw(entry: Map.Entry<String, Long>) = AppUsageRaw(entry.key, entry.value, walk.openCountByPackage[entry.key] ?: 0)
            val apps = walk.foregroundMsByPackage.filterKeys { it !in excluded }.entries
                .map(::raw)
                .filter { it.foregroundMs > 0 }
                .sortedByDescending { it.foregroundMs }
            val excludedApps = walk.foregroundMsByPackage.filterKeys { it in excluded }.entries
                .map(::raw)
                .filter { it.foregroundMs > 0 }
                .sortedByDescending { it.foregroundMs }

            DayUsage(
                date = date,
                apps = apps,
                sessions = walk.sessions.filter { it.packageName !in excluded },
                pickupCount = walk.pickupCount,
                excludedApps = excludedApps,
            )
        }

    /** Today's foreground time for [packages], counted exactly like the screen-time numbers. */
    suspend fun foregroundMsToday(packages: Set<String>): Map<String, Long> {
        if (packages.isEmpty()) return emptyMap()
        return getUsageForDay(LocalDate.now()).apps
            .filter { it.packageName in packages }
            .associate { it.packageName to it.foregroundMs }
    }

    /** The app on screen right now, from recent events, used to seed the blocker after a restart. */
    suspend fun currentForegroundPackage(): String? = withContext(Dispatchers.IO) {
        if (!hasUsageAccess()) return@withContext null
        val now = System.currentTimeMillis()
        val windowStart = now - RECENT_MS
        val walk = UsageEventWalker.walk(readEvents(windowStart, now), windowStart, now)
        val excluded = excludedPackages()
        walk.sessions
            .filter { it.endMs >= now - 1_000L && it.packageName !in excluded }
            .maxByOrNull { it.startMs }
            ?.packageName
    }

    /** Raw events in a short window, for following the foreground app without the accessibility service. */
    suspend fun recentEvents(fromMs: Long, toMs: Long): List<RawUsageEvent> = withContext(Dispatchers.IO) {
        if (hasUsageAccess()) readEvents(fromMs, toMs) else emptyList()
    }

    private fun readEvents(fromMs: Long, toMs: Long): List<RawUsageEvent> {
        val usageEvents = usageStatsManager.queryEvents(fromMs, toMs)
        val event = UsageEvents.Event()
        val result = ArrayList<RawUsageEvent>()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val type = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventType.ACTIVITY_RESUMED
                UsageEvents.Event.ACTIVITY_PAUSED -> UsageEventType.ACTIVITY_PAUSED
                UsageEvents.Event.ACTIVITY_STOPPED -> UsageEventType.ACTIVITY_STOPPED
                UsageEvents.Event.SCREEN_INTERACTIVE -> UsageEventType.SCREEN_INTERACTIVE
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventType.SCREEN_NON_INTERACTIVE
                else -> continue
            }
            result += RawUsageEvent(event.timeStamp, type, event.packageName, event.className)
        }
        return result
    }

    /**
     * Only the home screen and system UI are left out. Android's own screen time counts every other
     * app, including apps with no launcher icon (the in-call screen) and Focuno itself, so Focuno does too.
     */
    private fun excludedPackages(): Set<String> = context.packageManager.homePackages() + ALWAYS_EXCLUDED

    private companion object {
        const val TAG = "UsageTracking"
        const val LOOKBACK_MS = 3 * 60 * 60_000L
        const val RECENT_MS = 15 * 60_000L
        val ALWAYS_EXCLUDED = setOf("com.android.systemui", "android")
    }
}
