package com.startup.focuno.ui.screens.trophies

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.domain.model.BadgeId
import com.startup.focuno.domain.usecase.GameRules
import com.startup.focuno.ui.components.BuddyMood
import com.startup.focuno.ui.components.BuddyView
import com.startup.focuno.ui.components.GlassCard
import com.startup.focuno.ui.components.LoadingState
import com.startup.focuno.ui.components.ScreenTitle
import com.startup.focuno.ui.components.SectionTitle
import com.startup.focuno.ui.theme.FocunoTheme

@Composable
fun TrophiesScreen(modifier: Modifier = Modifier, viewModel: GameViewModel = hiltViewModel()) {
    val game by viewModel.state.collectAsStateWithLifecycle()
    TrophiesContent(game, modifier)
}

@Composable
fun TrophiesContent(game: GameUiState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        ScreenTitle(stringResource(R.string.tab_trophies))
        if (game.isLoading) {
            LoadingState()
            return@Column
        }
        val titles = stringArrayResource(R.array.level_titles)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        BuddyView(mood = BuddyMood.HAPPY, level = game.level.level, size = 96.dp)
                        Column {
                            Text(
                                stringResource(R.string.level_line, game.level.level, titles[game.level.titleIndex.coerceIn(0, titles.lastIndex)]),
                                style = MaterialTheme.typography.titleLarge,
                                color = FocunoTheme.colors.textPrimary,
                            )
                            Text(
                                stringResource(R.string.trophies_total_points, game.totalXp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = FocunoTheme.colors.textSecondary,
                            )
                            Text(
                                stringResource(R.string.trophies_next_level, game.level.xpForNextLevel - game.level.xpIntoLevel),
                                style = MaterialTheme.typography.bodySmall,
                                color = FocunoTheme.colors.warning,
                            )
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                GlassCard(Modifier.fillMaxWidth()) {
                    SectionTitle(stringResource(R.string.earn_title))
                    Spacer(Modifier.height(8.dp))
                    EarnLine(R.string.earn_session)
                    EarnLine(R.string.earn_urge)
                    EarnLine(R.string.earn_day)
                    EarnLine(R.string.earn_never_down)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle(stringResource(R.string.badges_title))
            }
            items(game.badges, key = { it.id.name }) { badge -> BadgeTile(badge) }
        }
    }
}

@Composable
private fun EarnLine(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = FocunoTheme.colors.textSecondary,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

@Composable
private fun BadgeTile(badge: BadgeUi) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(if (badge.unlocked) badgeColor(badge.id) else FocunoTheme.colors.trackInactive, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (badge.unlocked) badgeIcon(badge.id) else Icons.Rounded.Lock,
                contentDescription = null,
                tint = if (badge.unlocked) FocunoTheme.colors.trackInactive else FocunoTheme.colors.textTertiary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(badgeName(badge.id)),
            style = MaterialTheme.typography.labelLarge,
            color = if (badge.unlocked) FocunoTheme.colors.textPrimary else FocunoTheme.colors.textTertiary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(badgeHint(badge.id)),
            style = MaterialTheme.typography.labelSmall,
            color = FocunoTheme.colors.textTertiary,
            textAlign = TextAlign.Center,
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
