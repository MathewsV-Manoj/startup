package com.startup.focuno.ui.screens.block

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.domain.model.BlockSchedule
import com.startup.focuno.domain.model.BlockScope
import com.startup.focuno.ui.components.shortVideoName
import com.startup.focuno.ui.components.AppIcon
import com.startup.focuno.ui.components.EmptyState
import com.startup.focuno.ui.components.GlassCard
import com.startup.focuno.ui.components.LoadingState
import com.startup.focuno.ui.components.ScreenTitle
import com.startup.focuno.ui.components.SectionTitle
import com.startup.focuno.ui.components.durationText
import com.startup.focuno.ui.components.formatClock
import com.startup.focuno.ui.theme.FocunoTheme
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BlockScreen(
    onAddSchedule: () -> Unit,
    onEditSchedule: (BlockSchedule) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BlockViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BlockContent(
        state = state,
        onAdd = onAddSchedule,
        onEdit = onEditSchedule,
        onToggle = viewModel::setEnabled,
        onQuickBlock = viewModel::startQuickBlock,
        modifier = modifier,
    )
}

@Composable
fun BlockContent(
    state: BlockUiState,
    onAdd: () -> Unit,
    onEdit: (BlockSchedule) -> Unit,
    onToggle: (BlockSchedule, Boolean) -> Unit,
    onQuickBlock: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle(stringResource(R.string.tab_block))
            if (state.isLoading) {
                LoadingState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item { QuickBlockCard(state, onQuickBlock) }
                    if (state.groups.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Rounded.Shield,
                                title = stringResource(R.string.block_empty_title),
                                message = stringResource(R.string.block_empty_message),
                                actionLabel = stringResource(R.string.block_add_schedule),
                                onAction = onAdd,
                            )
                        }
                    } else {
                        items(state.groups, key = { it.packageName }) { group ->
                            GlassCard(Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    AppIcon(group.packageName, size = 40.dp)
                                    SectionTitle(group.label)
                                }
                                Spacer(Modifier.height(8.dp))
                                group.schedules.forEach { row -> ScheduleRow(row, onEdit, onToggle) }
                            }
                        }
                    }
                }
            }
        }
        if (!state.isLoading && state.groups.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.block_add_schedule)) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = FocunoTheme.colors.textPrimary,
            )
        }
    }
}

@Composable
private fun QuickBlockCard(state: BlockUiState, onQuickBlock: (Int) -> Unit) {
    val now by produceState(initialValue = System.currentTimeMillis(), state.quickBlockUntilMs) {
        while (true) {
            delay(1_000)
            value = System.currentTimeMillis()
        }
    }
    val active = state.quickBlockUntilMs > now
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            SectionTitle(stringResource(R.string.quick_block_title))
        }
        Spacer(Modifier.height(8.dp))
        if (active) {
            Text(
                stringResource(R.string.quick_block_active, durationText(state.quickBlockUntilMs - now, roundUp = true)),
                style = MaterialTheme.typography.titleMedium,
                color = FocunoTheme.colors.productive,
            )
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.quick_block_active_note), style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.textSecondary)
        } else {
            Text(stringResource(R.string.quick_block_explainer), style = MaterialTheme.typography.bodyMedium, color = FocunoTheme.colors.textSecondary)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 60, 120).forEach { minutes ->
                    AssistChip(
                        onClick = { onQuickBlock(minutes) },
                        label = { Text(if (minutes < 60) stringResource(R.string.duration_minutes, minutes) else stringResource(R.string.duration_hours, minutes / 60)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(row: ScheduleRowUi, onEdit: (BlockSchedule) -> Unit, onToggle: (BlockSchedule, Boolean) -> Unit) {
    val schedule = row.schedule
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onEdit(schedule) }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "${formatClock(schedule.startMinuteOfDay)} – ${formatClock(schedule.endMinuteOfDay)}",
                style = MaterialTheme.typography.titleMedium,
                color = FocunoTheme.colors.textPrimary,
            )
            Text(daysSummary(schedule.daysOfWeekMask), style = MaterialTheme.typography.bodySmall, color = FocunoTheme.colors.textSecondary)
            if (schedule.scope == BlockScope.SHORT_VIDEO) {
                Text(
                    stringResource(R.string.schedule_scope_feed, shortVideoName(schedule.packageName)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (schedule.strictMode) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = FocunoTheme.colors.warning, modifier = Modifier.height(14.dp))
                    Text(
                        text = if (row.lockedUntilMs != null) stringResource(R.string.schedule_strict_locked) else stringResource(R.string.schedule_strict),
                        style = MaterialTheme.typography.labelSmall,
                        color = FocunoTheme.colors.warning,
                    )
                }
            }
        }
        Switch(checked = schedule.enabled, onCheckedChange = { onToggle(schedule, it) }, enabled = row.lockedUntilMs == null)
    }
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
