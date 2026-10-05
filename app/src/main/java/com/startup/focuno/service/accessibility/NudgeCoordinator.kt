package com.startup.focuno.service.accessibility

import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.model.AppSettings
import com.startup.focuno.data.repository.FocusSessionRepository
import com.startup.focuno.data.repository.InstalledAppsRepository
import com.startup.focuno.data.repository.UsageTrackingRepository
import com.startup.focuno.domain.usecase.ComputeDayStatsUseCase
import com.startup.focuno.domain.usecase.Nudge
import com.startup.focuno.domain.usecase.NudgeEngine
import com.startup.focuno.domain.usecase.NudgeInput
import com.startup.focuno.domain.usecase.NudgeKind
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the running facts the pure [NudgeEngine] needs: pickups per app, continuous use, today's total. */
@Singleton
class NudgeCoordinator @Inject constructor(
    private val settingsStore: SettingsStore,
    private val computeDayStats: ComputeDayStatsUseCase,
    private val usageRepository: UsageTrackingRepository,
    private val focusSessions: FocusSessionRepository,
    private val installedApps: InstalledAppsRepository,
) {
    private val opens = HashMap<String, ArrayDeque<Long>>()
    private var pendingOpenPackage: String? = null
    private var lastTickMs = 0L
    private var continuousMs = 0L
    private var cachedTotalMs = 0L
    private var cachedTotalAtMs = 0L

    fun reset() {
        opens.clear()
        pendingOpenPackage = null
        lastTickMs = 0L
        continuousMs = 0L
    }

    fun onForegroundChanged(packageName: String, nowMs: Long) {
        val times = opens.getOrPut(packageName) { ArrayDeque() }
        times.addLast(nowMs)
        while (times.isNotEmpty() && nowMs - times.first() > HOUR_MS) times.removeFirst()
        pendingOpenPackage = packageName
    }

    /** Returns a nudge to show now, already recorded as delivered, or null. */
    suspend fun check(packageName: String?, isDistracting: Boolean, nowMs: Long, settings: AppSettings): Nudge? {
        val elapsed = if (lastTickMs == 0L) 0L else minOf(nowMs - lastTickMs, MAX_TICK_GAP_MS)
        lastTickMs = nowMs
        continuousMs = if (isDistracting) continuousMs + elapsed else 0L

        if (!settings.nudgesEnabled) return null

        val justOpened = packageName != null && pendingOpenPackage == packageName
        val today = LocalDate.now()
        val input = NudgeInput(
            nowMs = nowMs,
            today = today,
            nudgesEnabled = settings.nudgesEnabled,
            sensitivity = settings.nudgeSensitivity,
            lastNudgeAtMs = settings.lastNudgeAtMs,
            nudgeDay = settings.nudgeDay,
            nudgeCountToday = settings.nudgeCountToday,
            goalThresholdsHit = settings.goalThresholdsHit,
            dailyGoalMinutes = settings.dailyGoalMinutes,
            totalTodayMs = totalTodayMs(nowMs),
            appName = packageName?.let { installedApps.label(it) },
            isDistractingForeground = isDistracting,
            justOpened = justOpened,
            opensOfAppLastHour = packageName?.let { opens[it]?.size } ?: 0,
            continuousDistractingMs = continuousMs,
            msSinceFocusSessionEnded = if (justOpened && isDistracting) {
                focusSessions.lastCompletedEndMs()?.let { nowMs - it }
            } else {
                null
            },
        )
        if (justOpened) pendingOpenPackage = null

        val nudge = NudgeEngine.evaluate(input) ?: return null
        recordDelivered(nudge, nowMs, today, settings)
        return nudge
    }

    private suspend fun recordDelivered(nudge: Nudge, nowMs: Long, today: LocalDate, settings: AppSettings) {
        val countToday = if (settings.nudgeDay == today.toString()) settings.nudgeCountToday else 0
        settingsStore.recordNudge(nowMs, today.toString(), countToday + 1)
        if (nudge.kind == NudgeKind.CONTINUOUS_USE) continuousMs = 0L
        if (nudge.kind == NudgeKind.GOAL_PROGRESS) {
            // Mark this and every lower threshold as announced so 50% never fires after 75%.
            NudgeEngine.GOAL_THRESHOLDS.filter { it <= nudge.percent }.forEach {
                settingsStore.addGoalThresholdHit(NudgeEngine.goalKey(today, it), today.toString())
            }
        }
    }

    private suspend fun totalTodayMs(nowMs: Long): Long {
        if (nowMs - cachedTotalAtMs > TOTAL_REFRESH_MS && usageRepository.hasUsageAccess()) {
            cachedTotalMs = computeDayStats(LocalDate.now()).totalMs
            cachedTotalAtMs = nowMs
        }
        return cachedTotalMs
    }

    private companion object {
        const val HOUR_MS = 60 * 60_000L
        const val MAX_TICK_GAP_MS = 30_000L
        const val TOTAL_REFRESH_MS = 2 * 60_000L
    }
}
