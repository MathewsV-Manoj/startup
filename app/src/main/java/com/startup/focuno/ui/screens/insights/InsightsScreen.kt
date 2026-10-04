package com.startup.focuno.ui.screens.insights

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.FilterChip
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
import com.startup.focuno.ui.components.BarChart
import com.startup.focuno.ui.components.EmptyState
import com.startup.focuno.ui.components.GlassCard
import com.startup.focuno.ui.components.LineChart
import com.startup.focuno.ui.components.LoadingState
import com.startup.focuno.ui.components.RefreshWhileResumed
import com.startup.focuno.ui.components.ScreenTitle
import com.startup.focuno.ui.components.SectionTitle
import com.startup.focuno.ui.components.StatTile
import com.startup.focuno.ui.components.durationText
import com.startup.focuno.ui.components.formatClock
import com.startup.focuno.ui.theme.FocunoTheme
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun InsightsScreen(modifier: Modifier = Modifier, viewModel: InsightsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshWhileResumed(intervalMs = 60_000L, onRefresh = viewModel::refresh)
    InsightsContent(state = state, onSelectRange = viewModel::selectRange, modifier = modifier)
}

@Composable
fun InsightsContent(state: InsightsUiState, onSelectRange: (InsightsRange) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        ScreenTitle(stringResource(R.string.tab_insights))
        if (state.isLoading) {
            LoadingState()
            return@Column
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InsightsRange.entries.forEach { option ->
                    FilterChip(
                        selected = option == state.range,
                        onClick = { onSelectRange(option) },
                        label = { Text(stringResource(if (option == InsightsRange.WEEK) R.string.insights_7_days else R.string.insights_30_days)) },
                    )
                }
            }

            if (!state.hasAnyData) {
                EmptyState(
                    icon = Icons.Rounded.Insights,
                    title = stringResource(R.string.insights_empty_title),
                    message = stringResource(R.string.insights_empty_message),
                )
            } else {
                ScoreTrendCard(state)
                DistractingTrendCard(state)
                WorstHourCard(state)
                BypassCard(state)
            }
            Text(
                text = stringResource(R.string.insights_self_comparison),
                style = MaterialTheme.typography.bodySmall,
                color = FocunoTheme.colors.textTertiary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

private val shortDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)

@Composable
private fun ScoreTrendCard(state: InsightsUiState) {
    GlassCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.insights_score_trend))
        Spacer(Modifier.height(4.dp))
        Text(
            text = state.averageScore?.let { stringResource(R.string.insights_average_score, it) } ?: stringResource(R.string.insights_no_scores),
            style = MaterialTheme.typography.bodyMedium,
            color = FocunoTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(12.dp))
        LineChart(
            values = state.points.map { it.score?.toFloat() },
            color = MaterialTheme.colorScheme.secondary,
            startLabel = state.points.firstOrNull()?.date?.format(shortDate).orEmpty(),
            endLabel = state.points.lastOrNull()?.date?.format(shortDate).orEmpty(),
        )
    }
}

@Composable
private fun DistractingTrendCard(state: InsightsUiState) {
    GlassCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.insights_distracting_trend))
        Spacer(Modifier.height(12.dp))
        BarChart(
            values = state.points.map { it.distractingMs / 60_000f },
            color = FocunoTheme.colors.distracting,
            startLabel = state.points.firstOrNull()?.date?.format(shortDate).orEmpty(),
            endLabel = state.points.lastOrNull()?.date?.format(shortDate).orEmpty(),
        )
    }
}

@Composable
private fun WorstHourCard(state: InsightsUiState) {
    GlassCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.insights_worst_hour))
        Spacer(Modifier.height(4.dp))
        val hour = state.worstHour
        if (hour == null) {
            Text(stringResource(R.string.insights_worst_hour_none), style = MaterialTheme.typography.bodyMedium, color = FocunoTheme.colors.textSecondary)
        } else {
            Text(
                text = stringResource(
                    R.string.insights_worst_hour_value,
                    formatClock(hour * 60),
                    formatClock(((hour + 1) % 24) * 60),
                    durationText(state.hourlyDistractingMs[hour]),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = FocunoTheme.colors.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            BarChart(
                values = state.hourlyDistractingMs.map { it / 60_000f },
                color = FocunoTheme.colors.distracting.copy(alpha = 0.5f),
                highlightIndex = hour,
                highlightColor = FocunoTheme.colors.distracting,
                startLabel = "00",
                endLabel = "23",
            )
        }
    }
}

@Composable
private fun BypassCard(state: InsightsUiState) {
    val bypass = state.bypass
    GlassCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.insights_bypass_title))
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.insights_bypass_explainer), style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.textSecondary)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatTile(stringResource(R.string.insights_bypass_started), bypass.started.toString(), FocunoTheme.colors.textPrimary)
            StatTile(stringResource(R.string.insights_bypass_abandoned), bypass.abandoned.toString(), FocunoTheme.colors.productive)
            StatTile(stringResource(R.string.insights_bypass_granted), bypass.granted.toString(), FocunoTheme.colors.warning)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.insights_blocks_and_sessions, bypass.blockHits, state.focusSessionsCompleted),
            style = MaterialTheme.typography.bodySmall,
            color = FocunoTheme.colors.textSecondary,
        )
    }
}
