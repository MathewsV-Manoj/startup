package com.startup.focuno.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Download
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.data.model.AppSettings
import com.startup.focuno.domain.model.NudgeSensitivity
import com.startup.focuno.ui.components.Panel
import com.startup.focuno.ui.components.SectionTitle
import com.startup.focuno.ui.components.SubScreenTopBar
import com.startup.focuno.ui.theme.FocunoTheme
import java.time.LocalDate

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
    val exportResult by viewModel.exportResult.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(exportResult) {
        val result = exportResult ?: return@LaunchedEffect
        Toast.makeText(context, if (result) R.string.export_done else R.string.export_failed, Toast.LENGTH_SHORT).show()
        viewModel.clearExportResult()
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.export(uri)
    }
    SettingsContent(
        settings = settings,
        onBack = onBack,
        onOpenHealth = onOpenHealth,
        onOpenPrivacy = onOpenPrivacy,
        onOpenUsageCheck = onOpenUsageCheck,
        onGoalChanged = viewModel::setDailyGoalMinutes,
        onStudyGoalChanged = viewModel::setStudyGoalMinutes,
        onNudgesEnabled = viewModel::setNudgesEnabled,
        onSensitivity = viewModel::setNudgeSensitivity,
        onExport = { exportLauncher.launch("focuno-${LocalDate.now()}.csv") },
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
    onStudyGoalChanged: (Int) -> Unit,
    onNudgesEnabled: (Boolean) -> Unit,
    onSensitivity: (NudgeSensitivity) -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        SubScreenTopBar(stringResource(R.string.settings_title), onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GoalCard(
                title = stringResource(R.string.settings_study_goal_title),
                explainer = stringResource(R.string.settings_study_goal_explainer),
                minutes = settings.studyGoalMinutes,
                range = 30f..600f,
                steps = 18,
                onChanged = onStudyGoalChanged,
            )
            GoalCard(
                title = stringResource(R.string.settings_goal_title),
                explainer = stringResource(R.string.settings_goal_explainer),
                minutes = settings.dailyGoalMinutes,
                range = 60f..600f,
                steps = 17,
                onChanged = onGoalChanged,
            )
            NudgeCard(settings, onNudgesEnabled, onSensitivity)
            LinkCard(Icons.Rounded.Shield, stringResource(R.string.health_title), stringResource(R.string.settings_health_subtitle), onOpenHealth)
            LinkCard(Icons.Rounded.Timer, stringResource(R.string.usage_check_title), stringResource(R.string.settings_usage_check_subtitle), onOpenUsageCheck)
            LinkCard(Icons.Rounded.PrivacyTip, stringResource(R.string.privacy_title), stringResource(R.string.settings_privacy_subtitle), onOpenPrivacy)
            LinkCard(Icons.Rounded.Download, stringResource(R.string.export_title), stringResource(R.string.export_subtitle), onExport)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** A goal in hours and minutes, set with a slider in half-hour steps. */
@Composable
private fun GoalCard(
    title: String,
    explainer: String,
    minutes: Int,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChanged: (Int) -> Unit,
) {
    // The thumb follows the finger locally and the goal is saved once, when the finger lifts.
    var dragValue by remember(minutes) { mutableFloatStateOf(minutes.toFloat()) }
    Panel(Modifier.fillMaxWidth()) {
        SectionTitle(title)
        Spacer(Modifier.height(4.dp))
        Text(explainer, style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.textSecondary)
        Spacer(Modifier.height(12.dp))
        val value = dragValue.toInt()
        Text(
            stringResource(R.string.settings_goal_value, value / 60, value % 60),
            style = MaterialTheme.typography.titleLarge,
            color = FocunoTheme.colors.productive,
        )
        Slider(
            value = dragValue,
            onValueChange = { dragValue = it },
            onValueChangeFinished = { onChanged(dragValue.toInt()) },
            valueRange = range,
            steps = steps,
        )
    }
}

@Composable
private fun NudgeCard(settings: AppSettings, onEnabled: (Boolean) -> Unit, onSensitivity: (NudgeSensitivity) -> Unit) {
    Panel(Modifier.fillMaxWidth()) {
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
    Panel(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.textSecondary)
            }
        }
    }
}
