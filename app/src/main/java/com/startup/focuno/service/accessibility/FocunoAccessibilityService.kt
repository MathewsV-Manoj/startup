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
 * PRIVACY: this service looks at the package name of the app in front. When a Reels or Shorts rule is
 * active it also checks a few technical screen-part names to tell whether that feed is showing. It never
 * reads text, messages, usernames or any other content on screen.
 */
@AndroidEntryPoint
class FocunoAccessibilityService : AccessibilityService(), BlockHost {

    @Inject
    lateinit var engine: BlockEngine

    private var overlayManager: OverlayWindowManager? = null
    private var receiverRegistered = false
    private var baseEventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

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
        serviceInfo?.let { baseEventTypes = it.eventTypes and AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED.inv() }
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
        val packageName = event?.packageName?.toString() ?: return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> engine.onWindowChanged(packageName)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> engine.onContentChanged(packageName)
        }
    }

    override fun onInterrupt() = Unit

    override fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun goBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    override fun activeWindowPackage(): String? = rootInActiveWindow?.packageName?.toString()

    override fun isShortVideoShowing(packageName: String): Boolean =
        ShortVideoDetector.isShowing(rootInActiveWindow, packageName)

    override fun leaveShortVideoFeed(packageName: String): Boolean =
        ShortVideoDetector.leaveToHomeTab(rootInActiveWindow, packageName)

    override fun setContentEventsEnabled(enabled: Boolean) {
        val info = serviceInfo ?: return
        val wanted = if (enabled) baseEventTypes or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED else baseEventTypes
        if (info.eventTypes != wanted) {
            info.eventTypes = wanted
            serviceInfo = info
        }
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
        // The engine may already be running on the basic-mode host. Only stop it if it is still ours.
        if (engine.usesHost(this)) engine.stop()
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
