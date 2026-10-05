package com.startup.focuno.domain.usecase

import com.startup.focuno.domain.model.RawUsageEvent
import com.startup.focuno.domain.model.UsageEventType
import com.startup.focuno.domain.model.UsageSession
import com.startup.focuno.domain.model.UsageWalkResult

/**
 * Turns a chronological stream of usage events into per-app foreground time.
 *
 * Rules that exist because of real bugs:
 *  - Sessions are tracked per package as a SET of resumed activities, so moving between two screens of
 *    one app neither loses time nor counts as a new "open".
 *  - SCREEN_NON_INTERACTIVE closes every open session, so a phone left locked with an app in front never
 *    keeps counting.
 *  - A RESUMED event that arrives while the screen is off is ignored.
 *  - Only the part of a session inside [windowStartMs, windowEndMs] is counted, so events fetched from
 *    before the window (to catch an app already open at midnight) never leak into the total.
 *  - No app can exceed the window length. If it would, it is clamped and reported.
 */
object UsageEventWalker {

    /** A re-open this soon after a close is the same visit (activity-to-activity transition), not a new open. */
    private const val SAME_VISIT_GAP_MS = 1_500L

    fun walk(events: List<RawUsageEvent>, windowStartMs: Long, windowEndMs: Long): UsageWalkResult {
        val windowMs = windowEndMs - windowStartMs
        val resumedActivities = HashMap<String, MutableSet<String>>()
        val sessionStart = HashMap<String, Long>()
        val lastClose = HashMap<String, Long>()
        val foregroundMs = HashMap<String, Long>()
        val openCounts = HashMap<String, Int>()
        val sessions = ArrayList<UsageSession>()
        var screenOn = true
        var pickups = 0

        fun closeSession(pkg: String, atMs: Long) {
            val start = sessionStart.remove(pkg) ?: return
            lastClose[pkg] = atMs
            val from = maxOf(start, windowStartMs)
            val to = minOf(atMs, windowEndMs)
            if (to > from) {
                foregroundMs[pkg] = (foregroundMs[pkg] ?: 0L) + (to - from)
                sessions += UsageSession(pkg, from, to)
            }
        }

        for (event in events.sortedBy { it.timestampMs }) {
            if (event.timestampMs > windowEndMs) break
            val ts = event.timestampMs
            when (event.type) {
                UsageEventType.ACTIVITY_RESUMED -> {
                    if (!screenOn) continue
                    val pkg = event.packageName ?: continue
                    val activities = resumedActivities.getOrPut(pkg) { mutableSetOf() }
                    val wasIdle = activities.isEmpty()
                    activities += event.className.orEmpty()
                    if (wasIdle) {
                        sessionStart[pkg] = ts
                        val continuingVisit = lastClose[pkg]?.let { ts - it <= SAME_VISIT_GAP_MS } ?: false
                        if (ts >= windowStartMs && !continuingVisit) {
                            openCounts[pkg] = (openCounts[pkg] ?: 0) + 1
                        }
                    }
                }

                UsageEventType.ACTIVITY_PAUSED, UsageEventType.ACTIVITY_STOPPED -> {
                    val pkg = event.packageName ?: continue
                    val activities = resumedActivities[pkg] ?: continue
                    val cls = event.className
                    if (cls.isNullOrEmpty()) activities.clear() else activities.remove(cls)
                    if (activities.isEmpty()) {
                        resumedActivities.remove(pkg)
                        closeSession(pkg, ts)
                    }
                }

                UsageEventType.SCREEN_NON_INTERACTIVE -> {
                    screenOn = false
                    for (pkg in sessionStart.keys.toList()) closeSession(pkg, ts)
                    resumedActivities.clear()
                }

                UsageEventType.SCREEN_INTERACTIVE -> {
                    screenOn = true
                    if (ts in windowStartMs..windowEndMs) pickups++
                }
            }
        }

        if (screenOn) {
            for (pkg in sessionStart.keys.toList()) closeSession(pkg, windowEndMs)
        }

        val clamped = HashSet<String>()
        val finalMs = HashMap<String, Long>()
        for ((pkg, ms) in foregroundMs) {
            if (ms > windowMs) {
                clamped += pkg
                finalMs[pkg] = windowMs
            } else {
                finalMs[pkg] = ms
            }
        }

        return UsageWalkResult(
            foregroundMsByPackage = finalMs,
            openCountByPackage = openCounts,
            sessions = sessions.sortedBy { it.startMs },
            pickupCount = pickups,
            clampedPackages = clamped,
        )
    }
}
