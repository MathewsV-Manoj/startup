package com.startup.focuno.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.startup.focuno.R
import com.startup.focuno.ui.theme.FocunoTheme

/** The switch, plus the warning the person sees while it is off: without Strict a session can be ended early. */
@Composable
fun StrictSwitch(
    strict: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** Replaces the usual note, e.g. to say why the switch is disabled. */
    note: String? = null,
) {
    val tint = if (strict) FocunoTheme.colors.productive else FocunoTheme.colors.warning
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(if (strict) Icons.Rounded.Lock else Icons.Rounded.LockOpen, contentDescription = null, tint = tint)
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.strict_label), style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.textPrimary)
            Text(
                note ?: stringResource(if (strict) R.string.strict_on_note else R.string.strict_off_warning),
                style = MaterialTheme.typography.bodySmall,
                color = tint,
            )
        }
        Switch(checked = strict, onCheckedChange = onChange, enabled = enabled)
    }
}
