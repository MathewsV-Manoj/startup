package com.startup.focuno.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.startup.focuno.R
import com.startup.focuno.data.local.OemAutostart
import com.startup.focuno.data.local.OemFamily
import com.startup.focuno.data.local.SystemSettingsIntents
import com.startup.focuno.ui.theme.FocunoTheme

/** Android 13+ greys out the Accessibility switch for apps installed outside an app store. */
@Composable
fun RestrictedSettingCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    GlassCard(modifier.fillMaxWidth()) {
        Text(stringResource(R.string.restricted_title), style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.warning)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.restricted_body), style = MaterialTheme.typography.bodyMedium, color = FocunoTheme.colors.textSecondary)
        Spacer(Modifier.height(10.dp))
        NumberedSteps(listOf(R.string.restricted_step1, R.string.restricted_step2, R.string.restricted_step3, R.string.restricted_step4))
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.restricted_note), style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.textSecondary)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { SystemSettingsIntents.launchFirstThatWorks(context, listOf(SystemSettingsIntents.appDetails(context))) }) {
            Text(stringResource(R.string.restricted_open_app_info))
        }
    }
}

/** Step-by-step keep-alive instructions for the phone maker detected on this device. */
@Composable
fun OemAutostartCard(modifier: Modifier = Modifier, family: OemFamily = OemAutostart.detect()) {
    val context = LocalContext.current
    GlassCard(modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.oem_card_title, stringResource(OemAutostart.nameRes(family))),
            style = MaterialTheme.typography.titleMedium,
            color = FocunoTheme.colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.oem_card_body), style = MaterialTheme.typography.bodyMedium, color = FocunoTheme.colors.textSecondary)
        Spacer(Modifier.height(10.dp))
        NumberedSteps(OemAutostart.stepRes(family))
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                val links = OemAutostart.deepLinks(family) + SystemSettingsIntents.appDetails(context)
                SystemSettingsIntents.launchFirstThatWorks(context, links)
            },
        ) {
            Text(stringResource(R.string.oem_open_settings))
        }
    }
}

@Composable
fun NumberedSteps(stepResIds: List<Int>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        stepResIds.forEachIndexed { index, res ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Text("${index + 1}.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Text(stringResource(res), style = MaterialTheme.typography.bodyMedium, color = FocunoTheme.colors.textPrimary, modifier = Modifier.padding(end = 4.dp))
            }
        }
    }
}
