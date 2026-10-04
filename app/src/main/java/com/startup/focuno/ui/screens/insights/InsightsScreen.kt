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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.ui.components.BarChart
import com.startup.focuno.ui.components.EmptyState
import com.startup.focuno.ui.components.LineChart
import com.startup.focuno.ui.components.LoadingState
import com.startup.focuno.ui.components.Panel
import com.startup.focuno.ui.components.RefreshWhileResumed
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
    if (state.isLoading) {
        LoadingState(modifier)
        return
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
            EmptyState(icon = Icons.Rounded.Insights, title = stringResource(R.string.insights_empty_title), message = "")
        } else {
            ScoreTrendCard(state)
            DistractingTrendCard(state)
            WorstHourCard(state)
            UrgesCard(state)
        }
        Spacer(Modifier.height(12.dp))
    }
}

private val shortDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)

/** A card title with an optional big value on the right. */
@Composable
private fun CardHeader(title: String, value: String?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionTitle(title, Modifier.weight(1f))
        if (value != null) Text(value, style = MaterialTheme.typography.titleLarge, color = FocunoTheme.colors.textPrimary)
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ScoreTrendCard(state: InsightsUiState) {
    Panel(Modifier.fillMaxWidth()) {
        CardHeader(stringResource(R.string.insights_score_trend), state.averageScore?.let { stringResource(R.string.insights_average_score, it) })
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
    Panel(Modifier.fillMaxWidth()) {
        CardHeader(stringResource(R.string.insights_distracting_trend), null)
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
    val hour = state.worstHour ?: return
    Panel(Modifier.fillMaxWidth()) {
        CardHeader(stringResource(R.string.insights_worst_hour), "${formatClock(hour * 60)}–${formatClock(((hour + 1) % 24) * 60)}")
        BarChart(
            values = state.hourlyDistractingMs.map { it / 60_000f },
            color = FocunoTheme.colors.distracting.copy(alpha = 0.5f),
            highlightIndex = hour,
            highlightColor = FocunoTheme.colors.distracting,
            startLabel = "00",
            endLabel = "23",
        )
        Spacer(Modifier.height(8.dp))
        Text(durationText(state.hourlyDistractingMs[hour]), style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.textSecondary)
    }
}

@Composable
private fun UrgesCard(state: InsightsUiState) {
    val bypass = state.bypass
    Panel(Modifier.fillMaxWidth()) {
        CardHeader(stringResource(R.string.insights_bypass_title), null)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatTile(stringResource(R.string.insights_bypass_abandoned), bypass.abandoned.toString(), FocunoTheme.colors.productive)
            StatTile(stringResource(R.string.insights_bypass_granted), bypass.granted.toString(), FocunoTheme.colors.warning)
            StatTile(stringResource(R.string.insights_sessions), state.focusSessionsCompleted.toString(), MaterialTheme.colorScheme.secondary)
        }
    }
}
