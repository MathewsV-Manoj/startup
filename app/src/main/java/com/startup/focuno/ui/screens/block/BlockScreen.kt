package com.startup.focuno.ui.screens.block

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.data.repository.ProtectionStatus
import com.startup.focuno.domain.model.BlockSchedule
import com.startup.focuno.domain.model.BlockScope
import com.startup.focuno.ui.components.AppIcon
import com.startup.focuno.ui.components.LoadingState
import com.startup.focuno.ui.components.Panel
import com.startup.focuno.ui.components.ProtectionBanner
import com.startup.focuno.ui.components.RefreshWhileResumed
import com.startup.focuno.ui.components.ScreenTitle
import com.startup.focuno.ui.components.durationText
import com.startup.focuno.ui.components.formatClock
import com.startup.focuno.ui.components.shortVideoName
import com.startup.focuno.ui.theme.FocunoTheme
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BlockScreen(
    onOpenHealth: () -> Unit,
    onAddSchedule: () -> Unit,
    onEditSchedule: (BlockSchedule) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BlockViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshWhileResumed(intervalMs = 5_000L, onRefresh = viewModel::refreshProtection)
    BlockContent(
        state = state,
        onOpenHealth = onOpenHealth,
        onAdd = onAddSchedule,
        onEdit = onEditSchedule,
        onToggle = viewModel::setEnabled,
        modifier = modifier,
    )
}

@Composable
fun BlockContent(
    state: BlockUiState,
    onOpenHealth: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (BlockSchedule) -> Unit,
    onToggle: (BlockSchedule, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle(stringResource(R.string.tab_block)) { ShieldDot(state.protection, onOpenHealth) }
            when {
                state.isLoading -> LoadingState()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { ProtectionBanner(state.protection, onFix = onOpenHealth) }
                    item { FocusRunningRow(state.focusUntilMs) }
                    if (state.items.isEmpty()) {
                        item { EmptyBlocks(onAdd) }
                    } else {
                        items(state.items, key = { it.row.schedule.id }) { item -> ScheduleRow(item, onEdit, onToggle) }
                    }
                }
            }
        }
        if (!state.isLoading && state.items.isNotEmpty()) {
            FloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = FocunoTheme.colors.textPrimary,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.block_add))
            }
        }
    }
}

/** A small green or red dot with "On" or "Off", so the state of blocking is visible at a glance. */
@Composable
private fun ShieldDot(status: ProtectionStatus?, onClick: () -> Unit) {
    if (status == null) return
    val on = status.blockingActive
    val tint = if (on) FocunoTheme.colors.productive else FocunoTheme.colors.distracting
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).background(tint, CircleShape))
        Text(stringResource(if (on) R.string.status_on else R.string.status_off), style = MaterialTheme.typography.labelLarge, color = tint)
    }
}

@Composable
private fun FocusRunningRow(untilMs: Long) {
    val now by produceState(initialValue = System.currentTimeMillis(), untilMs) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    if (untilMs <= now) return
    Panel(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = FocunoTheme.colors.productive)
            Text(
                stringResource(R.string.block_focus_running, durationText(untilMs - now, roundUp = true)),
                style = MaterialTheme.typography.titleSmall,
                color = FocunoTheme.colors.productive,
            )
        }
    }
}

@Composable
private fun EmptyBlocks(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Rounded.Shield, contentDescription = null, tint = FocunoTheme.colors.textTertiary, modifier = Modifier.size(72.dp))
        Text(
            stringResource(R.string.block_empty),
            style = MaterialTheme.typography.titleLarge,
            color = FocunoTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onAdd, modifier = Modifier.height(56.dp), shape = RoundedCornerShape(50)) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.block_add), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ScheduleRow(item: ScheduleItem, onEdit: (BlockSchedule) -> Unit, onToggle: (BlockSchedule, Boolean) -> Unit) {
    val schedule = item.row.schedule
    val locked = item.row.lockedUntilMs != null
    Panel(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable { onEdit(schedule) },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AppIcon(schedule.packageName, size = 44.dp)
            Column(Modifier.weight(1f)) {
                val name = if (schedule.scope == BlockScope.SHORT_VIDEO) {
                    stringResource(R.string.block_row_feed, item.label, shortVideoName(schedule.packageName))
                } else {
                    item.label
                }
                Text(name, style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.textPrimary, maxLines = 1)
                Text(
                    "${timeRange(schedule)} · ${daysSummary(schedule.daysOfWeekMask)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FocunoTheme.colors.textSecondary,
                    maxLines = 1,
                )
            }
            Icon(
                imageVector = if (schedule.strictMode) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                contentDescription = stringResource(if (schedule.strictMode) R.string.cd_strict_on else R.string.cd_strict_off),
                tint = if (schedule.strictMode) FocunoTheme.colors.productive else FocunoTheme.colors.warning,
                modifier = Modifier.size(20.dp),
            )
            Switch(checked = schedule.enabled, onCheckedChange = { onToggle(schedule, it) }, enabled = !locked)
        }
    }
}

@Composable
private fun timeRange(schedule: BlockSchedule): String =
    if (schedule.startMinuteOfDay == schedule.endMinuteOfDay) {
        stringResource(R.string.when_all_day)
    } else {
        "${formatClock(schedule.startMinuteOfDay)}–${formatClock(schedule.endMinuteOfDay)}"
    }

@Composable
private fun daysSummary(mask: Int): String {
    val weekdays = 0b0011111
    val weekend = 0b1100000
    return when (mask) {
        BlockSchedule.ALL_DAYS -> stringResource(R.string.days_every_day)
        weekdays -> stringResource(R.string.days_weekdays)
        weekend -> stringResource(R.string.days_weekends)
        else -> DayOfWeek.entries.filterIndexed { index, _ -> mask and (1 shl index) != 0 }
            .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }
}
