package com.startup.focuno.data.repository

import androidx.room.withTransaction
import com.startup.focuno.data.room.DailySummaryDao
import com.startup.focuno.data.room.DailySummaryEntity
import com.startup.focuno.data.room.DailyUsageDao
import com.startup.focuno.data.room.DailyUsageEntity
import com.startup.focuno.data.room.FocunoDatabase
import com.startup.focuno.data.room.HourTotal
import com.startup.focuno.data.room.HourlyDistractingDao
import com.startup.focuno.data.room.HourlyDistractingEntity
import com.startup.focuno.domain.model.AppCategory
import com.startup.focuno.domain.model.DayStats
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryRepository @Inject constructor(
    private val database: FocunoDatabase,
    private val usageDao: DailyUsageDao,
    private val summaryDao: DailySummaryDao,
    private val hourlyDao: HourlyDistractingDao,
) {
    fun observeSummaries(from: LocalDate, to: LocalDate): Flow<List<DailySummaryEntity>> =
        summaryDao.observeRange(from.toString(), to.toString())

    suspend fun summaries(from: LocalDate, to: LocalDate): List<DailySummaryEntity> =
        summaryDao.range(from.toString(), to.toString())

    suspend fun existingDates(from: LocalDate, to: LocalDate): Set<String> =
        summaryDao.existingDates(from.toString(), to.toString()).toSet()

    fun observeHourlyTotals(from: LocalDate, to: LocalDate): Flow<List<HourTotal>> =
        hourlyDao.observeTotalsByHour(from.toString(), to.toString())

    /** Writes a finished day as one atomic replace, so a crash can never leave half a day behind. */
    suspend fun saveDay(stats: DayStats) {
        val date = stats.date.toString()
        database.withTransaction {
            usageDao.deleteForDate(date)
            usageDao.upsertAll(
                stats.apps.map {
                    DailyUsageEntity(date, it.packageName, it.foregroundMs, it.openCount, it.category == AppCategory.DISTRACTING)
                },
            )
            summaryDao.upsert(
                DailySummaryEntity(
                    date = date,
                    totalMs = stats.totalMs,
                    distractingMs = stats.distractingMs,
                    productiveMs = stats.productiveMs,
                    focusScore = stats.focusScore,
                    longestFocusStreakMs = stats.longestFocusStreakMs,
                    pickupCount = stats.pickupCount,
                    distractingOpenCount = stats.distractingOpenCount,
                ),
            )
            hourlyDao.deleteForDate(date)
            hourlyDao.upsertAll(
                stats.hourlyDistractingMs.mapIndexedNotNull { hour, ms ->
                    if (ms > 0) HourlyDistractingEntity(date, hour, ms) else null
                },
            )
        }
    }

    suspend fun pruneBefore(cutoff: LocalDate) {
        val date = cutoff.toString()
        database.withTransaction {
            usageDao.deleteOlderThan(date)
            summaryDao.deleteOlderThan(date)
            hourlyDao.deleteOlderThan(date)
        }
    }
}
