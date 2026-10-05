package com.startup.focuno.ui.screens.block

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.startup.focuno.R
import com.startup.focuno.domain.model.BlockSchedule
import com.startup.focuno.domain.model.BlockScope
import com.startup.focuno.domain.usecase.StrictLock
import com.startup.focuno.ui.components.AppIcon
import com.startup.focuno.ui.components.Panel
import com.startup.focuno.ui.components.StrictSwitch
import com.startup.focuno.ui.components.LoadingState
import com.startup.focuno.ui.components.TimePickerDialog
import com.startup.focuno.ui.components.durationText
import com.startup.focuno.ui.components.formatClock
import com.startup.focuno.ui.components.shortVideoName
import com.startup.focuno.ui.theme.FocunoTheme
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorSheet(
    state: ScheduleEditorUiState,
    onDismiss: () -> Unit,
    onQuery: (String) -> Unit,
    onSelectApp: (String) -> Unit,
    onChooseScope: (BlockScope) -> Unit,
    onChoosePreset: (WhenPreset) -> Unit,
    onChooseCustomTime: () -> Unit,
    onStepBack: () -> Unit,
    onShowTimePicker: (TimeField) -> Unit,
    onHideTimePicker: () -> Unit,
    onSetTime: (TimeField, Int) -> Unit,
    onToggleDay: (Int) -> Unit,
    onSetStrict: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onChooseLimit: () -> Unit,
    onChooseMindful: () -> Unit,
    onSetLimitMinutes: (Int) -> Unit,
    onSetLimitOpens: (Int) -> Unit,
    onSaveLimit: () -> Unit,
    onDeleteLimit: () -> Unit,
) {
    if (!state.isOpen) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FocunoTheme.colors.surfaceElevated,
    ) {
        when (state.step) {
            EditorStep.PICK_APP -> AppPicker(state, onQuery, onSelectApp)
            EditorStep.WHAT -> WhatStep(state, onChooseScope, onStepBack)
            EditorStep.WHEN -> WhenStep(state, onChoosePreset, onChooseCustomTime, onChooseLimit, onChooseMindful, onSetStrict, onStepBack)
            EditorStep.DETAILS -> Details(state, onShowTimePicker, onToggleDay, onSetStrict, onSave, onDelete, onStepBack)
            EditorStep.LIMIT -> LimitStep(state, onSetLimitMinutes, onSetLimitOpens, onSetStrict, onSaveLimit, onDeleteLimit, onStepBack)
        }
    }
    state.timePickerFor?.let { field ->
        TimePickerDialog(
            title = stringResource(if (field == TimeField.START) R.string.editor_start_time else R.string.editor_end_time),
            initialMinuteOfDay = if (field == TimeField.START) state.startMinute else state.endMinute,
            onConfirm = { onSetTime(field, it) },
            onDismiss = onHideTimePicker,
        )
    }
}

@Composable
private fun StepHeader(title: String, packageName: String?, onBack: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        if (packageName != null) AppIcon(packageName, size = 48.dp)
        Text(title, style = MaterialTheme.typography.headlineSmall, color = FocunoTheme.colors.textPrimary, modifier = Modifier.weight(1f))
        if (onBack != null) TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
    }
}

