package com.focuno.core.domain.repository

import com.focuno.core.model.AppUsage
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Read access to the device's app usage. Implemented in `:core:data` on top of
 * `UsageStatsManager` (`:core:usage`).
 */
interface UsageRepository {
    /** Whether the special `PACKAGE_USAGE_STATS` access has been granted. */
    suspend fun hasUsageAccess(): Boolean

    /** Usage rows for a single day. */
    suspend fun usageFor(date: LocalDate): List<AppUsage>

    /** Usage rows for an inclusive date range, keyed by day. */
    suspend fun usageFor(from: LocalDate, to: LocalDate): Map<LocalDate, List<AppUsage>>

    /** Reactive view of a single day's usage for the UI. */
    fun observeUsage(date: LocalDate): Flow<List<AppUsage>>

    /** Persist a user override of the category for an app. */
    suspend fun setCategoryOverride(packageName: String, category: com.focuno.core.model.AppCategory)
}
