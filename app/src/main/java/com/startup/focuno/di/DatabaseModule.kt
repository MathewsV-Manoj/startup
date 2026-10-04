package com.startup.focuno.di

import android.content.Context
import androidx.room.Room
import com.startup.focuno.data.room.FocunoDatabase
import com.startup.focuno.data.room.MIGRATION_1_2
import com.startup.focuno.data.room.MIGRATION_2_3
import com.startup.focuno.data.room.MIGRATION_3_4
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FocunoDatabase =
        Room.databaseBuilder(context, FocunoDatabase::class.java, "focuno.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides fun dailyUsageDao(db: FocunoDatabase) = db.dailyUsageDao()
    @Provides fun dailySummaryDao(db: FocunoDatabase) = db.dailySummaryDao()
    @Provides fun hourlyDistractingDao(db: FocunoDatabase) = db.hourlyDistractingDao()
    @Provides fun blockScheduleDao(db: FocunoDatabase) = db.blockScheduleDao()
    @Provides fun bypassEventDao(db: FocunoDatabase) = db.bypassEventDao()
    @Provides fun blockHitDao(db: FocunoDatabase) = db.blockHitDao()
    @Provides fun focusSessionDao(db: FocunoDatabase) = db.focusSessionDao()
    @Provides fun appCategoryDao(db: FocunoDatabase) = db.appCategoryDao()
    @Provides fun appLimitDao(db: FocunoDatabase) = db.appLimitDao()
}
