package com.startup.focuno.ui.screens.trophies

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.domain.model.BadgeId
import com.startup.focuno.domain.model.Quest
import com.startup.focuno.domain.model.QuestKind
import com.startup.focuno.ui.components.BuddyMood
import com.startup.focuno.ui.components.BuddyView
import com.startup.focuno.ui.components.LoadingState
import com.startup.focuno.ui.components.Panel
import com.startup.focuno.ui.components.ScreenTitle
import com.startup.focuno.ui.components.SectionTitle
import com.startup.focuno.ui.components.durationText
import com.startup.focuno.ui.theme.FocunoTheme

@Composable
fun TrophiesScreen(modifier: Modifier = Modifier, viewModel: GameViewModel = hiltViewModel()) {
    val game by viewModel.state.collectAsStateWithLifecycle()
    TrophiesContent(game, modifier)
}

@Composable
fun TrophiesContent(game: GameUiState, modifier: Modifier = Modifier) {
    var openBadge by rememberSaveable { mutableStateOf<BadgeId?>(null) }
    Column(modifier.fillMaxSize()) {
        ScreenTitle(stringResource(R.string.tab_me))
        if (game.isLoading) {
            LoadingState()
            return@Column
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { LevelHeader(game) }
            item(span = { GridItemSpan(maxLineSpan) }) { NumbersRow(game) }
            if (game.quests.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { Missions(game.quests) }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(stringResource(R.string.badges_title)) }
            items(game.badges, key = { it.id.name }) { badge -> BadgeTile(badge, onClick = { openBadge = badge.id }) }
        }
    }
    openBadge?.let { id ->
        AlertDialog(
            onDismissRequest = { openBadge = null },
            icon = { Icon(badgeIcon(id), contentDescription = null) },
            title = { Text(stringResource(badgeName(id))) },
            text = { Text(stringResource(badgeHint(id))) },
            confirmButton = { TextButton(onClick = { openBadge = null }) { Text(stringResource(R.string.action_ok)) } },
        )
    }
}

@Composable
private fun LevelHeader(game: GameUiState) {
    val titles = stringArrayResource(R.array.level_titles)
    val level = game.level
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BuddyView(mood = BuddyMood.HAPPY, level = level.level, size = 128.dp, description = stringResource(R.string.buddy_description))
        Text(stringResource(R.string.level_number, level.level), style = MaterialTheme.typography.headlineMedium, color = FocunoTheme.colors.textPrimary)
        Text(
            titles[level.titleIndex.coerceIn(0, titles.lastIndex)],
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { level.fraction },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
            color = FocunoTheme.colors.productive,
            trackColor = FocunoTheme.colors.trackInactive,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.xp_progress, level.xpIntoLevel, level.xpForNextLevel),
            style = MaterialTheme.typography.labelMedium,
            color = FocunoTheme.colors.textTertiary,
        )
    }
}

@Composable
private fun NumbersRow(game: GameUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NumberTile(
            icon = Icons.Rounded.LocalFireDepartment,
            tint = FocunoTheme.colors.warning,
            value = game.streak.toString(),
            label = stringResource(R.string.streak_label),
            modifier = Modifier.weight(1f),
        )
        NumberTile(
            icon = Icons.Rounded.Star,
            tint = MaterialTheme.colorScheme.secondary,
            value = game.totalXp.toString(),
            label = stringResource(R.string.points_label),
            modifier = Modifier.weight(1f),
        )
        NumberTile(
            icon = Icons.Rounded.Bolt,
            tint = FocunoTheme.colors.productive,
            value = stringResource(R.string.quest_reward, game.todayActionXp),
            label = stringResource(R.string.day_today),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NumberTile(icon: ImageVector, tint: Color, value: String, label: String, modifier: Modifier = Modifier) {
    Panel(modifier, contentPadding = PaddingValues(vertical = 14.dp, horizontal = 8.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = FocunoTheme.colors.textPrimary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = FocunoTheme.colors.textTertiary)
        }
    }
}

@Composable
private fun Missions(quests: List<Quest>) {
    Panel(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.quests_title))
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            quests.forEach { MissionRow(it) }
        }
    }
}

