package com.startup.focuno.data.repository

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.startup.focuno.service.accessibility.FocunoAccessibilityService
import com.startup.focuno.service.monitor.FocunoMonitorService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ProtectionIssue { USAGE_ACCESS, ACCESSIBILITY, OVERLAY, NOTIFICATIONS, BATTERY, BACKGROUND_SERVICE }

data class ProtectionStatus(
    val usageAccess: Boolean,
    val accessibilityEnabled: Boolean,
    val overlayGranted: Boolean,
    val notificationsGranted: Boolean,
    val batteryExempt: Boolean,
    /** True while Focuno's own background service is running, which basic blocking depends on. */
    val monitorRunning: Boolean = false,
) {
    /**
     * Blocking works through Accessibility, or through basic mode (Usage access plus the background service).
     * Either way the pause screen needs permission to draw over other apps.
     */
    val blockingActive: Boolean get() = overlayGranted && (accessibilityEnabled || (usageAccess && monitorRunning))

    /** What is missing for blocking to work, or empty when it already does. */
    val blockingIssues: List<ProtectionIssue>
        get() = if (blockingActive) emptyList() else buildList {
            if (!overlayGranted) add(ProtectionIssue.OVERLAY)
            if (!accessibilityEnabled) {
                if (!usageAccess) add(ProtectionIssue.USAGE_ACCESS) else if (!monitorRunning) add(ProtectionIssue.BACKGROUND_SERVICE)
            }
        }

    /** Blocking is on, but without Accessibility, so Reels-only blocking is not available. */
    val basicModeOnly: Boolean get() = blockingActive && !accessibilityEnabled

    val issues: List<ProtectionIssue>
        get() = buildList {
            if (!accessibilityEnabled) add(ProtectionIssue.ACCESSIBILITY)
            if (!overlayGranted) add(ProtectionIssue.OVERLAY)
            if (!usageAccess) add(ProtectionIssue.USAGE_ACCESS)
            if (!notificationsGranted) add(ProtectionIssue.NOTIFICATIONS)
            if (!batteryExempt) add(ProtectionIssue.BATTERY)
        }
}

@Singleton
class ProtectionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageRepository: UsageTrackingRepository,
) {
    fun status(): ProtectionStatus = ProtectionStatus(
        usageAccess = usageRepository.hasUsageAccess(),
        accessibilityEnabled = isAccessibilityServiceEnabled(),
        overlayGranted = Settings.canDrawOverlays(context),
        notificationsGranted = areNotificationsGranted(),
        batteryExempt = isIgnoringBatteryOptimizations(),
        monitorRunning = FocunoMonitorService.isRunning,
    )

    fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val ours = ComponentName(context, FocunoAccessibilityService::class.java)
        return enabled.split(':').any { entry ->
            ComponentName.unflattenFromString(entry)?.let { it == ours } ?: false
        }
    }

    fun areNotificationsGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun isIgnoringBatteryOptimizations(): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    /**
     * Android 13+ greys out the Accessibility toggle for apps installed outside an app store
     * ("Restricted setting"). There is no public API to read that flag, so this is a heuristic:
     * a sideloaded build on API 33+ is very likely to need the manual unlock.
     */
    fun likelyNeedsRestrictedSettingUnlock(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val installer = try {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        return installer != PLAY_STORE
    }

    private companion object {
        const val PLAY_STORE = "com.android.vending"
    }
}
