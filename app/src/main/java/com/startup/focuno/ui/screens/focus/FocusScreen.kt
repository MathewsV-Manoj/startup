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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.startup.focuno.ui.components.ProtectionBanner
import com.startup.focuno.ui.components.RefreshWhileResumed
import com.startup.focuno.ui.components.StrictSwitch
import com.startup.focuno.ui.components.TimerDial
import com.startup.focuno.ui.components.durationText
import com.startup.focuno.ui.theme.FocunoTheme
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private val DURATIONS = listOf(15, 25, 45, 60, 90)

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
        modifier = modifier,
    )
}

@Composable
fun HomeContent(
    focus: FocusUiState,
    onOpenSettings: () -> Unit,
    onOpenHealth: () -> Unit,
    onStartSession: (minutes: Int, strict: Boolean) -> Unit,
    onStopSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopRow(focus, onOpenSettings, onOpenHealth)
        ProtectionBanner(focus.protection, onFix = onOpenHealth)
        Spacer(Modifier.height(28.dp))
        val endsAt = focus.sessionEndsAtMs
        if (endsAt == null) {
            IdleTimer(onStartSession)
        } else {
            RunningTimer(
                startedAtMs = focus.sessionStartedAtMs ?: endsAt,
                endsAtMs = endsAt,
                strict = focus.sessionStrict,
                onStop = onStopSession,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TopRow(focus: FocusUiState, onOpenSettings: () -> Unit, onOpenHealth: () -> Unit) {
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
private fun IdleTimer(onStart: (minutes: Int, strict: Boolean) -> Unit) {
    var minutes by rememberSaveable { mutableIntStateOf(25) }
    var strict by rememberSaveable { mutableStateOf(false) }
    var confirmStrict by rememberSaveable { mutableStateOf(false) }

    TimerDial(progress = minutes / 90f) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(minutes.toString(), style = MaterialTheme.typography.displayLarge, color = FocunoTheme.colors.textPrimary)
            Text(stringResource(R.string.home_minutes_unit), style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.textSecondary)
        }
    }
    Spacer(Modifier.height(28.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        DURATIONS.forEach { option ->
            DurationPill(option, selected = option == minutes, onClick = { minutes = option })
        }
    }
    Spacer(Modifier.height(20.dp))
    StrictSwitch(strict = strict, onChange = { strict = it })
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = { if (strict) confirmStrict = true else onStart(minutes, false) },
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(50),
    ) {
        Text(stringResource(R.string.home_start), style = MaterialTheme.typography.titleLarge)
    }

    if (confirmStrict) {
        AlertDialog(
            onDismissRequest = { confirmStrict = false },
            icon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            title = { Text(stringResource(R.string.strict_confirm_title, minutes)) },
            text = { Text(stringResource(R.string.strict_confirm_body)) },
            confirmButton = {
                Button(onClick = {
                    confirmStrict = false
                    onStart(minutes, true)
                }) { Text(stringResource(R.string.strict_confirm_ok)) }
            },
            dismissButton = { TextButton(onClick = { confirmStrict = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun DurationPill(minutes: Int, selected: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.cd_minutes, minutes)
    Text(
        text = minutes.toString(),
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
private fun RunningTimer(startedAtMs: Long, endsAtMs: Long, strict: Boolean, onStop: () -> Unit) {
    val now by produceState(initialValue = System.currentTimeMillis(), endsAtMs) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    var confirmEnd by rememberSaveable { mutableStateOf(false) }
    val total = (endsAtMs - startedAtMs).coerceAtLeast(1L)
    val remaining = (endsAtMs - now).coerceAtLeast(0L)

    TimerDial(progress = remaining.toFloat() / total) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.timer_clock, (remaining / 60_000).toInt(), ((remaining / 1_000) % 60).toInt()),
                style = MaterialTheme.typography.displayMedium,
                color = FocunoTheme.colors.textPrimary,
            )
            Text(stringResource(R.string.home_focusing), style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.textSecondary)
        }
    }
    Spacer(Modifier.height(32.dp))
    if (strict) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = FocunoTheme.colors.productive)
            Text(
                stringResource(R.string.home_locked_until, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(endsAtMs))),
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
