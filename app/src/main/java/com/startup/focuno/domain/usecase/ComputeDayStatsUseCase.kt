package com.startup.focuno.domain.usecase

import com.startup.focuno.data.repository.AppCategoryRepository
import com.startup.focuno.data.repository.UsageTrackingRepository
import com.startup.focuno.domain.model.AppCategory
import com.startup.focuno.domain.model.AppDayUsage
import com.startup.focuno.domain.model.DayStats
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class ComputeDayStatsUseCase @Inject constructor(
    private val usageRepository: UsageTrackingRepository,
    private val categoryRepository: AppCategoryRepository,
) {
    suspend operator fun invoke(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): DayStats {
        val usage = usageRepository.getUsageForDay(date, zone)
        val overrides = categoryRepository.overrides()
        val categoryOf = { pkg: String -> categoryRepository.resolve(pkg, overrides) }

        val apps = usage.apps
            .map { AppDayUsage(it.packageName, it.foregroundMs, it.openCount, categoryOf(it.packageName)) }
            .sortedByDescending { it.foregroundMs }

        val distractingApps = apps.filter { it.category == AppCategory.DISTRACTING }
        val totalMs = apps.sumOf { it.foregroundMs }
        val distractingMs = distractingApps.sumOf { it.foregroundMs }
        val productiveMs = apps.filter { it.category == AppCategory.PRODUCTIVE }.sumOf { it.foregroundMs }
        val distractingOpens = distractingApps.sumOf { it.openCount }
        val isDistracting = { pkg: String -> categoryOf(pkg) == AppCategory.DISTRACTING }

        return DayStats(
            date = date,
            totalMs = totalMs,
            distractingMs = distractingMs,
            productiveMs = productiveMs,
            neutralMs = totalMs - distractingMs - productiveMs,
            focusScore = FocusScoreCalculator.score(totalMs, distractingMs, distractingOpens),
            pickupCount = usage.pickupCount,
            distractingOpenCount = distractingOpens,
            longestFocusStreakMs = FocusMetrics.longestFocusStreakMs(usage.sessions, isDistracting),
            apps = apps,
            hourlyDistractingMs = FocusMetrics.hourlyMs(usage.sessions.filter { isDistracting(it.packageName) }, zone),
            excludedApps = usage.excludedApps,
        )
    }
}
