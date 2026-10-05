package com.startup.focuno.ui.screens.settings

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.ui.components.Panel
import com.startup.focuno.ui.components.LoadingState
import com.startup.focuno.ui.components.RefreshWhileResumed
import com.startup.focuno.ui.components.SectionTitle
import com.startup.focuno.ui.components.SubScreenTopBar
import com.startup.focuno.ui.components.durationText
import com.startup.focuno.ui.theme.FocunoTheme
import java.text.DateFormat
import java.util.Date

/** Lets you compare Focuno's number with Android Settings, app by app, and see exactly what is left out. */
@Composable
fun UsageCheckScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: UsageCheckViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshWhileResumed(intervalMs = 15_000L, onRefresh = viewModel::refresh)

    Column(modifier.fillMaxSize()) {
        SubScreenTopBar(stringResource(R.string.usage_check_title), onBack)
        if (state.isLoading) {
            LoadingState()
            return@Column
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Panel(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.usage_check_counted_total), style = MaterialTheme.typography.labelLarge, color = FocunoTheme.colors.textSecondary)
                Text(durationText(state.totalMs), style = MaterialTheme.typography.displaySmall, color = FocunoTheme.colors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.usage_check_updated, DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(state.updatedAtMs))),
                    style = MaterialTheme.typography.bodySmall,
                    color = FocunoTheme.colors.textTertiary,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = viewModel::refresh) { Text(stringResource(R.string.usage_check_refresh)) }
            }
            Text(stringResource(R.string.usage_check_explainer), style = MaterialTheme.typography.bodyMedium, color = FocunoTheme.colors.textSecondary)

            if (!state.hasAccess) {
                Text(stringResource(R.string.apps_no_access_message), style = MaterialTheme.typography.bodyMedium, color = FocunoTheme.colors.warning)
            }
            UsageList(stringResource(R.string.usage_check_counted), state.counted, stringResource(R.string.usage_check_none))
            UsageList(stringResource(R.string.usage_check_not_counted), state.notCounted, stringResource(R.string.usage_check_none_excluded))
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun UsageList(title: String, lines: List<UsageLine>, emptyText: String) {
    Panel(Modifier.fillMaxWidth()) {
        SectionTitle(title)
        Spacer(Modifier.height(8.dp))
        if (lines.isEmpty()) {
            Text(emptyText, style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.textTertiary)
        }
        lines.forEach { line ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(line.label, style = MaterialTheme.typography.bodyMedium, color = FocunoTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                Text(durationText(line.foregroundMs), style = MaterialTheme.typography.bodyMedium, color = FocunoTheme.colors.textSecondary)
            }
        }
    }
}
