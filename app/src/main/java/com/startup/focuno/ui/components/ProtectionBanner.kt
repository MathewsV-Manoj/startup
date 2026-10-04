package com.startup.focuno.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.startup.focuno.R
import com.startup.focuno.data.repository.ProtectionIssue
import com.startup.focuno.data.repository.ProtectionStatus
import com.startup.focuno.ui.theme.FocunoTheme

@Composable
fun issueLabel(issue: ProtectionIssue): String = stringResource(
    when (issue) {
        ProtectionIssue.ACCESSIBILITY -> R.string.issue_accessibility
        ProtectionIssue.OVERLAY -> R.string.issue_overlay
        ProtectionIssue.USAGE_ACCESS -> R.string.issue_usage_access
        ProtectionIssue.NOTIFICATIONS -> R.string.issue_notifications
        ProtectionIssue.BATTERY -> R.string.issue_battery
    },
)

/**
 * Only drawn when something is actually wrong. Red when blocking is not running (impossible to miss),
 * amber when only screen-time measuring is affected. Hidden otherwise.
 */
@Composable
fun ProtectionBanner(status: ProtectionStatus?, onFix: () -> Unit, modifier: Modifier = Modifier) {
    if (status == null) return
    val blockingBroken = !status.blockingActive
    val usageBroken = !status.usageAccess
    if (!blockingBroken && !usageBroken) return

    val tint = if (blockingBroken) FocunoTheme.colors.distracting else FocunoTheme.colors.warning
    val shape = RoundedCornerShape(20.dp)
    val missing = status.issues.filter {
        if (blockingBroken) it == ProtectionIssue.ACCESSIBILITY || it == ProtectionIssue.OVERLAY else it == ProtectionIssue.USAGE_ACCESS
    }

    // map is inline, so the composable label lookup is allowed here; joinToString is not inline.
    val missingText = missing.map { issueLabel(it) }.joinToString(", ")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.16f), shape)
            .border(1.dp, tint.copy(alpha = 0.7f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Warning, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            Text(
                text = stringResource(if (blockingBroken) R.string.banner_blocking_off_title else R.string.banner_usage_off_title),
                style = MaterialTheme.typography.titleMedium,
                color = FocunoTheme.colors.textPrimary,
            )
        }
        Text(
            text = stringResource(
                if (blockingBroken) R.string.banner_blocking_off_body else R.string.banner_usage_off_body,
                missingText,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = FocunoTheme.colors.textSecondary,
        )
        Button(
            onClick = onFix,
            colors = ButtonDefaults.buttonColors(containerColor = tint, contentColor = FocunoTheme.colors.trackInactive),
        ) {
            Text(stringResource(R.string.banner_fix_now))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0417)
@Composable
private fun ProtectionBannerPreview() {
    FocunoTheme {
        ProtectionBanner(
            status = ProtectionStatus(usageAccess = true, accessibilityEnabled = false, overlayGranted = true, notificationsGranted = true, batteryExempt = false),
            onFix = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
