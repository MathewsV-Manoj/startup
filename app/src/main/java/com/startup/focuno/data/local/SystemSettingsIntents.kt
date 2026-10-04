package com.startup.focuno.data.local

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.startup.focuno.R

/** Intents that open the exact Android settings screen for each permission Focuno needs. */
object SystemSettingsIntents {

    private fun packageUri(context: Context): Uri = Uri.fromParts("package", context.packageName, null)

    fun usageAccess(context: Context): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).setData(packageUri(context))

    fun accessibility(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun overlay(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).setData(packageUri(context))

    fun batteryExemption(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(packageUri(context))

    fun batteryOptimizationList(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun appDetails(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(packageUri(context))

    fun notificationSettings(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    /** Starts the first intent that works. Returns false if none did, so the caller can show a fallback. */
    fun launchFirstThatWorks(context: Context, intents: List<Intent>): Boolean {
        for (intent in intents) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: ActivityNotFoundException) {
                continue
            } catch (e: SecurityException) {
                Log.w("SettingsIntents", "Not allowed to open ${intent.action ?: intent.component}", e)
            }
        }
        return false
    }
}

enum class OemFamily { XIAOMI, OPPO_REALME, VIVO, ONEPLUS, SAMSUNG, OTHER }

/**
 * Phone makers add their own battery managers that force-stop background apps, and Android revokes the
 * accessibility permission when an app is force-stopped. Each maker hides the fix in a different place.
 */
object OemAutostart {

    fun detect(manufacturer: String = Build.MANUFACTURER, brand: String = Build.BRAND): OemFamily {
        val key = "${manufacturer.lowercase()} ${brand.lowercase()}"
        return when {
            listOf("xiaomi", "redmi", "poco", "mi ").any { key.contains(it) } -> OemFamily.XIAOMI
            listOf("oppo", "realme").any { key.contains(it) } -> OemFamily.OPPO_REALME
            listOf("vivo", "iqoo").any { key.contains(it) } -> OemFamily.VIVO
            key.contains("oneplus") -> OemFamily.ONEPLUS
            key.contains("samsung") -> OemFamily.SAMSUNG
            else -> OemFamily.OTHER
        }
    }

    fun nameRes(family: OemFamily): Int = when (family) {
        OemFamily.XIAOMI -> R.string.oem_xiaomi_name
        OemFamily.OPPO_REALME -> R.string.oem_oppo_name
        OemFamily.VIVO -> R.string.oem_vivo_name
        OemFamily.ONEPLUS -> R.string.oem_oneplus_name
        OemFamily.SAMSUNG -> R.string.oem_samsung_name
        OemFamily.OTHER -> R.string.oem_other_name
    }

    fun stepRes(family: OemFamily): List<Int> = when (family) {
        OemFamily.XIAOMI -> listOf(R.string.oem_xiaomi_step1, R.string.oem_xiaomi_step2, R.string.oem_xiaomi_step3)
        OemFamily.OPPO_REALME -> listOf(R.string.oem_oppo_step1, R.string.oem_oppo_step2, R.string.oem_oppo_step3)
        OemFamily.VIVO -> listOf(R.string.oem_vivo_step1, R.string.oem_vivo_step2, R.string.oem_vivo_step3)
        OemFamily.ONEPLUS -> listOf(R.string.oem_oneplus_step1, R.string.oem_oneplus_step2, R.string.oem_oneplus_step3)
        OemFamily.SAMSUNG -> listOf(R.string.oem_samsung_step1, R.string.oem_samsung_step2, R.string.oem_samsung_step3)
        OemFamily.OTHER -> listOf(R.string.oem_other_step1, R.string.oem_other_step2, R.string.oem_other_step3)
    }

    /** Deep links that exist on some versions of each maker's software. The first one that opens wins. */
    fun deepLinks(family: OemFamily): List<Intent> = when (family) {
        OemFamily.XIAOMI -> listOf(
            component("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        )
        OemFamily.OPPO_REALME -> listOf(
            component("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            component("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            component("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
        )
        OemFamily.VIVO -> listOf(
            component("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            component("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
        )
        OemFamily.ONEPLUS -> listOf(
            component("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
        )
        OemFamily.SAMSUNG -> listOf(
            component("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
        )
        OemFamily.OTHER -> emptyList()
    }

    private fun component(pkg: String, cls: String) = Intent().setComponent(ComponentName(pkg, cls))
}
