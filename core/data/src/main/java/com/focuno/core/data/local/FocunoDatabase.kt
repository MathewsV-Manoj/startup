package com.focuno.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AppUsageEntity::class, FocusScoreEntity::class, BlockRuleEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FocunoDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
    abstract fun focusScoreDao(): FocusScoreDao
    abstract fun blockRuleDao(): BlockRuleDao

    companion object {
        const val NAME = "focuno.db"
    }
}
