package com.startup.focuno.data.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.startup.focuno.domain.model.AppCategory
import com.startup.focuno.domain.model.BlockScope
import com.startup.focuno.domain.model.BypassOutcome

/** Dates are ISO local dates ("2026-10-04") so they sort and range-query as plain text. */
@Entity(tableName = "daily_usage", primaryKeys = ["date", "packageName"], indices = [Index("date")])
data class DailyUsageEntity(
    val date: String,
    val packageName: String,
    val foregroundMs: Long,
    val openCount: Int,
    val isDistracting: Boolean,
)

@Entity(tableName = "daily_summary")
data class DailySummaryEntity(
    @PrimaryKey val date: String,
    val totalMs: Long,
    val distractingMs: Long,
    val productiveMs: Long,
    val focusScore: Int?,
    val longestFocusStreakMs: Long,
    val pickupCount: Int,
    val distractingOpenCount: Int,
)

@Entity(tableName = "hourly_distracting", primaryKeys = ["date", "hour"])
data class HourlyDistractingEntity(
    val date: String,
    val hour: Int,
    val distractingMs: Long,
)

@Entity(tableName = "block_schedule", indices = [Index("packageName")])
data class BlockScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val scope: BlockScope,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val daysOfWeekMask: Int,
    val enabled: Boolean,
    val strictMode: Boolean,
)

@Entity(tableName = "bypass_event", indices = [Index("timestamp")])
data class BypassEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val outcome: BypassOutcome,
    val reasonText: String?,
)

@Entity(tableName = "block_hit", indices = [Index("timestamp")])
data class BlockHitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
)

/** endTs starts as the planned end and becomes the real end if the session is stopped early. */
@Entity(tableName = "focus_session")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTs: Long,
    val endTs: Long,
    val plannedMs: Long,
    val completed: Boolean,
    val interruptions: Int,
    /** What was studied, e.g. "Signals". Empty when no subject was picked. */
    val subject: String = "",
)

@Entity(tableName = "app_category")
data class AppCategoryEntity(
    @PrimaryKey val packageName: String,
    val category: AppCategory,
    val userOverridden: Boolean,
)

/** One daily time limit per app, so the package name is the key. */
@Entity(tableName = "app_limit")
data class AppLimitEntity(
    @PrimaryKey val packageName: String,
    val dailyMinutes: Int,
    val strict: Boolean,
    val enabled: Boolean,
    val maxOpens: Int = 0,
)