@Composable
private fun MissionRow(quest: Quest) {
    val title = stringResource(
        when (quest.kind) {
            QuestKind.FOCUS_SESSION -> R.string.quest_focus_title
            QuestKind.STAY_UNDER_GOAL -> R.string.quest_goal_title
            QuestKind.RESIST_URGE -> R.string.quest_urge_title
        },
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(if (quest.done) FocunoTheme.colors.productive else Color.Transparent, CircleShape)
                .border(2.dp, if (quest.done) FocunoTheme.colors.productive else FocunoTheme.colors.textTertiary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (quest.done) Icon(Icons.Rounded.Check, contentDescription = null, tint = FocunoTheme.colors.trackInactive, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = FocunoTheme.colors.textPrimary)
            if (quest.kind == QuestKind.STAY_UNDER_GOAL) {
                Text(
                    if (quest.overGoal) stringResource(R.string.quest_goal_over) else stringResource(R.string.quest_goal_ok, durationText(quest.minutesLeft * 60_000L)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (quest.overGoal) FocunoTheme.colors.warning else FocunoTheme.colors.textTertiary,
                )
            }
        }
        Text(stringResource(R.string.quest_reward, quest.rewardXp), style = MaterialTheme.typography.titleSmall, color = FocunoTheme.colors.warning)
    }
}

@Composable
private fun BadgeTile(badge: BadgeUi, onClick: () -> Unit) {
    Column(
        Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(if (badge.unlocked) badgeColor(badge.id) else FocunoTheme.colors.surfaceElevated, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (badge.unlocked) badgeIcon(badge.id) else Icons.Rounded.Lock,
                contentDescription = null,
                tint = if (badge.unlocked) FocunoTheme.colors.trackInactive else FocunoTheme.colors.textTertiary,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(badgeName(badge.id)),
            style = MaterialTheme.typography.labelMedium,
            color = if (badge.unlocked) FocunoTheme.colors.textPrimary else FocunoTheme.colors.textTertiary,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

private fun badgeIcon(id: BadgeId): ImageVector = when (id) {
    BadgeId.FIRST_STEPS -> Icons.Rounded.Flag
    BadgeId.FOCUS_STARTER, BadgeId.FOCUS_PRO -> Icons.Rounded.Timer
    BadgeId.STREAK_3, BadgeId.STREAK_7, BadgeId.STREAK_30 -> Icons.Rounded.LocalFireDepartment
    BadgeId.URGE_FIGHTER, BadgeId.URGE_MASTER -> Icons.Rounded.Bolt
    BadgeId.SHIELD_UP -> Icons.Rounded.Shield
    BadgeId.RISING_STAR -> Icons.Rounded.Star
    BadgeId.LEGEND -> Icons.Rounded.EmojiEvents
}

@Composable
private fun badgeColor(id: BadgeId) = when (id) {
    BadgeId.STREAK_3, BadgeId.STREAK_7, BadgeId.STREAK_30 -> FocunoTheme.colors.warning
    BadgeId.URGE_FIGHTER, BadgeId.URGE_MASTER -> FocunoTheme.colors.distracting
    BadgeId.FOCUS_STARTER, BadgeId.FOCUS_PRO, BadgeId.SHIELD_UP -> FocunoTheme.colors.productive
    BadgeId.FIRST_STEPS, BadgeId.RISING_STAR, BadgeId.LEGEND -> MaterialTheme.colorScheme.secondary
}

fun badgeName(id: BadgeId): Int = when (id) {
    BadgeId.FIRST_STEPS -> R.string.badge_first_steps
    BadgeId.FOCUS_STARTER -> R.string.badge_focus_starter
    BadgeId.FOCUS_PRO -> R.string.badge_focus_pro
    BadgeId.STREAK_3 -> R.string.badge_streak_3
    BadgeId.STREAK_7 -> R.string.badge_streak_7
    BadgeId.STREAK_30 -> R.string.badge_streak_30
    BadgeId.URGE_FIGHTER -> R.string.badge_urge_fighter
    BadgeId.URGE_MASTER -> R.string.badge_urge_master
    BadgeId.SHIELD_UP -> R.string.badge_shield_up
    BadgeId.RISING_STAR -> R.string.badge_rising_star
    BadgeId.LEGEND -> R.string.badge_legend
}

fun badgeHint(id: BadgeId): Int = when (id) {
    BadgeId.FIRST_STEPS -> R.string.badge_first_steps_hint
    BadgeId.FOCUS_STARTER -> R.string.badge_focus_starter_hint
    BadgeId.FOCUS_PRO -> R.string.badge_focus_pro_hint
    BadgeId.STREAK_3 -> R.string.badge_streak_3_hint
    BadgeId.STREAK_7 -> R.string.badge_streak_7_hint
    BadgeId.STREAK_30 -> R.string.badge_streak_30_hint
    BadgeId.URGE_FIGHTER -> R.string.badge_urge_fighter_hint
    BadgeId.URGE_MASTER -> R.string.badge_urge_master_hint
    BadgeId.SHIELD_UP -> R.string.badge_shield_up_hint
    BadgeId.RISING_STAR -> R.string.badge_rising_star_hint
    BadgeId.LEGEND -> R.string.badge_legend_hint
}
