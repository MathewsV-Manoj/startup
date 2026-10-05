package com.startup.focuno.ui.screens.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.data.repository.FocusSessionRepository
import com.startup.focuno.domain.model.FocusMode
import com.startup.focuno.domain.model.FocusPlan
import com.startup.focuno.domain.model.FocusSound
import com.startup.focuno.ui.components.ProtectionBanner
import com.startup.focuno.ui.components.RefreshWhileResumed
import com.startup.focuno.ui.components.Segmented
import com.startup.focuno.ui.components.StrictSwitch
import com.startup.focuno.ui.components.TimerDial
import com.startup.focuno.ui.components.durationText
import com.startup.focuno.ui.theme.FocunoTheme
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private val DURATIONS = listOf(15, 25, 45, 60, 90)
private val ROUND_CHOICES = listOf(2, 3, 4)
private const val HOUR_MS = 3_600_000L

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenHealth: () -> Unit,
    modifier: Modifier = Modifier,
    focusViewModel: FocusViewModel = hiltViewModel(),
) {
    val focus by focusViewModel.state.collectAsStateWithLifecycle()
    RefreshWhileResumed(intervalMs = 30_000L, onRefresh = focusViewModel::refresh)
    HomeContent(
        focus = focus,
        onOpenSettings = onOpenSettings,
        onOpenHealth = onOpenHealth,
        onStartSession = focusViewModel::startFocusSession,
        onStopSession = focusViewModel::stopFocusSession,
        onAddSubject = focusViewModel::addSubject,
        onRemoveSubject = focusViewModel::removeSubject,
        onSetSound = focusViewModel::setFocusSound,
        modifier = modifier,
        onPlanOver = focusViewModel::refresh,
    )
}

