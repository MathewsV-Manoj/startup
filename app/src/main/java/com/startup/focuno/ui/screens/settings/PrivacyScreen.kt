package com.startup.focuno.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.startup.focuno.R
import com.startup.focuno.ui.components.Panel
import com.startup.focuno.ui.components.SubScreenTopBar
import com.startup.focuno.ui.theme.FocunoTheme

@Composable
fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        SubScreenTopBar(stringResource(R.string.privacy_title), onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Panel(Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.privacy_paragraph),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FocunoTheme.colors.textPrimary,
                )
            }
            Panel(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.privacy_reads_title), style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.textPrimary)
                Text(
                    stringResource(R.string.privacy_reads_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocunoTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Panel(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.privacy_never_title), style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.textPrimary)
                Text(
                    stringResource(R.string.privacy_never_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocunoTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
