package com.startup.focuno.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        ProtectionIssue.BACKGROUND_SERVICE -> R.string.issue_background
    },
)

/**
 * One line, only when something is wrong: red when blocking is off, amber when only screen time is affected.
 * The Fix button goes to the health check, which lists exactly what is missing.
 */
@Composable
fun ProtectionBanner(status: ProtectionStatus?, onFix: () -> Unit, modifier: Modifier = Modifier) {
    if (status == null) return
    val blockingBroken = !status.blockingActive
    if (!blockingBroken && status.usageAccess) return

    val tint = if (blockingBroken) FocunoTheme.colors.distracting else FocunoTheme.colors.warning
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tint.copy(alpha = 0.14f))
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Warning, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(
            text = stringResource(if (blockingBroken) R.string.banner_blocking_off else R.string.banner_usage_off),
            style = MaterialTheme.typography.titleSmall,
            color = FocunoTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onFix, colors = ButtonDefaults.textButtonColors(contentColor = tint)) {
            Text(stringResource(R.string.banner_fix_now), style = MaterialTheme.typography.labelLarge)
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
