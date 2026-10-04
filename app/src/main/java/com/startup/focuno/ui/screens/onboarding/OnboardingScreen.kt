package com.startup.focuno.ui.screens.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.data.repository.ProtectionIssue
import com.startup.focuno.ui.components.AppIcon
import com.startup.focuno.ui.components.Panel
import com.startup.focuno.ui.components.LoadingState
import com.startup.focuno.ui.components.OemAutostartCard
import com.startup.focuno.ui.components.RefreshWhileResumed
import com.startup.focuno.ui.components.RestrictedSettingCard
import com.startup.focuno.ui.components.formatClock
import com.startup.focuno.ui.components.rememberPermissionFixer
import com.startup.focuno.ui.theme.DeepVoidPurple
import com.startup.focuno.ui.theme.FocunoTheme

@Composable
fun OnboardingScreen(modifier: Modifier = Modifier, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The person leaves for Android settings and comes back, so keep the permission status fresh.
    RefreshWhileResumed(intervalMs = 1_500L, onRefresh = viewModel::refreshStatus)
    val fix = rememberPermissionFixer()

    Box(modifier.fillMaxSize().background(DeepVoidPurple).safeDrawingPadding()) {
        if (state.isLoading) {
            LoadingState()
        } else {
            Column(Modifier.fillMaxSize()) {
                ProgressDots(state.stepNumber, state.stepCount)
                Box(Modifier.weight(1f)) {
                    Crossfade(targetState = state.step, label = "onboarding-step") { step ->
                        StepContent(step, state, viewModel, fix)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressDots(current: Int, total: Int) {
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (index in 1..total) {
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (index == current) 10.dp else 7.dp)
                    .background(if (index <= current) MaterialTheme.colorScheme.primary else FocunoTheme.colors.trackInactive, CircleShape),
            )
        }
    }
}

@Composable
private fun StepContent(
    step: OnboardingStep,
    state: OnboardingUiState,
    vm: OnboardingViewModel,
    fix: (ProtectionIssue) -> Unit,
) {
    val status = state.status
    when (step) {
        OnboardingStep.PURPOSE -> StepFrame(
            title = stringResource(R.string.onb_purpose_title),
            body = stringResource(R.string.onb_purpose_body),
            primaryLabel = stringResource(R.string.onb_purpose_cta),
            onPrimary = vm::next,
        ) {
            Bullet(stringResource(R.string.onb_purpose_point1))
            Bullet(stringResource(R.string.onb_purpose_point2))
            Bullet(stringResource(R.string.onb_purpose_point3))
        }

        OnboardingStep.USAGE_ACCESS -> PermissionStep(
            title = stringResource(R.string.onb_usage_title),
            body = stringResource(R.string.onb_usage_body),
            granted = status?.usageAccess == true,
            grantLabel = stringResource(R.string.onb_open_settings),
            onGrant = { fix(ProtectionIssue.USAGE_ACCESS) },
            onNext = vm::next,
            onBack = vm::back,
        )

        OnboardingStep.ACCESSIBILITY -> PermissionStep(
            title = stringResource(R.string.onb_accessibility_title),
            body = stringResource(R.string.onb_accessibility_body),
            granted = status?.accessibilityEnabled == true,
            grantLabel = stringResource(R.string.onb_open_settings),
            onGrant = { fix(ProtectionIssue.ACCESSIBILITY) },
            onNext = vm::next,
            onBack = vm::back,
        ) {
            if (state.showRestrictedHelp) RestrictedSettingCard()
            if (state.accessibilityEnabledButIdle) {
                Text(stringResource(R.string.health_accessibility_idle), style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.warning)
            }
        }

        OnboardingStep.OVERLAY -> PermissionStep(
            title = stringResource(R.string.onb_overlay_title),
            body = stringResource(R.string.onb_overlay_body),
            granted = status?.overlayGranted == true,
            grantLabel = stringResource(R.string.onb_open_settings),
            onGrant = { fix(ProtectionIssue.OVERLAY) },
            onNext = vm::next,
            onBack = vm::back,
        )

        OnboardingStep.STAY_ALIVE -> StepFrame(
            title = stringResource(R.string.onb_stay_alive_title),
            body = stringResource(R.string.onb_stay_alive_body),
            primaryLabel = stringResource(R.string.action_continue),
            onPrimary = vm::next,
            onBack = vm::back,
        ) {
            StatusRow(
                label = stringResource(R.string.issue_notifications),
                granted = status?.notificationsGranted == true,
                actionLabel = stringResource(R.string.onb_allow),
                onAction = { fix(ProtectionIssue.NOTIFICATIONS) },
            )
            StatusRow(
                label = stringResource(R.string.issue_battery),
                granted = status?.batteryExempt == true,
                actionLabel = stringResource(R.string.onb_allow),
                onAction = { fix(ProtectionIssue.BATTERY) },
            )
        }

        OnboardingStep.AUTOSTART -> StepFrame(
            title = stringResource(R.string.onb_autostart_title),
            body = stringResource(R.string.onb_autostart_body),
            primaryLabel = stringResource(R.string.onb_autostart_done),
            onPrimary = vm::next,
            secondaryLabel = stringResource(R.string.onb_skip),
            onSecondary = vm::next,
            onBack = vm::back,
        ) {
            OemAutostartCard()
        }

        OnboardingStep.PICK_APPS -> PickAppsStep(state, vm)
        OnboardingStep.FIRST_SCHEDULE -> FirstScheduleStep(state, vm)
    }
}

@Composable
private fun StepFrame(
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = FocunoTheme.colors.textPrimary)
            Text(body, style = MaterialTheme.typography.bodyLarge, color = FocunoTheme.colors.textSecondary)
            content()
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = onPrimary, enabled = primaryEnabled, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(primaryLabel) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (onBack != null) TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) } else Spacer(Modifier.size(1.dp))
                if (secondaryLabel != null && onSecondary != null) TextButton(onClick = onSecondary) { Text(secondaryLabel) }
            }
        }
    }
}

