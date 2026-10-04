package com.startup.focuno.data.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: schedules gain a scope (whole app, or only the Reels/Shorts feed).
 * The table is rebuilt instead of altered so it ends up exactly as Room expects it, and existing
 * schedules all become whole-app schedules.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `block_schedule_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`packageName` TEXT NOT NULL, " +
                "`scope` TEXT NOT NULL, " +
                "`startMinuteOfDay` INTEGER NOT NULL, " +
                "`endMinuteOfDay` INTEGER NOT NULL, " +
                "`daysOfWeekMask` INTEGER NOT NULL, " +
                "`enabled` INTEGER NOT NULL, " +
                "`strictMode` INTEGER NOT NULL)",
        )
        db.execSQL(
            "INSERT INTO `block_schedule_new` (`id`, `packageName`, `scope`, `startMinuteOfDay`, `endMinuteOfDay`, `daysOfWeekMask`, `enabled`, `strictMode`) " +
                "SELECT `id`, `packageName`, 'APP', `startMinuteOfDay`, `endMinuteOfDay`, `daysOfWeekMask`, `enabled`, `strictMode` FROM `block_schedule`",
        )
        db.execSQL("DROP TABLE `block_schedule`")
        db.execSQL("ALTER TABLE `block_schedule_new` RENAME TO `block_schedule`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_block_schedule_packageName` ON `block_schedule` (`packageName`)")
    }
}

/** v2 -> v3: daily time limits per app. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `app_limit` (" +
                "`packageName` TEXT NOT NULL, " +
                "`dailyMinutes` INTEGER NOT NULL, " +
                "`strict` INTEGER NOT NULL, " +
                "`enabled` INTEGER NOT NULL, " +
                "PRIMARY KEY(`packageName`))",
        )
    }
}