@Composable
fun HomeContent(
    focus: FocusUiState,
    onOpenSettings: () -> Unit,
    onOpenHealth: () -> Unit,
    onStartSession: (mode: FocusMode, amount: Int, strict: Boolean, subject: String) -> Unit,
    onStopSession: () -> Unit,
    onAddSubject: (String) -> Unit,
    onRemoveSubject: (String) -> Unit,
    onSetSound: (FocusSound) -> Unit,
    modifier: Modifier = Modifier,
    onPlanOver: () -> Unit = {},
) {
    var choosingSound by rememberSaveable { mutableStateOf(false) }
    if (choosingSound) {
        SoundDialog(current = focus.focusSound, onSelect = onSetSound, onDismiss = { choosingSound = false })
    }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopRow(focus, onOpenSettings, onOpenHealth, onOpenSound = { choosingSound = true })
        ProtectionBanner(focus.protection, onFix = onOpenHealth)
        Spacer(Modifier.height(28.dp))
        val plan = focus.plan
        if (plan == null) {
            IdleTimer(focus.subjects, onStartSession, onAddSubject, onRemoveSubject)
        } else {
            RunningTimer(
                plan = plan,
                strict = focus.sessionStrict,
                subject = focus.sessionSubject,
                onStop = onStopSession,
                onPlanOver = onPlanOver,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TopRow(focus: FocusUiState, onOpenSettings: () -> Unit, onOpenHealth: () -> Unit, onOpenSound: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val streakText = pluralStringResource(R.plurals.streak_days, focus.streak, focus.streak)
        Pill(Icons.Rounded.LocalFireDepartment, focus.streak.toString(), FocunoTheme.colors.warning, streakText)
        val screenTime = if (focus.hasUsageAccess) focus.today?.let { durationText(it.totalMs) } else null
        Pill(
            icon = Icons.Rounded.PhoneAndroid,
            text = screenTime ?: stringResource(R.string.gauge_dash),
            tint = FocunoTheme.colors.productive,
            description = stringResource(R.string.cd_screen_time_today, screenTime ?: stringResource(R.string.gauge_dash)),
            onClick = if (focus.hasUsageAccess) null else onOpenHealth,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onOpenSound) {
            Icon(
                Icons.Rounded.Headphones,
                contentDescription = stringResource(R.string.sound_title),
                tint = if (focus.focusSound == FocusSound.OFF) FocunoTheme.colors.textSecondary else MaterialTheme.colorScheme.secondary,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings_title), tint = FocunoTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun Pill(icon: ImageVector, text: String, tint: Color, description: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(FocunoTheme.colors.surfaceElevated)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = FocunoTheme.colors.textPrimary)
    }
}

@Composable
private fun IdleTimer(
    subjects: List<String>,
    onStart: (mode: FocusMode, amount: Int, strict: Boolean, subject: String) -> Unit,
    onAddSubject: (String) -> Unit,
    onRemoveSubject: (String) -> Unit,
) {
    var modeIndex by rememberSaveable { mutableIntStateOf(0) }
    var minutes by rememberSaveable { mutableIntStateOf(25) }
    var rounds by rememberSaveable { mutableIntStateOf(4) }
    var strict by rememberSaveable { mutableStateOf(false) }
    var confirmStrict by rememberSaveable { mutableStateOf(false) }
    var subject by rememberSaveable { mutableStateOf("") }
    var managing by rememberSaveable { mutableStateOf(false) }
    val mode = FocusMode.entries[modeIndex]
    // A subject removed elsewhere must not stay selected.
    val chosen = subject.takeIf { it in subjects }.orEmpty()
    val amount = if (mode == FocusMode.POMODORO) rounds else minutes
    val canBeStrict = mode != FocusMode.STOPWATCH
    val lockMinutes = when (mode) {
        FocusMode.POMODORO -> rounds * FocusPlan.POMODORO_FOCUS_MIN + (rounds - 1) * FocusPlan.POMODORO_BREAK_MIN
        else -> minutes
    }

    Segmented(
        options = listOf(stringResource(R.string.mode_timer), stringResource(R.string.mode_pomodoro), stringResource(R.string.mode_stopwatch)),
        selected = modeIndex,
        onSelect = { modeIndex = it },
    )
    Spacer(Modifier.height(24.dp))
    TimerDial(
        progress = when (mode) {
            FocusMode.TIMER -> minutes / 90f
            FocusMode.POMODORO -> rounds / 4f
            FocusMode.STOPWATCH -> 0f
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when (mode) {
                    FocusMode.TIMER -> minutes.toString()
                    FocusMode.POMODORO -> FocusPlan.POMODORO_FOCUS_MIN.toString()
                    FocusMode.STOPWATCH -> clockText(0L)
                },
                style = if (mode == FocusMode.STOPWATCH) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge,
                color = FocunoTheme.colors.textPrimary,
            )
            Text(
                if (mode == FocusMode.STOPWATCH) stringResource(R.string.mode_stopwatch) else stringResource(R.string.home_minutes_unit),
                style = MaterialTheme.typography.titleMedium,
                color = FocunoTheme.colors.textSecondary,
            )
        }
    }
    Spacer(Modifier.height(24.dp))
    when (mode) {
        FocusMode.TIMER -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            DURATIONS.forEach { option ->
                ChoicePill(option.toString(), stringResource(R.string.cd_minutes, option), selected = option == minutes, onClick = { minutes = option })
            }
        }
        FocusMode.POMODORO -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.pomodoro_caption, FocusPlan.POMODORO_FOCUS_MIN, FocusPlan.POMODORO_BREAK_MIN),
                style = MaterialTheme.typography.bodyMedium,
                color = FocunoTheme.colors.textSecondary,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ROUND_CHOICES.forEach { option ->
                    ChoicePill(
                        stringResource(R.string.pomodoro_rounds, option),
                        stringResource(R.string.cd_rounds, option),
                        selected = option == rounds,
                        onClick = { rounds = option },
                    )
                }
            }
        }
        FocusMode.STOPWATCH -> Text(
            stringResource(R.string.stopwatch_caption),
            style = MaterialTheme.typography.bodyMedium,
            color = FocunoTheme.colors.textSecondary,
        )
    }
    Spacer(Modifier.height(16.dp))
    SubjectRow(subjects, chosen, onSelect = { subject = if (it == chosen) "" else it }, onManage = { managing = true })
    Spacer(Modifier.height(16.dp))
    if (canBeStrict) {
        StrictSwitch(strict = strict, onChange = { strict = it })
        Spacer(Modifier.height(20.dp))
    }
    Button(
        onClick = { if (strict && canBeStrict) confirmStrict = true else onStart(mode, amount, false, chosen) },
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(50),
    ) {
        Text(stringResource(R.string.home_start), style = MaterialTheme.typography.titleLarge)
    }

    if (confirmStrict) {
        AlertDialog(
            onDismissRequest = { confirmStrict = false },
            icon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            title = { Text(stringResource(R.string.strict_confirm_title, lockMinutes)) },
            text = { Text(stringResource(R.string.strict_confirm_body)) },
            confirmButton = {
                Button(onClick = {
                    confirmStrict = false
                    onStart(mode, amount, true, chosen)
                }) { Text(stringResource(R.string.strict_confirm_ok)) }
            },
            dismissButton = { TextButton(onClick = { confirmStrict = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (managing) {
        SubjectsDialog(subjects, onAdd = onAddSubject, onRemove = onRemoveSubject, onDismiss = { managing = false })
    }
}

/** "12:34", or "1:02:03" once past an hour. */
@Composable
private fun clockText(ms: Long): String {
    val totalSeconds = (ms / 1_000).toInt()
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        stringResource(R.string.timer_clock_hours, hours, minutes, seconds)
    } else {
        stringResource(R.string.timer_clock, minutes, seconds)
    }
}

@Composable
private fun SubjectRow(subjects: List<String>, chosen: String, onSelect: (String) -> Unit, onManage: () -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        items(subjects, key = { it }) { name -> Chip(name, selected = name == chosen, onClick = { onSelect(name) }) }
        item(key = "+") {
            Chip(
                text = if (subjects.isEmpty()) stringResource(R.string.subject_add) else "+",
                selected = false,
                onClick = onManage,
                description = stringResource(R.string.subject_manage),
            )
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit, description: String? = null) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) FocunoTheme.colors.textPrimary else FocunoTheme.colors.textSecondary,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f) else FocunoTheme.colors.surfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .let { if (description != null) it.semantics { contentDescription = description } else it },
    )
}

