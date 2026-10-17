package com.focuno.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.focuno.core.data.repository.DefaultUsageRepository
import com.focuno.core.domain.usecase.ComputeFocusScoreUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

/**
 * Periodically materializes today's usage and recomputes the focus score.
 * Scheduled by `:app` with a 15-minute periodic constraint.
 */
@HiltWorker
class AggregationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val usageRepository: DefaultUsageRepository,
    private val computeFocusScore: ComputeFocusScoreUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        val today = LocalDate.now()
        usageRepository.refresh(today)
        computeFocusScore(today)
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        const val UNIQUE_NAME = "focuno_aggregation"
    }
}
