package com.startup.focuno.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.startup.focuno.R
import com.startup.focuno.ui.theme.FocunoTheme

/** Shown once after an update: one line per new thing, so nothing new goes unnoticed. */
@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    val items = listOf(
        Icons.Rounded.Timer to R.string.new_focus_modes,
        Icons.Rounded.Lock to R.string.new_strict,
        Icons.Rounded.HourglassBottom to R.string.new_limits,
        Icons.Rounded.Block to R.string.new_lock_mode,
        Icons.Rounded.SelfImprovement to R.string.new_mindful,
        Icons.Rounded.School to R.string.new_exam,
        Icons.Rounded.Widgets to R.string.new_widget,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items.forEach { (icon, text) -> NewRow(icon, stringResource(text)) }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.new_ok)) } },
    )
}

@Composable
private fun NewRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = FocunoTheme.colors.textPrimary)
    }
}
