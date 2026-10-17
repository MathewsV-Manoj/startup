package com.focuno.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.focuno.core.data.worker.AggregationWorker
import java.util.concurrent.TimeUnit

/**
 * Schedules periodic usage aggregation + score computation. Kept in `:app` so
 * `:core:data` stays free of scheduling policy.
 */
object WorkScheduler {

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<AggregationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AggregationWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
