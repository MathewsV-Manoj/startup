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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ProtectionIssue { USAGE_ACCESS, ACCESSIBILITY, OVERLAY, NOTIFICATIONS, BATTERY }

data class ProtectionStatus(
    val usageAccess: Boolean,
    val accessibilityEnabled: Boolean,
    val overlayGranted: Boolean,
    val notificationsGranted: Boolean,
    val batteryExempt: Boolean,
) {
    /** Blocking only works when Android lets the service run AND the block screen can be drawn. */
    val blockingActive: Boolean get() = accessibilityEnabled && overlayGranted

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
