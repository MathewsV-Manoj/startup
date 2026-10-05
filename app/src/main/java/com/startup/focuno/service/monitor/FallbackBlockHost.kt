package com.startup.focuno.service.monitor

import android.content.Context
import android.content.Intent
import com.startup.focuno.service.accessibility.BlockHost
import com.startup.focuno.service.accessibility.OverlayWindowManager

/**
 * Lets the blocking engine run WITHOUT the accessibility service, using Usage access to see the app in front
 * and "Display over other apps" to draw the pause screen. Going Home is allowed from the background because the
 * overlay permission exempts the app from the background-launch rule.
 *
 * It cannot see inside other apps, so blocking only Reels or Shorts is not available in this mode.
 */
class FallbackBlockHost(
    private val context: Context,
    private val tracker: ForegroundTracker,
) : BlockHost {

    override val overlay: OverlayWindowManager by lazy { OverlayWindowManager(context) }

    override fun goHome() {
        context.startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override fun goBack() = goHome()

    override fun activeWindowPackage(): String? = tracker.current

    override fun isShortVideoShowing(packageName: String): Boolean = false

    override fun leaveShortVideoFeed(packageName: String): Boolean = false

    override fun setContentEventsEnabled(enabled: Boolean) = Unit
}
