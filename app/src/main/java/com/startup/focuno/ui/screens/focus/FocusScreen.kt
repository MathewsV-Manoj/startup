package com.startup.focuno.ui.screens.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.domain.model.DayStats
import com.startup.focuno.ui.components.AppIcon
import com.startup.focuno.ui.components.EmptyState
import com.startup.focuno.ui.components.ErrorState
import com.startup.focuno.ui.components.FocusGauge
import com.startup.focuno.ui.components.GlassCard
import com.startup.focuno.ui.components.LoadingState
import com.startup.focuno.ui.components.ProtectionBanner
import com.startup.focuno.ui.components.RefreshWhileResumed
import com.startup.focuno.ui.components.ScreenTitle
import com.startup.focuno.ui.components.SectionTitle
import com.startup.focuno.ui.components.StatTile
import com.startup.focuno.ui.components.durationText
import com.startup.focuno.ui.theme.FocunoTheme
import kotlinx.coroutines.delay
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun FocusScreen(
    onOpenSettings: () -> Unit,
    onOpenHealth: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshWhileResumed(intervalMs = 30_000L, onRefresh = viewModel::refresh)
    FocusContent(
        state = state,
        onOpenSettings = onOpenSettings,
        onOpenHealth = onOpenHealth,
        onRetry = viewModel::refresh,
        onStartSession = viewModel::startFocusSession,
        onStopSession = viewModel::stopFocusSession,
        modifier = modifier,
    )
}

@Composable
fun FocusContent(
    state: FocusUiState,
    onOpenSettings: () -> Unit,
    onOpenHealth: () -> Unit,
    onRetry: () -> Unit,
    onStartSession: (Int) -> Unit,
    onStopSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        ScreenTitle(stringResource(R.string.tab_focus)) {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings_title), tint = FocunoTheme.colors.textSecondary)
            }
        }
        when {
            state.isLoading -> LoadingState()
            state.hasError && state.today == null && state.hasUsageAccess ->
                ErrorState(stringResource(R.string.error_generic), onRetry)
            else -> Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProtectionBanner(state.protection, onFix = onOpenHealth)
                GaugeCard(state, onOpenHealth)
                if (state.hasUsageAccess) {
                    TodayCard(state.today)
                    FocusSessionCard(state.sessionEndsAtMs, onStartSession, onStopSession)
                    StreakCard(state)
                    TopDistractionsCard(state.topDistractions)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GaugeCard(state: FocusUiState, onOpenHealth: () -> Unit) {
    val score = state.today?.focusScore
    GlassCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 24.dp, horizontal = 20.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            FocusGauge(score = score)
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(scoreMessage(score, state.hasUsageAccess)),
                style = MaterialTheme.typography.bodyLarge,
                color = FocunoTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            if (!state.hasUsageAccess) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onOpenHealth) { Text(stringResource(R.string.focus_allow_usage_access)) }
            }
        }
    }
}

private fun scoreMessage(score: Int?, hasAccess: Boolean): Int = when {
    !hasAccess -> R.string.score_msg_no_access
    score == null -> R.string.score_msg_no_data
    score >= 80 -> R.string.score_msg_great
    score >= 60 -> R.string.score_msg_good
    score >= 40 -> R.string.score_msg_scattered
    else -> R.string.score_msg_rough
}

@Composable
private fun TodayCard(today: DayStats?) {
    GlassCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.focus_today))
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatTile(stringResource(R.string.focus_total), today?.let { durationText(it.totalMs) } ?: stringResource(R.string.gauge_dash), FocunoTheme.colors.textPrimary)
            StatTile(stringResource(R.string.focus_productive), today?.let { durationText(it.productiveMs) } ?: stringResource(R.string.gauge_dash), FocunoTheme.colors.productive)
            StatTile(stringResource(R.string.focus_distracting), today?.let { durationText(it.distractingMs) } ?: stringResource(R.string.gauge_dash), FocunoTheme.colors.distracting)
        }
        if (today != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = pluralStringResource(R.plurals.focus_pickups, today.pickupCount, today.pickupCount),
                style = MaterialTheme.typography.bodyMedium,
                color = FocunoTheme.colors.textSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FocusSessionCard(endsAtMs: Long?, onStart: (Int) -> Unit, onStop: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            SectionTitle(stringResource(R.string.session_title))
        }
        Spacer(Modifier.height(8.dp))
        if (endsAtMs == null) {
            Text(stringResource(R.string.session_explainer), style = MaterialTheme.typography.bodyMedium, color = FocunoTheme.colors.textSecondary)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(25, 45, 60).forEach { minutes ->
                    AssistChip(onClick = { onStart(minutes) }, label = { Text(stringResource(R.string.session_minutes_chip, minutes)) })
                }
            }
        } else {
            val now by produceState(initialValue = System.currentTimeMillis()) {
                while (true) {
                    delay(1_000)
                    value = System.currentTimeMillis()
                }
            }
            val remaining = (endsAtMs - now).coerceAtLeast(0)
            val minutes = (remaining / 60_000).toInt()
            val seconds = ((remaining / 1_000) % 60).toInt()
            Text(
                text = stringResource(R.string.session_remaining, minutes, seconds),
                style = MaterialTheme.typography.headlineMedium,
                color = FocunoTheme.colors.productive,
            )
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.session_running_note), style = MaterialTheme.typography.bodyMedium, color = FocunoTheme.colors.textSecondary)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onStop) { Text(stringResource(R.string.session_end_early)) }
        }
    }
}

@Composable
private fun StreakCard(state: FocusUiState) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = FocunoTheme.colors.warning)
            Column {
                SectionTitle(pluralStringResource(R.plurals.streak_days, state.streak, state.streak))
                Text(
                    text = stringResource(R.string.streak_explainer, state.goalMinutes / 60, state.goalMinutes % 60),
                    style = MaterialTheme.typography.bodySmall,
                    color = FocunoTheme.colors.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            state.week.forEach { day ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val color = when (day.outcome) {
                        DayOutcome.MET_GOAL -> FocunoTheme.colors.productive
                        DayOutcome.OVER_GOAL -> FocunoTheme.colors.warning
                        DayOutcome.NO_DATA, DayOutcome.TODAY -> FocunoTheme.colors.trackInactive
                    }
                    Box(
                        Modifier
                            .size(28.dp)
                            .background(color, CircleShape),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (day.outcome == DayOutcome.TODAY) FocunoTheme.colors.textPrimary else FocunoTheme.colors.textTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopDistractionsCard(items: List<TopDistraction>) {
    GlassCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.focus_top_distractions))
        Spacer(Modifier.height(12.dp))
        if (items.isEmpty()) {
            Text(stringResource(R.string.focus_no_distractions), style = MaterialTheme.typography.bodyMedium, color = FocunoTheme.colors.textSecondary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppIcon(item.usage.packageName, size = 40.dp)
                        Column(Modifier.weight(1f)) {
                            Text(item.label, style = MaterialTheme.typography.titleSmall, color = FocunoTheme.colors.textPrimary)
                            Text(
                                text = pluralStringResource(R.plurals.opens_count, item.usage.openCount, item.usage.openCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = FocunoTheme.colors.textSecondary,
                            )
                        }
                        Text(durationText(item.usage.foregroundMs), style = MaterialTheme.typography.titleSmall, color = FocunoTheme.colors.distracting)
                    }
                }
            }
        }
    }
}
