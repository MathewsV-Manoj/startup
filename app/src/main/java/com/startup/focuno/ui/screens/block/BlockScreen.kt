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
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.startup.focuno.domain.model.AppLimit
import com.startup.focuno.domain.model.BlockSchedule
import com.startup.focuno.domain.model.BlockScope
import com.startup.focuno.ui.components.AppIcon
import com.startup.focuno.ui.components.LoadingState
import com.startup.focuno.ui.components.Panel
import com.startup.focuno.ui.components.ProtectionBanner
import com.startup.focuno.ui.components.RefreshWhileResumed
import com.startup.focuno.ui.components.ScreenTitle
import com.startup.focuno.ui.components.StrictSwitch
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
    onEditLimit: (AppLimit, Long?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BlockViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshWhileResumed(intervalMs = 5_000L, onRefresh = viewModel::refreshProtection)
    RefreshWhileResumed(intervalMs = 30_000L, onRefresh = viewModel::refreshUsage)
    LaunchedEffect(state.limits.size) { viewModel.refreshUsage() }
    BlockContent(
        state = state,
        onOpenHealth = onOpenHealth,
        onAdd = onAddSchedule,
        onEdit = onEditSchedule,
        onToggle = viewModel::setEnabled,
        onEditLimit = { onEditLimit(it.limit, it.lockedUntilMs) },
        onToggleLimit = viewModel::setLimitEnabled,
        onSetBudget = viewModel::setBudget,
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
    onEditLimit: (LimitItem) -> Unit,
    onToggleLimit: (LimitItem, Boolean) -> Unit,
    onSetBudget: (minutes: Int, strict: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEmpty = state.items.isEmpty() && state.limits.isEmpty()
    var editingBudget by rememberSaveable { mutableStateOf(false) }
    if (editingBudget) {
        BudgetDialog(state.budget, onSave = onSetBudget, onDismiss = { editingBudget = false })
    }
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
                    item { BudgetRow(state.budget, onClick = { editingBudget = true }) }
                    if (isEmpty) {
                        item { EmptyBlocks(onAdd) }
                    }
                    if (state.limits.isNotEmpty()) {
                        item { SectionLabel(stringResource(R.string.section_limits)) }
                        items(state.limits, key = { "limit:${it.limit.packageName}" }) { item -> LimitRow(item, onEditLimit, onToggleLimit) }
                    }
                    if (state.items.isNotEmpty()) {
                        item { SectionLabel(stringResource(R.string.section_schedules)) }
                        items(state.items, key = { "schedule:${it.row.schedule.id}" }) { item -> ScheduleRow(item, onEdit, onToggle) }
                    }
                }
            }
        }
        if (!state.isLoading && !isEmpty) {
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

/** One budget for all time-eater apps together. Always shown, so it is easy to find and switch on. */
@Composable
private fun BudgetRow(budget: BudgetUi, onClick: () -> Unit) {
    val on = budget.minutes > 0
    val dailyMs = budget.minutes * 60_000L
    val over = on && budget.usedMs >= dailyMs
    Panel(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(onClick = onClick),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(FocunoTheme.colors.distracting.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.HourglassBottom, contentDescription = null, tint = FocunoTheme.colors.distracting)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.budget_title), style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.textPrimary)
                Text(
                    if (on) stringResource(R.string.limit_used, durationText(budget.usedMs), durationText(dailyMs)) else stringResource(R.string.budget_off),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (over) FocunoTheme.colors.distracting else FocunoTheme.colors.textSecondary,
                )
                if (on) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(FocunoTheme.colors.trackInactive),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((budget.usedMs.toFloat() / dailyMs).coerceIn(0.02f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (over) FocunoTheme.colors.distracting else FocunoTheme.colors.warning),
                        )
                    }
                }
            }
            if (on) {
                Icon(
                    imageVector = if (budget.strict) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = stringResource(if (budget.strict) R.string.cd_strict_on else R.string.cd_strict_off),
                    tint = if (budget.strict) FocunoTheme.colors.productive else FocunoTheme.colors.warning,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun BudgetDialog(budget: BudgetUi, onSave: (Int, Boolean) -> Unit, onDismiss: () -> Unit) {
    val locked = budget.lockedUntilMs != null
    var minutes by rememberSaveable { mutableIntStateOf(budget.minutes) }
    var strict by rememberSaveable { mutableStateOf(budget.strict) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.HourglassBottom, contentDescription = null) },
        title = { Text(stringResource(R.string.budget_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.budget_body), style = MaterialTheme.typography.bodyMedium)
                if (locked) {
                    Text(stringResource(R.string.limit_locked), style = MaterialTheme.typography.bodyMedium, color = FocunoTheme.colors.warning)
                }
                BUDGET_CHOICES.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { option ->
                            val selected = option == minutes
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else FocunoTheme.colors.trackInactive)
                                    .clickable(enabled = !locked) { minutes = option },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (option == 0) stringResource(R.string.budget_off_short) else durationText(option * 60_000L),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) FocunoTheme.colors.textPrimary else FocunoTheme.colors.textSecondary,
                                )
                            }
                        }
                    }
                }
                if (minutes > 0) {
                    StrictSwitch(
                        strict = strict,
                        onChange = { strict = it },
                        enabled = !locked,
                        note = stringResource(if (strict) R.string.limit_strict_on else R.string.limit_strict_off),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(minutes, strict)
                    onDismiss()
                },
                enabled = !locked,
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private val BUDGET_CHOICES = listOf(0, 30, 60, 90, 120, 180)

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = FocunoTheme.colors.textTertiary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}

@Composable
private fun LimitRow(item: LimitItem, onEdit: (LimitItem) -> Unit, onToggle: (LimitItem, Boolean) -> Unit) {
    val limit = item.limit
    val fraction = (item.usedMs.toFloat() / limit.dailyMs).coerceIn(0f, 1f)
    val over = item.usedMs >= limit.dailyMs
    Panel(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable { onEdit(item) },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AppIcon(limit.packageName, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(item.label, style = MaterialTheme.typography.titleMedium, color = FocunoTheme.colors.textPrimary, maxLines = 1)
                Text(
                    stringResource(R.string.limit_used, durationText(item.usedMs), durationText(limit.dailyMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (over) FocunoTheme.colors.distracting else FocunoTheme.colors.textSecondary,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(FocunoTheme.colors.trackInactive),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (over) FocunoTheme.colors.distracting else FocunoTheme.colors.productive),
                    )
                }
            }
            Icon(
                imageVector = if (limit.strict) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                contentDescription = stringResource(if (limit.strict) R.string.cd_strict_on else R.string.cd_strict_off),
                tint = if (limit.strict) FocunoTheme.colors.productive else FocunoTheme.colors.warning,
                modifier = Modifier.size(20.dp),
            )
            Switch(checked = limit.enabled, onCheckedChange = { onToggle(item, it) }, enabled = item.lockedUntilMs == null)
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
