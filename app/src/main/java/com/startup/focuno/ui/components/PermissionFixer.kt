package com.startup.focuno.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.startup.focuno.data.local.SystemSettingsIntents
import com.startup.focuno.data.repository.ProtectionIssue
import com.startup.focuno.service.monitor.MonitorServiceLauncher

/** Returns a function that takes the person to the exact place that fixes a given permission. */
@Composable
fun rememberPermissionFixer(): (ProtectionIssue) -> Unit {
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Android stops showing the permission prompt after two refusals, so fall back to the settings page.
        if (!granted) SystemSettingsIntents.launchFirstThatWorks(context, listOf(SystemSettingsIntents.notificationSettings(context)))
    }
    return remember(context, notificationLauncher) {
        { issue: ProtectionIssue ->
            when (issue) {
                ProtectionIssue.USAGE_ACCESS -> SystemSettingsIntents.launchFirstThatWorks(
                    context,
                    listOf(SystemSettingsIntents.usageAccess(context), android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)),
                )
                ProtectionIssue.ACCESSIBILITY -> SystemSettingsIntents.launchFirstThatWorks(context, listOf(SystemSettingsIntents.accessibility()))
                ProtectionIssue.OVERLAY -> SystemSettingsIntents.launchFirstThatWorks(
                    context,
                    listOf(SystemSettingsIntents.overlay(context), SystemSettingsIntents.appDetails(context)),
                )
                ProtectionIssue.NOTIFICATIONS ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        true
                    } else {
                        SystemSettingsIntents.launchFirstThatWorks(context, listOf(SystemSettingsIntents.notificationSettings(context)))
                    }
                ProtectionIssue.BATTERY -> SystemSettingsIntents.launchFirstThatWorks(
                    context,
                    listOf(SystemSettingsIntents.batteryExemption(context), SystemSettingsIntents.batteryOptimizationList()),
                )
                ProtectionIssue.BACKGROUND_SERVICE -> MonitorServiceLauncher.start(context)
            }
            Unit
        }
    }
}
