package com.startup.focuno.data.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyUsageEntity::class,
        DailySummaryEntity::class,
        HourlyDistractingEntity::class,
        BlockScheduleEntity::class,
        BypassEventEntity::class,
        BlockHitEntity::class,
        FocusSessionEntity::class,
        AppCategoryEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class FocunoDatabase : RoomDatabase() {
    abstract fun dailyUsageDao(): DailyUsageDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun hourlyDistractingDao(): HourlyDistractingDao
    abstract fun blockScheduleDao(): BlockScheduleDao
    abstract fun bypassEventDao(): BypassEventDao
    abstract fun blockHitDao(): BlockHitDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun appCategoryDao(): AppCategoryDao
}
