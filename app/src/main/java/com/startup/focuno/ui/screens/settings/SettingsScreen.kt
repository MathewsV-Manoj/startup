package com.startup.focuno.ui.screens.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.data.model.AppSettings
import com.startup.focuno.domain.model.NudgeSensitivity
import com.startup.focuno.ui.components.GlassCard
import com.startup.focuno.ui.components.SectionTitle
import com.startup.focuno.ui.components.SubScreenTopBar
import com.startup.focuno.ui.theme.FocunoTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenHealth: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenUsageCheck: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    SettingsContent(
        settings = settings,
        onBack = onBack,
        onOpenHealth = onOpenHealth,
        onOpenPrivacy = onOpenPrivacy,
        onOpenUsageCheck = onOpenUsageCheck,
        onGoalChanged = viewModel::setDailyGoalMinutes,
        onNudgesEnabled = viewModel::setNudgesEnabled,
        onSensitivity = viewModel::setNudgeSensitivity,
        modifier = modifier,
    )
}

@Composable
fun SettingsContent(
    settings: AppSettings,
    onBack: () -> Unit,
    onOpenHealth: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenUsageCheck: () -> Unit,
    onGoalChanged: (Int) -> Unit,
    onNudgesEnabled: (Boolean) -> Unit,
    onSensitivity: (NudgeSensitivity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        SubScreenTopBar(stringResource(R.string.settings_title), onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GoalCard(settings.dailyGoalMinutes, onGoalChanged)
            NudgeCard(settings, onNudgesEnabled, onSensitivity)
            LinkCard(Icons.Rounded.Shield, stringResource(R.string.health_title), stringResource(R.string.settings_health_subtitle), onOpenHealth)
            LinkCard(Icons.Rounded.Timer, stringResource(R.string.usage_check_title), stringResource(R.string.settings_usage_check_subtitle), onOpenUsageCheck)
            LinkCard(Icons.Rounded.PrivacyTip, stringResource(R.string.privacy_title), stringResource(R.string.settings_privacy_subtitle), onOpenPrivacy)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun GoalCard(goalMinutes: Int, onChanged: (Int) -> Unit) {
    // The thumb follows the finger locally and the goal is saved once, when the finger lifts.
    var dragValue by remember(goalMinutes) { mutableFloatStateOf(goalMinutes.toFloat()) }
    GlassCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.settings_goal_title))
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.settings_goal_explainer), style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.textSecondary)
        Spacer(Modifier.height(12.dp))
        val minutes = dragValue.toInt()
        Text(
            stringResource(R.string.settings_goal_value, minutes / 60, minutes % 60),
            style = MaterialTheme.typography.titleLarge,
            color = FocunoTheme.colors.productive,
        )
        Slider(
            value = dragValue,
            onValueChange = { dragValue = it },
            onValueChangeFinished = { onChanged(dragValue.toInt()) },
            valueRange = 60f..600f,
            steps = 17,
        )
    }
}

@Composable
private fun NudgeCard(settings: AppSettings, onEnabled: (Boolean) -> Unit, onSensitivity: (NudgeSensitivity) -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionTitle(stringResource(R.string.settings_nudges_title))
                Text(stringResource(R.string.settings_nudges_explainer), style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.textSecondary)
            }
            Switch(checked = settings.nudgesEnabled, onCheckedChange = onEnabled)
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.settings_sensitivity), style = MaterialTheme.typography.titleSmall, color = FocunoTheme.colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NudgeSensitivity.entries.forEach { option ->
                FilterChip(
                    selected = option == settings.nudgeSensitivity,
                    onClick = { onSensitivity(option) },
                    enabled = settings.nudgesEnabled,
                    label = {
                        Text(
                            stringResource(
                                when (option) {
                                    NudgeSensitivity.LOW -> R.string.sensitivity_low
                                    NudgeSensitivity.MEDIUM -> R.string.sensitivity_medium
                                    NudgeSensitivity.HIGH -> R.string.sensitivity_high
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun LinkCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.textSecondary)
            }
        }
    }
}
