package com.startup.focuno.service.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.startup.focuno.domain.model.ShortVideoApps

/**
 * Tells whether Instagram is showing Reels or YouTube is showing Shorts.
 *
 * PRIVACY: this looks ONLY at a few technical screen-part names (view ids) and whether they are selected or
 * visible. It never reads text, usernames, messages, captions or any other content.
 *
 * These ids belong to the other apps and they can change in an app update, which would stop this feature
 * working until Focuno is updated. They could not be checked against live apps while writing this.
 */
object ShortVideoDetector {

    private const val IG_PAGER = "${ShortVideoApps.INSTAGRAM}:id/clips_viewer_view_pager"
    private const val IG_REELS_TAB = "${ShortVideoApps.INSTAGRAM}:id/clips_tab"
    private const val IG_HOME_TAB = "${ShortVideoApps.INSTAGRAM}:id/feed_tab"
    private const val YT_REEL_LIST = "${ShortVideoApps.YOUTUBE}:id/reel_recycler"
    private const val YT_REEL_PAGE = "${ShortVideoApps.YOUTUBE}:id/reel_player_page_container"

    fun isShowing(root: AccessibilityNodeInfo?, packageName: String): Boolean {
        if (root == null) return false
        return when (packageName) {
            ShortVideoApps.INSTAGRAM ->
                anyVisible(root, IG_PAGER) || anySelected(root, IG_REELS_TAB)
            ShortVideoApps.YOUTUBE ->
                anyVisible(root, YT_REEL_LIST) || anyVisible(root, YT_REEL_PAGE)
            else -> false
        }
    }

    /** Taps the app's Home tab to leave the short-video feed, if this app has a known one. */
    fun leaveToHomeTab(root: AccessibilityNodeInfo?, packageName: String): Boolean {
        if (root == null || packageName != ShortVideoApps.INSTAGRAM) return false
        for (node in root.findAccessibilityNodeInfosByViewId(IG_HOME_TAB).orEmpty()) {
            var current: AccessibilityNodeInfo? = node
            repeat(MAX_PARENT_HOPS) {
                if (current?.isClickable == true && current?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return true
                current = current?.parent
            }
        }
        return false
    }

    private fun anyVisible(root: AccessibilityNodeInfo, viewId: String): Boolean =
        root.findAccessibilityNodeInfosByViewId(viewId).orEmpty().any { it.isVisibleToUser }

    private fun anySelected(root: AccessibilityNodeInfo, viewId: String): Boolean =
        root.findAccessibilityNodeInfosByViewId(viewId).orEmpty().any { it.isSelected }

    private const val MAX_PARENT_HOPS = 4
}
