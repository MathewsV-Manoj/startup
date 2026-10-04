package com.startup.focuno.service.monitor

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.startup.focuno.data.repository.ProtectionRepository
import com.startup.focuno.data.repository.UsageTrackingRepository
import com.startup.focuno.domain.usecase.ComputeDayStatsUseCase
import com.startup.focuno.service.accessibility.BlockEngine
import com.startup.focuno.service.accessibility.EngineLog
import com.startup.focuno.service.accessibility.FocunoAccessibilityService
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
 *
 * It also does two protective jobs:
 *  - every 30 seconds it re-checks protection, so a revoked permission is reported within half a minute;
 *  - while the accessibility service is not running, it runs BASIC blocking itself: it follows the foreground
 *    app from usage events and lets the blocking engine draw the pause screen. That means blocking still works
 *    for people whose phone will not let them switch Accessibility on.
 */
@AndroidEntryPoint
class FocunoMonitorService : Service() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var protectionRepository: ProtectionRepository
    @Inject lateinit var usageRepository: UsageTrackingRepository
    @Inject lateinit var computeDayStats: ComputeDayStatsUseCase
    @Inject lateinit var engine: BlockEngine
    @Inject lateinit var engineLog: EngineLog

    private var backgroundScope: CoroutineScope? = null
    private var mainScope: CoroutineScope? = null

    private val tracker = ForegroundTracker()
    private val fallbackHost by lazy { FallbackBlockHost(this, tracker) }
    private var lastPollMs = 0L
    private var receiverRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!engine.usesHost(fallbackHost)) return
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> engine.onScreenOff()
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> engine.onScreenOn()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationHelper.ensureChannels()
        isRunning = true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
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
        if (backgroundScope == null) {
            backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { it.launch { monitorLoop() } }
        }
        if (mainScope == null) {
            // The blocking engine and its overlay belong to the main thread.
            mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { it.launch { basicBlockingLoop() } }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        if (receiverRegistered) {
            unregisterReceiver(screenReceiver)
            receiverRegistered = false
        }
        if (engine.usesHost(fallbackHost)) engine.stop()
        backgroundScope?.cancel()
        backgroundScope = null
        mainScope?.cancel()
        mainScope = null
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

    /**
     * Starts the engine on the basic host whenever Accessibility is not running but Usage access and the
     * overlay permission are granted, and hands over cleanly when Accessibility takes over (or the permissions go).
     */
    private suspend fun CoroutineScope.basicBlockingLoop() {
        while (isActive) {
            var waitMs = IDLE_CHECK_MS
            try {
                val canRun = !FocunoAccessibilityService.isConnected &&
                    usageRepository.hasUsageAccess() &&
                    Settings.canDrawOverlays(this@FocunoMonitorService)
                val usingBasic = engine.usesHost(fallbackHost)
                when {
                    canRun && !engine.isRunning -> startBasicBlocking()
                    !canRun && usingBasic -> {
                        engine.stop()
                        engineLog.add("Basic blocking stopped")
                    }
                }
                if (engine.usesHost(fallbackHost)) {
                    pollForeground()
                    waitMs = POLL_MS
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Basic blocking pass failed", e)
            }
            delay(waitMs)
        }
    }

    private suspend fun startBasicBlocking() {
        tracker.reset(usageRepository.currentForegroundPackage())
        lastPollMs = System.currentTimeMillis()
        engine.start(fallbackHost)
        engineLog.add("Basic blocking started (no Accessibility needed)")
    }

    private suspend fun pollForeground() {
        val now = System.currentTimeMillis()
        val events = usageRepository.recentEvents(lastPollMs - POLL_OVERLAP_MS, now)
        lastPollMs = now
        if (tracker.apply(events)) tracker.current?.let { engine.onWindowChanged(it) }
    }

    companion object {
        private const val TAG = "FocunoMonitor"
        private const val NO_SCORE = "—"
        private const val PROTECTION_CHECK_MS = 30_000L
        private const val SCORE_REFRESH_MS = 3 * 60_000L
        private const val POLL_MS = 700L
        private const val POLL_OVERLAP_MS = 1_500L
        private const val IDLE_CHECK_MS = 3_000L

        /** True while this service is alive. Basic blocking only works while it is. */
        @Volatile
        var isRunning: Boolean = false
            private set
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
