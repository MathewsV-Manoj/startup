package com.startup.focuno.ui.screens.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.data.repository.ProtectionIssue
import com.startup.focuno.data.repository.ProtectionStatus
import com.startup.focuno.ui.components.Panel
import com.startup.focuno.ui.components.LoadingState
import com.startup.focuno.ui.components.OemAutostartCard
import com.startup.focuno.ui.components.RefreshWhileResumed
import com.startup.focuno.ui.components.RestrictedSettingCard
import com.startup.focuno.ui.components.SubScreenTopBar
import com.startup.focuno.ui.components.issueLabel
import com.startup.focuno.ui.components.rememberPermissionFixer
import com.startup.focuno.ui.theme.FocunoTheme

@Composable
fun HealthScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: HealthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val log by viewModel.log.collectAsStateWithLifecycle()
    // Refresh on every return from Android settings, which is when a permission has probably just changed.
    RefreshWhileResumed(intervalMs = 5_000L, onRefresh = viewModel::refresh)
    val fix = rememberPermissionFixer()

    Column(modifier.fillMaxSize()) {
        SubScreenTopBar(stringResource(R.string.health_title), onBack)
        val status = state.status
        if (status == null) {
            LoadingState()
            return@Column
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(
                    when {
                        !status.blockingActive -> R.string.health_needs_attention
                        status.basicModeOnly -> R.string.health_basic_mode
                        else -> R.string.health_all_good
                    },
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    !status.blockingActive -> FocunoTheme.colors.distracting
                    status.basicModeOnly -> FocunoTheme.colors.warning
                    else -> FocunoTheme.colors.productive
                },
            )

            val items = listOf(
                ProtectionIssue.ACCESSIBILITY to R.string.health_accessibility_why,
                ProtectionIssue.OVERLAY to R.string.health_overlay_why,
                ProtectionIssue.USAGE_ACCESS to R.string.health_usage_why,
                ProtectionIssue.NOTIFICATIONS to R.string.health_notifications_why,
                ProtectionIssue.BATTERY to R.string.health_battery_why,
            )
            items.forEach { (issue, whyRes) ->
                HealthRow(
                    issue = issue,
                    why = stringResource(whyRes),
                    granted = isGranted(status, issue),
                    note = if (issue == ProtectionIssue.ACCESSIBILITY && state.accessibilityEnabledButIdle) stringResource(R.string.health_accessibility_idle) else null,
                    onFix = { fix(issue) },
                )
            }
            if (state.showRestrictedHelp) RestrictedSettingCard()
            OemAutostartCard(family = state.oem)
            BlockerLogCard(log)
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun isGranted(status: ProtectionStatus, issue: ProtectionIssue): Boolean = when (issue) {
    ProtectionIssue.ACCESSIBILITY -> status.accessibilityEnabled
    ProtectionIssue.OVERLAY -> status.overlayGranted
    ProtectionIssue.USAGE_ACCESS -> status.usageAccess
    ProtectionIssue.NOTIFICATIONS -> status.notificationsGranted
    ProtectionIssue.BATTERY -> status.batteryExempt
    ProtectionIssue.BACKGROUND_SERVICE -> status.monitorRunning
}

@Composable
private fun HealthRow(issue: ProtectionIssue, why: String, granted: Boolean, note: String?, onFix: () -> Unit) {
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                contentDescription = stringResource(if (granted) R.string.health_status_ok else R.string.health_status_needs_fix),
                tint = if (granted) FocunoTheme.colors.productive else FocunoTheme.colors.distracting,
            )
            Column(Modifier.weight(1f)) {
                Text(issueLabel(issue), style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.textPrimary)
                Text(why, style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.textSecondary)
                if (note != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(note, style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.warning)
                }
            }
            if (!granted) Button(onClick = onFix) { Text(stringResource(R.string.health_fix)) }
        }
    }
}
