package com.startup.focuno.service.accessibility

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/** A window is not an Activity, so Compose needs a lifecycle and a saved-state owner supplied by hand. */
private class OverlayLifecycleOwner : SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun resume() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}

private class ComposeOverlayRoot(
    context: Context,
    private val consumeBack: Boolean,
    content: @Composable () -> Unit,
) : FrameLayout(context) {

    val owner = OverlayLifecycleOwner()

    init {
        owner.resume()
        setViewTreeLifecycleOwner(owner as LifecycleOwner)
        setViewTreeSavedStateRegistryOwner(owner)
        addView(
            ComposeView(context).apply { setContent(content) },
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
    }

    /** The block screen must not be dismissible with the Back button. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (consumeBack && event.keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.dispatchKeyEvent(event)
    }
}

/**
 * Draws the block screen and nudge toasts as windows above other apps.
 * Uses a SYSTEM_ALERT_WINDOW overlay when the permission is granted, and falls back to an
 * accessibility overlay (which needs no extra permission) when it is not.
 */
class OverlayWindowManager(private val context: Context) {

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var blockRoot: ComposeOverlayRoot? = null
    private var nudgeRoot: ComposeOverlayRoot? = null
    private val hideNudgeRunnable = Runnable { hideNudge() }

    val isBlockShowing: Boolean get() = blockRoot != null

    /** Returns false if Android refused to draw the window, so the caller can fall back to another way of blocking. */
    fun showBlock(content: @Composable () -> Unit): Boolean {
        hideBlock()
        val root = ComposeOverlayRoot(context, consumeBack = true, content = content)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            title = "Focuno block"
            applyCutoutMode()
        }
        return if (addSafely(root, params)) {
            blockRoot = root
            true
        } else {
            root.owner.destroy()
            false
        }
    }

    fun hideBlock() {
        blockRoot?.let { removeSafely(it) }
        blockRoot = null
    }

    fun showNudge(durationMs: Long, content: @Composable () -> Unit) {
        hideNudge()
        val root = ComposeOverlayRoot(context, consumeBack = false, content = content)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            title = "Focuno nudge"
        }
        if (addSafely(root, params)) {
            nudgeRoot = root
            mainHandler.postDelayed(hideNudgeRunnable, durationMs)
        } else {
            root.owner.destroy()
        }
    }

    fun hideNudge() {
        mainHandler.removeCallbacks(hideNudgeRunnable)
        nudgeRoot?.let { removeSafely(it) }
        nudgeRoot = null
    }

    fun hideAll() {
        hideBlock()
        hideNudge()
    }

    private fun overlayType(): Int =
        if (Settings.canDrawOverlays(context)) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        }

    private fun WindowManager.LayoutParams.applyCutoutMode() {
        layoutInDisplayCutoutMode = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            else -> return
        }
    }

    private fun addSafely(view: View, params: WindowManager.LayoutParams): Boolean {
        try {
            windowManager.addView(view, params)
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Overlay add failed with type ${params.type}, retrying as accessibility overlay", e)
        }
        return try {
            params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            windowManager.addView(view, params)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not show overlay at all", e)
            false
        }
    }

    private fun removeSafely(root: ComposeOverlayRoot) {
        try {
            windowManager.removeView(root)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Overlay was already gone", e)
        }
        root.owner.destroy()
    }

    private companion object {
        const val TAG = "OverlayWindowManager"
    }
}
