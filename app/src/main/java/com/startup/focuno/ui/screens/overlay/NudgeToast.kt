package com.startup.focuno.ui.screens.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.startup.focuno.ui.components.Panel
import com.startup.focuno.ui.theme.FocunoTheme

/** A brief, dismissible card. Never a block: tapping anywhere on it closes it. */
@Composable
fun NudgeToast(text: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    FocunoTheme {
        Panel(
            modifier = modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clickable(onClick = onDismiss),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = FocunoTheme.colors.textPrimary,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0417)
@Composable
private fun NudgeToastPreview() {
    NudgeToast(text = "You've opened Instagram 7 times in the last hour.", onDismiss = {})
}