/** Pick the background sound. Picking one plays it: for the rest of a running timer, or as a short preview. */
@Composable
private fun SoundDialog(current: FocusSound, onSelect: (FocusSound) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Headphones, contentDescription = null) },
        title = { Text(stringResource(R.string.sound_title)) },
        text = {
            Column {
                FocusSound.entries.forEach { sound ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(sound) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = sound == current, onClick = { onSelect(sound) })
                        Text(stringResource(soundName(sound)), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
    )
}

private fun soundName(sound: FocusSound): Int = when (sound) {
    FocusSound.OFF -> R.string.sound_off
    FocusSound.WHITE -> R.string.sound_white
    FocusSound.PINK -> R.string.sound_pink
    FocusSound.BROWN -> R.string.sound_brown
    FocusSound.WAVES -> R.string.sound_waves
}

/** Add a subject, or remove one. Kept in a dialog so the Focus tab stays clean. */
@Composable
private fun SubjectsDialog(subjects: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.subject_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                subjects.forEach { subject ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(subject, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onRemove(subject) }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.subject_remove, subject))
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.subject_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(name)
                    name = ""
                },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.subject_add_button)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
    )
}

@Composable
private fun ChoicePill(text: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = if (selected) FocunoTheme.colors.textPrimary else FocunoTheme.colors.textSecondary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.primary else FocunoTheme.colors.surfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics { contentDescription = description },
    )
}

