package com.focuno.core.data.di

import android.content.Context
import androidx.room.Room
import com.focuno.core.data.local.BlockRuleDao
import com.focuno.core.data.local.FocunoDatabase
import com.focuno.core.data.local.FocusScoreDao
import com.focuno.core.data.local.UsageDao
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
        Room.databaseBuilder(context, FocunoDatabase::class.java, FocunoDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUsageDao(db: FocunoDatabase): UsageDao = db.usageDao()
    @Provides fun provideFocusScoreDao(db: FocunoDatabase): FocusScoreDao = db.focusScoreDao()
    @Provides fun provideBlockRuleDao(db: FocunoDatabase): BlockRuleDao = db.blockRuleDao()
}
