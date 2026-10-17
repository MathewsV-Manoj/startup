package com.focuno.core.domain.usecase

import com.focuno.core.domain.repository.UsageRepository
import com.focuno.core.model.AppUsage
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GetDailyUsageUseCase @Inject constructor(
    private val repository: UsageRepository,
) {
    suspend operator fun invoke(date: LocalDate): List<AppUsage> = repository.usageFor(date)
}

class ObserveDailyUsageUseCase @Inject constructor(
    private val repository: UsageRepository,
) {
    operator fun invoke(date: LocalDate): Flow<List<AppUsage>> = repository.observeUsage(date)
}
