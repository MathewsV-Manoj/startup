package com.startup.focuno.ui.screens.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.domain.model.Quest
import com.startup.focuno.domain.model.QuestKind
import com.startup.focuno.ui.components.BuddyMood
import com.startup.focuno.ui.components.BuddyView
import com.startup.focuno.ui.components.ErrorState
import com.startup.focuno.ui.components.GlassCard
import com.startup.focuno.ui.components.LoadingState
import com.startup.focuno.ui.components.ProtectionBanner
import com.startup.focuno.ui.components.RefreshWhileResumed
import com.startup.focuno.ui.components.ScreenTitle
import com.startup.focuno.ui.components.SectionTitle
import com.startup.focuno.ui.components.durationText
import com.startup.focuno.ui.screens.trophies.GameUiState
import com.startup.focuno.ui.screens.trophies.GameViewModel
import com.startup.focuno.ui.theme.FocunoTheme
import kotlinx.coroutines.delay
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenHealth: () -> Unit,
    modifier: Modifier = Modifier,
    focusViewModel: FocusViewModel = hiltViewModel(),
    gameViewModel: GameViewModel = hiltViewModel(),
) {
    val focus by focusViewModel.state.collectAsStateWithLifecycle()
    val game by gameViewModel.state.collectAsStateWithLifecycle()
    RefreshWhileResumed(intervalMs = 30_000L, onRefresh = focusViewModel::refresh)
    HomeContent(
        focus = focus,
        game = game,
        onOpenSettings = onOpenSettings,
        onOpenHealth = onOpenHealth,
        onRetry = focusViewModel::refresh,
        onStartSession = focusViewModel::startFocusSession,
        onStopSession = focusViewModel::stopFocusSession,
        modifier = modifier,
    )
}

@Composable
fun HomeContent(
    focus: FocusUiState,
    game: GameUiState,
    onOpenSettings: () -> Unit,
    onOpenHealth: () -> Unit,
    onRetry: () -> Unit,
    onStartSession: (Int) -> Unit,
    onStopSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        ScreenTitle(stringResource(R.string.tab_home)) {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings_title), tint = FocunoTheme.colors.textSecondary)
            }
        }
        when {
            focus.isLoading -> LoadingState()
            focus.hasError && focus.today == null && focus.hasUsageAccess -> ErrorState(stringResource(R.string.error_generic), onRetry)
            else -> Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProtectionBanner(focus.protection, onFix = onOpenHealth)
                BuddyCard(game, focus.today?.focusScore, focus.hasUsageAccess)
                FocusSessionCard(focus.sessionEndsAtMs, onStartSession, onStopSession)
                QuestsCard(game.quests)
                StreakCard(focus)
                if (!focus.hasUsageAccess) {
                    OutlinedButton(onClick = onOpenHealth, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text(stringResource(R.string.focus_allow_usage_access))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BuddyCard(game: GameUiState, score: Int?, hasAccess: Boolean) {
    val mood = when {
        !hasAccess || score == null -> BuddyMood.OKAY
        score >= 70 -> BuddyMood.HAPPY
        score >= 40 -> BuddyMood.OKAY
        else -> BuddyMood.SLEEPY
    }
    val message = when {
        !hasAccess || score == null -> R.string.buddy_msg_new
        score >= 70 -> R.string.buddy_msg_happy
        score >= 40 -> R.string.buddy_msg_okay
        else -> R.string.buddy_msg_sleepy
    }
    val titles = stringArrayResource(R.array.level_titles)
    val level = game.level
    GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            BuddyView(mood = mood, level = level.level, size = 170.dp, description = stringResource(R.string.buddy_description))
            Text(
                text = stringResource(message),
                style = MaterialTheme.typography.bodyLarge,
                color = FocunoTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.level_line, level.level, titles[level.titleIndex.coerceIn(0, titles.lastIndex)]),
                style = MaterialTheme.typography.headlineSmall,
                color = FocunoTheme.colors.textPrimary,
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { level.fraction },
                modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(50)),
                color = FocunoTheme.colors.productive,
                trackColor = FocunoTheme.colors.trackInactive,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.xp_progress, level.xpIntoLevel, level.xpForNextLevel),
                style = MaterialTheme.typography.bodyMedium,
                color = FocunoTheme.colors.textSecondary,
            )
            if (game.todayActionXp > 0) {
                Text(
                    text = stringResource(R.string.xp_today, game.todayActionXp),
                    style = MaterialTheme.typography.labelLarge,
                    color = FocunoTheme.colors.warning,
                )
            }
        }
    }
}

@Composable
private fun FocusSessionCard(endsAtMs: Long?, onStart: (Int) -> Unit, onStop: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        if (endsAtMs == null) {
            Button(onClick = { onStart(25) }, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.home_start_focus), style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.home_start_focus_hint), style = MaterialTheme.typography.bodyMedium, color = FocunoTheme.colors.textSecondary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 45, 60).forEach { minutes ->
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
            Text(
                text = stringResource(R.string.session_remaining, (remaining / 60_000).toInt(), ((remaining / 1_000) % 60).toInt()),
                style = MaterialTheme.typography.displaySmall,
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
private fun QuestsCard(quests: List<Quest>) {
    if (quests.isEmpty()) return
    GlassCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.quests_title))
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            quests.forEach { QuestRow(it) }
        }
    }
}

@Composable
private fun QuestRow(quest: Quest) {
    val title = stringResource(
        when (quest.kind) {
            QuestKind.FOCUS_SESSION -> R.string.quest_focus_title
            QuestKind.STAY_UNDER_GOAL -> R.string.quest_goal_title
            QuestKind.RESIST_URGE -> R.string.quest_urge_title
        },
    )
    val subtitle = when (quest.kind) {
        QuestKind.FOCUS_SESSION -> stringResource(if (quest.done) R.string.quest_done else R.string.quest_focus_todo)
        QuestKind.RESIST_URGE -> stringResource(if (quest.done) R.string.quest_done else R.string.quest_urge_todo)
        QuestKind.STAY_UNDER_GOAL ->
            if (quest.overGoal) stringResource(R.string.quest_goal_over) else stringResource(R.string.quest_goal_ok, durationText(quest.minutesLeft * 60_000L))
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(if (quest.done) FocunoTheme.colors.productive else Color.Transparent, CircleShape)
                .border(2.dp, if (quest.done) FocunoTheme.colors.productive else FocunoTheme.colors.textTertiary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (quest.done) Icon(Icons.Rounded.Check, contentDescription = null, tint = FocunoTheme.colors.trackInactive)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = if (quest.overGoal) FocunoTheme.colors.warning else FocunoTheme.colors.textSecondary)
            if (quest.kind == QuestKind.STAY_UNDER_GOAL) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { quest.progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    color = if (quest.overGoal) FocunoTheme.colors.warning else FocunoTheme.colors.productive,
                    trackColor = FocunoTheme.colors.trackInactive,
                )
            }
        }
        Text(
            text = stringResource(R.string.quest_reward, quest.rewardXp),
            style = MaterialTheme.typography.titleSmall,
            color = FocunoTheme.colors.warning,
        )
    }
}


@Composable
private fun StreakCard(state: FocusUiState) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = FocunoTheme.colors.warning, modifier = Modifier.size(32.dp))
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
                    Box(Modifier.size(30.dp).background(color, CircleShape))
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
