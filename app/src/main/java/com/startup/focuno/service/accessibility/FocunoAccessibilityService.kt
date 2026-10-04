package com.startup.focuno.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.startup.focuno.service.monitor.MonitorServiceLauncher
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * PRIVACY: this service reads package names only. It does not request window content
 * (canRetrieveWindowContent is false in accessibility_service_config.xml), never touches text, and
 * listens to window-state changes only. The package name of the app in front is the entire input.
 */
@AndroidEntryPoint
class FocunoAccessibilityService : AccessibilityService(), BlockHost {

    @Inject
    lateinit var engine: BlockEngine

    private var overlayManager: OverlayWindowManager? = null
    private var receiverRegistered = false

    override val overlay: OverlayWindowManager
        get() = overlayManager ?: OverlayWindowManager(this).also { overlayManager = it }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> engine.onScreenOff()
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> engine.onScreenOn()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isConnected = true
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverRegistered = true
        }
        engine.start(this)
        MonitorServiceLauncher.start(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        engine.onWindowChanged(packageName)
    }

    override fun onInterrupt() = Unit

    override fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        shutDown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutDown()
        super.onDestroy()
    }

    private fun shutDown() {
        isConnected = false
        engine.stop()
        if (receiverRegistered) {
            unregisterReceiver(screenReceiver)
            receiverRegistered = false
        }
        overlayManager = null
    }

    companion object {
        /** True only while Android is actually running the service, as opposed to merely having it enabled. */
        @Volatile
        var isConnected: Boolean = false
            private set
    }
}
