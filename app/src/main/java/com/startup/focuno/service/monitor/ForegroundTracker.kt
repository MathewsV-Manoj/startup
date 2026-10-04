package com.startup.focuno.service.monitor

import com.startup.focuno.domain.model.RawUsageEvent
import com.startup.focuno.domain.model.UsageEventType

/**
 * Works out which app is in front from a stream of usage events, WITHOUT the accessibility service.
 *
 * The answer is held as explicit state ([current]) and only changes when an event says so, so sitting still in
 * one app never makes it "disappear". Feeding it overlapping event windows is safe: replaying an event that was
 * already applied leaves the same final state.
 */
class ForegroundTracker {

    private val activities = HashMap<String, MutableSet<String>>()
    private val recency = LinkedHashSet<String>()

    var current: String? = null
        private set

    /** Start from a known foreground app (found once from recent history). */
    fun reset(packageName: String?) {
        activities.clear()
        recency.clear()
        current = packageName
        if (packageName != null) {
            activities[packageName] = mutableSetOf(SEEDED)
            recency.add(packageName)
        }
    }

    /** Applies events in time order. Returns true if the foreground app changed. */
    fun apply(events: List<RawUsageEvent>): Boolean {
        val before = current
        for (event in events.sortedBy { it.timestampMs }) {
            when (event.type) {
                UsageEventType.ACTIVITY_RESUMED -> {
                    val pkg = event.packageName ?: continue
                    activities.getOrPut(pkg) { mutableSetOf() } += event.className.orEmpty()
                    recency.remove(pkg)
                    recency.add(pkg)
                    current = pkg
                }

                UsageEventType.ACTIVITY_PAUSED, UsageEventType.ACTIVITY_STOPPED -> {
                    val pkg = event.packageName ?: continue
                    val open = activities[pkg] ?: continue
                    val cls = event.className
                    if (cls.isNullOrEmpty()) open.clear() else open.remove(cls)
                    open.remove(SEEDED)
                    if (open.isEmpty()) {
                        activities.remove(pkg)
                        recency.remove(pkg)
                        if (current == pkg) current = recency.lastOrNull()
                    }
                }

                UsageEventType.SCREEN_NON_INTERACTIVE -> {
                    activities.clear()
                    recency.clear()
                    current = null
                }

                UsageEventType.SCREEN_INTERACTIVE -> Unit
            }
        }
        return current != before
    }

    private companion object {
        const val SEEDED = "<seeded>"
    }
}