@Composable
private fun RunningTimer(plan: FocusPlan, strict: Boolean, subject: String, onStop: () -> Unit, onPlanOver: () -> Unit) {
    val now by produceState(initialValue = System.currentTimeMillis(), plan) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val over = now >= plan.endMs
    LaunchedEffect(over) { if (over) onPlanOver() }
    var confirmEnd by rememberSaveable { mutableStateOf(false) }

    if (plan.mode == FocusMode.STOPWATCH) {
        val elapsed = (now - plan.startMs).coerceAtLeast(0L)
        TimerDial(progress = (elapsed % HOUR_MS).toFloat() / HOUR_MS) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(clockText(elapsed), style = MaterialTheme.typography.displayMedium, color = FocunoTheme.colors.textPrimary)
                Text(
                    subject.ifBlank { stringResource(R.string.mode_stopwatch) },
                    style = MaterialTheme.typography.titleMedium,
                    color = FocunoTheme.colors.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { if (elapsed < FocusSessionRepository.STOPWATCH_COUNTS_AFTER_MS) confirmEnd = true else onStop() },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(50),
        ) {
            Text(stringResource(R.string.home_finish), style = MaterialTheme.typography.titleLarge)
        }
        if (confirmEnd) {
            AlertDialog(
                onDismissRequest = { confirmEnd = false },
                title = { Text(stringResource(R.string.finish_short_title)) },
                text = { Text(stringResource(R.string.finish_short_body)) },
                confirmButton = { Button(onClick = { confirmEnd = false }) { Text(stringResource(R.string.end_early_keep_going)) } },
                dismissButton = {
                    TextButton(onClick = {
                        confirmEnd = false
                        onStop()
                    }) { Text(stringResource(R.string.home_finish)) }
                },
            )
        }
        return
    }

    val phase = plan.phaseAt(now)
    val focusing = phase?.focusing ?: true
    val phaseStart = phase?.startsAtMs ?: plan.startMs
    val phaseEnd = phase?.endsAtMs ?: plan.endMs
    val remaining = (phaseEnd - now).coerceAtLeast(0L)
    val label = when {
        !focusing -> stringResource(R.string.home_break)
        plan.mode == FocusMode.POMODORO && phase != null -> {
            val round = stringResource(R.string.home_round, phase.round, phase.rounds)
            if (subject.isBlank()) round else "$subject · $round"
        }
        else -> subject.ifBlank { stringResource(R.string.home_focusing) }
    }

    TimerDial(progress = remaining.toFloat() / (phaseEnd - phaseStart).coerceAtLeast(1L)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                clockText(remaining),
                style = MaterialTheme.typography.displayMedium,
                color = if (focusing) FocunoTheme.colors.textPrimary else FocunoTheme.colors.productive,
            )
            Text(label, style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.textSecondary)
        }
    }
    Spacer(Modifier.height(32.dp))
    if (strict) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = FocunoTheme.colors.productive)
            Text(
                stringResource(R.string.home_locked_until, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(plan.endMs))),
                style = MaterialTheme.typography.titleMedium,
                color = FocunoTheme.colors.productive,
            )
        }
    } else {
        OutlinedButton(
            onClick = { confirmEnd = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(50),
        ) {
            Text(stringResource(R.string.home_end_early), style = MaterialTheme.typography.titleMedium)
        }
    }

    if (confirmEnd && !strict) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text(stringResource(R.string.end_early_title)) },
            text = { Text(stringResource(R.string.end_early_body)) },
            confirmButton = { Button(onClick = { confirmEnd = false }) { Text(stringResource(R.string.end_early_keep_going)) } },
            dismissButton = {
                TextButton(onClick = {
                    confirmEnd = false
                    onStop()
                }) { Text(stringResource(R.string.end_early_confirm)) }
            },
        )
    }
}
