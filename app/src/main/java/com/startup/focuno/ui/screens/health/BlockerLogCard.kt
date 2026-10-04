package com.startup.focuno.ui.screens.health

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.startup.focuno.R
import com.startup.focuno.ui.components.GlassCard
import com.startup.focuno.ui.theme.FocunoTheme

/** The last things the blocker saw and decided. Handy for a screenshot when a block misbehaves. */
@Composable
fun BlockerLogCard(lines: List<String>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    GlassCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.health_log_title),
                style = MaterialTheme.typography.titleMedium,
                color = FocunoTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                enabled = lines.isNotEmpty(),
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("Focuno blocker log", lines.joinToString("\n")))
                },
            ) {
                Text(stringResource(R.string.health_log_copy))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.health_log_explainer), style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.textSecondary)
        Spacer(Modifier.height(10.dp))
        if (lines.isEmpty()) {
            Text(stringResource(R.string.health_log_empty), style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.textTertiary)
        } else {
            lines.takeLast(25).forEach { line ->
                Text(line, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = FocunoTheme.colors.textSecondary)
            }
        }
    }
}
