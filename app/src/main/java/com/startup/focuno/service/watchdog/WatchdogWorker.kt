package com.startup.focuno.service.watchdog

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.startup.focuno.data.repository.ProtectionRepository
import com.startup.focuno.service.monitor.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Every 15 minutes: is the accessibility service still enabled and is the overlay permission still granted?
 * If either is gone, fire a high-priority notification. This is the backup for the 30-second check in the
 * monitor service, which itself can be killed by an aggressive battery manager.
 */
@HiltWorker
class WatchdogWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val protectionRepository: ProtectionRepository,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        notificationHelper.ensureChannels()
        notificationHelper.updateProtection(protectionRepository.status())
        return Result.success()
    }
}
