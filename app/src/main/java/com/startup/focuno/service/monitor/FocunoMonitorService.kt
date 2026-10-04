package com.startup.focuno.service.monitor

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.startup.focuno.data.repository.ProtectionRepository
import com.startup.focuno.data.repository.UsageTrackingRepository
import com.startup.focuno.domain.usecase.ComputeDayStatsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Keeps the app alive in the background with a quiet notification showing today's focus score.
 * It also re-checks protection every 30 seconds, so a revoked accessibility permission is reported
 * within half a minute rather than at the next 15-minute watchdog run.
 */
@AndroidEntryPoint
class FocunoMonitorService : Service() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var protectionRepository: ProtectionRepository
    @Inject lateinit var usageRepository: UsageTrackingRepository
    @Inject lateinit var computeDayStats: ComputeDayStatsUseCase

    private var scope: CoroutineScope? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationHelper.ensureChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            }
            ServiceCompat.startForeground(this, NotificationHelper.MONITOR_ID, notificationHelper.monitorNotification(NO_SCORE), type)
        } catch (e: Exception) {
            Log.w(TAG, "Could not enter the foreground", e)
            stopSelf()
            return START_NOT_STICKY
        }
        if (scope == null) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { it.launch { monitorLoop() } }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope?.cancel()
        scope = null
        super.onDestroy()
    }

    private suspend fun CoroutineScope.monitorLoop() {
        var lastScoreText = NO_SCORE
        var lastScoreAtMs = 0L
        while (isActive) {
            try {
                notificationHelper.updateProtection(protectionRepository.status())
                val now = System.currentTimeMillis()
                if (now - lastScoreAtMs >= SCORE_REFRESH_MS) {
                    lastScoreAtMs = now
                    val score = if (usageRepository.hasUsageAccess()) computeDayStats(LocalDate.now()).focusScore else null
                    val text = score?.toString() ?: NO_SCORE
                    if (text != lastScoreText) {
                        lastScoreText = text
                        notificationHelper.updateMonitor(text)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Monitor pass failed", e)
            }
            delay(PROTECTION_CHECK_MS)
        }
    }

    private companion object {
        const val TAG = "FocunoMonitor"
        const val NO_SCORE = "—"
        const val PROTECTION_CHECK_MS = 30_000L
        const val SCORE_REFRESH_MS = 3 * 60_000L
    }
}

object MonitorServiceLauncher {
    private const val TAG = "MonitorLauncher"

    /** Android may refuse a foreground start from the background. That is logged, never fatal. */
    fun start(context: Context) {
        try {
            ContextCompat.startForegroundService(context, Intent(context, FocunoMonitorService::class.java))
        } catch (e: Exception) {
            Log.w(TAG, "Foreground start not allowed right now", e)
        }
    }
}
