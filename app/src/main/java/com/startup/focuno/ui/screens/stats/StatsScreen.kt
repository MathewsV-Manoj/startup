package com.startup.focuno.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.ui.components.FocusGauge
import com.startup.focuno.ui.components.GlassCard
import com.startup.focuno.ui.components.RefreshWhileResumed
import com.startup.focuno.ui.components.ScreenTitle
import com.startup.focuno.ui.components.durationText
import com.startup.focuno.ui.screens.apps.AppsScreen
import com.startup.focuno.ui.screens.focus.FocusViewModel
import com.startup.focuno.ui.screens.insights.InsightsScreen
import com.startup.focuno.ui.theme.FocunoTheme
import java.text.DateFormat
import java.util.Date

/** Apps and Trends in one place, with today's score and total always on top. */
@Composable
fun StatsScreen(
    onOpenHealth: () -> Unit,
    onScheduleApp: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusViewModel: FocusViewModel = hiltViewModel(),
) {
    val focus by focusViewModel.state.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableIntStateOf(0) }
    RefreshWhileResumed(intervalMs = 20_000L, onRefresh = focusViewModel::refresh)

    Column(modifier.fillMaxSize()) {
        ScreenTitle(stringResource(R.string.tab_stats))
        if (focus.hasUsageAccess) {
            GlassCard(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FocusGauge(score = focus.today?.focusScore, size = 112.dp, compact = true)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.stats_today_on_phone), style = MaterialTheme.typography.labelLarge, color = FocunoTheme.colors.textSecondary)
                        Text(
                            text = focus.today?.let { durationText(it.totalMs) } ?: stringResource(R.string.gauge_dash),
                            style = MaterialTheme.typography.headlineMedium,
                            color = FocunoTheme.colors.textPrimary,
                        )
                        if (focus.updatedAtMs > 0) {
                            Text(
                                text = stringResource(R.string.usage_check_updated, DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(focus.updatedAtMs))),
                                style = MaterialTheme.typography.labelSmall,
                                color = FocunoTheme.colors.textTertiary,
                            )
                        }
                        TextButton(onClick = focusViewModel::refresh) { Text(stringResource(R.string.usage_check_refresh)) }
                    }
                }
            }
        }
        Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = section == 0, onClick = { section = 0 }, label = { Text(stringResource(R.string.stats_apps)) })
            FilterChip(selected = section == 1, onClick = { section = 1 }, label = { Text(stringResource(R.string.stats_trends)) })
        }
        when (section) {
            0 -> AppsScreen(onOpenHealth = onOpenHealth, onScheduleApp = onScheduleApp, showTitle = false)
            else -> InsightsScreen(showTitle = false)
        }
    }
}
