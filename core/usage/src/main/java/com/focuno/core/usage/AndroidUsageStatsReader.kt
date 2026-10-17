package com.focuno.core.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidUsageStatsReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : UsageStatsReader {

    private val usageStatsManager: UsageStatsManager
        get() = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    override fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun usageForDay(epochDayStartMs: Long, epochDayEndMs: Long): List<PlatformUsage> {
        if (!hasUsageAccess()) return emptyList()

        val totals = usageStatsManager
            .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, epochDayStartMs, epochDayEndMs)
            .associate { it.packageName to it.totalTimeInForeground }

        val launches = countLaunches(epochDayStartMs, epochDayEndMs)

        return totals.entries
            .map { (pkg, totalMs) ->
                PlatformUsage(
                    packageName = pkg,
                    totalTimeInForegroundMs = totalMs,
                    launchCount = launches[pkg] ?: 0,
                )
            }
            .filter { it.totalTimeInForegroundMs > 0 }
            .sortedByDescending { it.totalTimeInForegroundMs }
    }

    private fun countLaunches(startMs: Long, endMs: Long): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        val events = usageStatsManager.queryEvents(startMs, endMs)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION")
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                counts[event.packageName] = (counts[event.packageName] ?: 0) + 1
            }
        }
        return counts
    }
}
