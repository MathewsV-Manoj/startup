package com.focuno.core.data.di

import com.focuno.core.common.DispatcherProvider
import com.focuno.core.data.repository.DefaultBlockRuleRepository
import com.focuno.core.data.repository.DefaultFocusScoreRepository
import com.focuno.core.data.repository.DefaultUsageRepository
import com.focuno.core.data.repository.DefaultUserFocusProfileRepository
import com.focuno.core.domain.repository.BlockRuleRepository
import com.focuno.core.domain.repository.FocusScoreRepository
import com.focuno.core.domain.repository.UsageRepository
import com.focuno.core.domain.repository.UserFocusProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUsageRepository(impl: DefaultUsageRepository): UsageRepository

    @Binds
    @Singleton
    abstract fun bindFocusScoreRepository(impl: DefaultFocusScoreRepository): FocusScoreRepository

    @Binds
    @Singleton
    abstract fun bindBlockRuleRepository(impl: DefaultBlockRuleRepository): BlockRuleRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: DefaultUserFocusProfileRepository): UserFocusProfileRepository

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider
}
