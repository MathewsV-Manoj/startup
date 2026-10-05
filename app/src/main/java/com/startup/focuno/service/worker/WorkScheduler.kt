package com.startup.focuno.service.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.startup.focuno.service.watchdog.WatchdogWorker
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val ROLLUP_NAME = "focuno_daily_rollup"
    private const val WATCHDOG_NAME = "focuno_watchdog"

    /** Safe to call any number of times: KEEP leaves an already-scheduled job alone. */
    fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val rollup = PeriodicWorkRequestBuilder<DailyRollupWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(millisUntilNextRollup(), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(ROLLUP_NAME, ExistingPeriodicWorkPolicy.KEEP, rollup)

        val watchdog = PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(WATCHDOG_NAME, ExistingPeriodicWorkPolicy.KEEP, watchdog)
    }

    /** Milliseconds until the next 00:05 local time. */
    internal fun millisUntilNextRollup(now: LocalDateTime = LocalDateTime.now()): Long {
        var next = now.toLocalDate().atTime(0, 5)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
    }
}