@Composable
private fun ChoiceTile(title: String, subtitle: String, onClick: () -> Unit, enabled: Boolean = true) {
    Panel(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = FocunoTheme.colors.textPrimary, modifier = Modifier.weight(1f))
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = FocunoTheme.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun AppPicker(state: ScheduleEditorUiState, onQuery: (String) -> Unit, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp)) {
        Text(stringResource(R.string.editor_pick_app), style = MaterialTheme.typography.headlineSmall, color = FocunoTheme.colors.textPrimary)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.editor_search_apps)) },
        )
        Spacer(Modifier.height(8.dp))
        if (state.isLoadingApps) {
            Column(Modifier.height(240.dp)) { LoadingState() }
        } else {
            LazyColumn(Modifier.heightIn(max = 420.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(state.apps, key = { it.packageName }) { app ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(app.packageName) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AppIcon(app.packageName, size = 44.dp)
                        Text(app.label, style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.textPrimary)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun WhatStep(state: ScheduleEditorUiState, onChoose: (BlockScope) -> Unit, onBack: () -> Unit) {
    val pkg = state.packageName ?: return
    val feed = shortVideoName(pkg)
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StepHeader(stringResource(R.string.editor_what_title), pkg, onBack)
        ChoiceTile(
            title = stringResource(R.string.editor_what_app),
            subtitle = "",
            onClick = { onChoose(BlockScope.APP) },
        )
        ChoiceTile(
            title = stringResource(R.string.editor_what_feed, feed),
            subtitle = "",
            onClick = { onChoose(BlockScope.SHORT_VIDEO) },
        )
        if (!state.accessibilityOn) {
            Text(
                stringResource(R.string.feed_needs_accessibility, feed),
                style = MaterialTheme.typography.bodySmall,
                color = FocunoTheme.colors.warning,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun WhenStep(
    state: ScheduleEditorUiState,
    onPreset: (WhenPreset) -> Unit,
    onCustom: () -> Unit,
    onLimit: () -> Unit,
    onMindful: () -> Unit,
    onSetStrict: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val pkg = state.packageName ?: return
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StepHeader(stringResource(R.string.editor_when_title), pkg, onBack)
        // Above the presets on purpose: a preset saves on tap, so the choice has to be made first.
        StrictSwitch(strict = state.strict, onChange = onSetStrict)
        WhenPreset.entries.forEach { preset ->
            val allowed = !state.strict || StrictLock.allowsStrict(preset.startMinute, preset.endMinute, BlockSchedule.ALL_DAYS)
            ChoiceTile(
                title = stringResource(presetName(preset)),
                subtitle = if (!allowed) {
                    stringResource(R.string.when_not_with_strict)
                } else if (preset.startMinute == preset.endMinute) {
                    ""
                } else {
                    "${formatClock(preset.startMinute)}–${formatClock(preset.endMinute)}"
                },
                onClick = { onPreset(preset) },
                enabled = allowed,
            )
        }
        if (state.scope == BlockScope.APP) {
            ChoiceTile(stringResource(R.string.when_limit), stringResource(R.string.when_limit_sub), onClick = onLimit)
            ChoiceTile(stringResource(R.string.when_mindful), stringResource(R.string.when_mindful_sub), onClick = onMindful)
        }
        OutlinedButton(onClick = onCustom, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(50)) {
            Text(stringResource(R.string.when_custom))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LimitStep(
    state: ScheduleEditorUiState,
    onSetMinutes: (Int) -> Unit,
    onSetOpens: (Int) -> Unit,
    onSetStrict: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val pkg = state.packageName ?: return
    val locked = state.lockedUntilMs != null
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StepHeader(state.appLabel, pkg, onBack = if (state.isEditingLimit) null else onBack)
        Text(stringResource(R.string.limit_title), style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.textSecondary)
        if (locked) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = FocunoTheme.colors.warning)
                Text(stringResource(R.string.limit_locked), style = MaterialTheme.typography.bodyMedium, color = FocunoTheme.colors.warning)
            }
        }
        PillGrid(
            options = LIMIT_CHOICES,
            selected = state.limitMinutes,
            enabled = !locked,
            label = { if (it == 0) stringResource(R.string.limit_off) else durationText(it * 60_000L) },
            onSelect = onSetMinutes,
        )
        Text(stringResource(R.string.limit_opens_title), style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.textSecondary)
        PillGrid(
            options = OPEN_CHOICES,
            selected = state.limitOpens,
            enabled = !locked,
            label = { if (it == 0) stringResource(R.string.limit_off) else it.toString() },
            onSelect = onSetOpens,
        )
        StrictSwitch(
            strict = state.strict,
            onChange = onSetStrict,
            enabled = !locked,
            note = stringResource(if (state.strict) R.string.limit_strict_on else R.string.limit_strict_off),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            if (state.isEditingLimit) {
                OutlinedButton(onClick = onDelete, enabled = !locked, modifier = Modifier.height(52.dp), shape = RoundedCornerShape(50)) {
                    Text(stringResource(R.string.action_delete))
                }
            }
            Button(onClick = onSave, enabled = state.canSaveLimit, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(50)) {
                Text(stringResource(R.string.action_save))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Equal-width pills, three to a row, one selected. */
@Composable
private fun PillGrid(options: List<Int>, selected: Int, enabled: Boolean, label: @Composable (Int) -> String, onSelect: (Int) -> Unit) {
    options.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else FocunoTheme.colors.trackInactive)
                        .clickable(enabled = enabled, role = Role.RadioButton) { onSelect(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label(option),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) FocunoTheme.colors.textPrimary else FocunoTheme.colors.textSecondary,
                    )
                }
            }
            // Keep the last row's pills the same width as the rows above.
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

private fun presetName(preset: WhenPreset): Int = when (preset) {
    WhenPreset.ALL_DAY -> R.string.when_all_day
    WhenPreset.NIGHT -> R.string.when_night
    WhenPreset.SCHOOL -> R.string.when_school
    WhenPreset.EVENING -> R.string.when_evening
}

@Composable
private fun Details(
    state: ScheduleEditorUiState,
    onShowTimePicker: (TimeField) -> Unit,
    onToggleDay: (Int) -> Unit,
    onSetStrict: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val pkg = state.packageName ?: return
    val locked = state.lockedUntilMs != null
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StepHeader(
            title = state.appLabel,
            packageName = pkg,
            onBack = if (state.isEditing) null else onBack,
        )
        if (state.scope == BlockScope.SHORT_VIDEO) {
            Text(
                text = stringResource(R.string.editor_what_feed, shortVideoName(pkg)),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        state.lockedUntilMs?.let { until ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = FocunoTheme.colors.warning)
                Text(
                    stringResource(R.string.editor_locked, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(until))),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocunoTheme.colors.warning,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { onShowTimePicker(TimeField.START) }, enabled = !locked, modifier = Modifier.weight(1f).height(72.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.editor_from), style = MaterialTheme.typography.labelSmall)
                    Text(formatClock(state.startMinute), style = MaterialTheme.typography.headlineSmall)
                }
            }
            OutlinedButton(onClick = { onShowTimePicker(TimeField.END) }, enabled = !locked, modifier = Modifier.weight(1f).height(72.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.editor_until), style = MaterialTheme.typography.labelSmall)
                    Text(formatClock(state.endMinute), style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
        if (state.isOvernight) {
            Text(
                stringResource(if (state.startMinute == state.endMinute) R.string.editor_all_day else R.string.editor_overnight),
                style = MaterialTheme.typography.bodySmall,
                color = FocunoTheme.colors.textSecondary,
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DayOfWeek.entries.forEachIndexed { index, day ->
                DayToggle(
                    letter = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    description = day.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    selected = state.daysMask and (1 shl index) != 0,
                    enabled = !locked,
                    onClick = { onToggleDay(index) },
                )
            }
        }
        if (state.daysMask == 0) {
            Text(stringResource(R.string.editor_pick_a_day), style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.distracting)
        }

        StrictSwitch(
            strict = state.strict,
            onChange = onSetStrict,
            enabled = !locked && (state.strict || state.strictAllowed),
            note = if (!state.strictAllowed) stringResource(R.string.strict_needs_break) else null,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            if (state.isEditing) {
                OutlinedButton(onClick = onDelete, enabled = !locked, modifier = Modifier.height(52.dp), shape = RoundedCornerShape(50)) {
                    Text(stringResource(R.string.action_delete))
                }
            }
            Button(onClick = onSave, enabled = state.canSave, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(50)) {
                Text(stringResource(R.string.action_save))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DayToggle(letter: String, description: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val fill = if (selected) MaterialTheme.colorScheme.primary else FocunoTheme.colors.trackInactive
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(fill.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) FocunoTheme.colors.textPrimary else FocunoTheme.colors.textSecondary,
        )
    }
}
