package com.focuno.core.data.repository

import com.focuno.core.common.DispatcherProvider
import com.focuno.core.data.local.AppUsageEntity
import com.focuno.core.data.local.UsageDao
import com.focuno.core.data.mapper.toDomain
import com.focuno.core.datastore.FocunoPreferences
import com.focuno.core.domain.repository.UsageRepository
import com.focuno.core.model.AppCategory
import com.focuno.core.model.AppUsage
import com.focuno.core.usage.UsageStatsReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultUsageRepository @Inject constructor(
    private val reader: UsageStatsReader,
    private val usageDao: UsageDao,
    private val preferences: FocunoPreferences,
    private val labelResolver: AppLabelResolver,
    private val categorizer: AppCategorizer,
    private val dispatchers: DispatcherProvider,
) : UsageRepository {

    override suspend fun hasUsageAccess(): Boolean = reader.hasUsageAccess()

    override suspend fun usageFor(date: LocalDate): List<AppUsage> = withContext(dispatchers.io) {
        var rows = usageDao.getForDay(date.toEpochDay())
        if (rows.isEmpty() && reader.hasUsageAccess()) {
            refresh(date)
            rows = usageDao.getForDay(date.toEpochDay())
        }
        applyOverrides(rows)
    }

    override suspend fun usageFor(from: LocalDate, to: LocalDate): Map<LocalDate, List<AppUsage>> =
        withContext(dispatchers.io) {
            val overrides = preferences.categoryOverrides.first()
            usageDao.getForRange(from.toEpochDay(), to.toEpochDay())
                .map { entity ->
                    entity.toDomain().let { domain ->
                        overrides[domain.packageName]?.let { domain.copy(category = it) } ?: domain
                    }
                }
                .groupBy { it.date }
        }

    override fun observeUsage(date: LocalDate): Flow<List<AppUsage>> = combine(
        usageDao.observeForDay(date.toEpochDay()),
        preferences.categoryOverrides,
    ) { rows, overrides ->
        rows.map { entity ->
            entity.toDomain().let { domain ->
                overrides[domain.packageName]?.let { domain.copy(category = it) } ?: domain
            }
        }
    }

    override suspend fun setCategoryOverride(packageName: String, category: AppCategory) {
        preferences.setCategoryOverride(packageName, category)
    }

    /** Pulls fresh usage from the platform into Room. Called by the aggregation worker. */
    suspend fun refresh(date: LocalDate) = withContext(dispatchers.io) {
        if (!reader.hasUsageAccess()) return@withContext
        val zone = ZoneId.systemDefault()
        val startMs = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val overrides = preferences.categoryOverrides.first()

        val rows = reader.usageForDay(startMs, endMs).map { usage ->
            AppUsageEntity(
                packageName = usage.packageName,
                epochDay = date.toEpochDay(),
                appLabel = labelResolver.labelFor(usage.packageName),
                totalMillis = usage.totalTimeInForegroundMs,
                launchCount = usage.launchCount,
                category = categorizer.categorize(usage.packageName, overrides).name,
            )
        }
        usageDao.upsertAll(rows)
    }

    private suspend fun applyOverrides(rows: List<AppUsageEntity>): List<AppUsage> {
        val overrides = preferences.categoryOverrides.first()
        return rows.map { entity ->
            val domain = entity.toDomain()
            overrides[domain.packageName]?.let { domain.copy(category = it) } ?: domain
        }
    }
}