@Composable
private fun PermissionStep(
    title: String,
    body: String,
    granted: Boolean,
    grantLabel: String,
    onGrant: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    extra: @Composable () -> Unit = {},
) {
    StepFrame(
        title = title,
        body = body,
        primaryLabel = if (granted) stringResource(R.string.action_continue) else grantLabel,
        onPrimary = if (granted) onNext else onGrant,
        secondaryLabel = if (granted) null else stringResource(R.string.onb_skip),
        onSecondary = if (granted) null else onNext,
        onBack = onBack,
    ) {
        StatusPill(granted)
        extra()
    }
}

@Composable
private fun StatusPill(granted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = if (granted) FocunoTheme.colors.productive else FocunoTheme.colors.trackInactive,
        )
        Text(
            stringResource(if (granted) R.string.onb_status_on else R.string.onb_status_off),
            style = MaterialTheme.typography.titleSmall,
            color = if (granted) FocunoTheme.colors.productive else FocunoTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun StatusRow(label: String, granted: Boolean, actionLabel: String, onAction: () -> Unit) {
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.textPrimary)
                StatusPill(granted)
            }
            if (!granted) OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 8.dp).size(6.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = FocunoTheme.colors.textPrimary)
    }
}

@Composable
private fun PickAppsStep(state: OnboardingUiState, vm: OnboardingViewModel) {
    StepFrame(
        title = stringResource(R.string.onb_pick_title),
        body = stringResource(R.string.onb_pick_body),
        primaryLabel = stringResource(R.string.action_continue),
        onPrimary = vm::confirmApps,
        onBack = vm::back,
        scrollable = false,
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.editor_search_apps)) },
        )
        if (state.appsLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp)) { LoadingState() }
        } else {
            LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(state.visibleApps, key = { it.packageName }) { app ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.toggleApp(app.packageName) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        AppIcon(app.packageName, size = 40.dp)
                        Text(app.label, style = MaterialTheme.typography.bodyLarge, color = FocunoTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                        Switch(checked = app.packageName in state.selected, onCheckedChange = { vm.toggleApp(app.packageName) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FirstScheduleStep(state: OnboardingUiState, vm: OnboardingViewModel) {
    val count = state.selected.size
    StepFrame(
        title = stringResource(R.string.onb_schedule_title),
        body = stringResource(R.string.onb_schedule_body),
        primaryLabel = stringResource(R.string.onb_schedule_create),
        onPrimary = { vm.finish(createSchedule = true) },
        primaryEnabled = count > 0 && !state.isFinishing,
        secondaryLabel = stringResource(R.string.onb_schedule_skip),
        onSecondary = { vm.finish(createSchedule = false) },
        onBack = vm::back,
    ) {
        SchedulePreset.entries.forEach { preset ->
            FilterChip(
                selected = preset == state.preset,
                onClick = { vm.setPreset(preset) },
                label = {
                    Text(
                        stringResource(
                            when (preset) {
                                SchedulePreset.NIGHT -> R.string.preset_night
                                SchedulePreset.STUDY_DAY -> R.string.preset_study_day
                                SchedulePreset.EVENING -> R.string.preset_evening
                            },
                            formatClock(preset.startMinute),
                            formatClock(preset.endMinute),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = if (count > 0) pluralStringResource(R.plurals.onb_schedule_summary, count, count) else stringResource(R.string.onb_schedule_none_selected),
            style = MaterialTheme.typography.bodyMedium,
            color = FocunoTheme.colors.textSecondary,
        )
        Text(stringResource(R.string.onb_schedule_honest), style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.textTertiary)
    }
}
