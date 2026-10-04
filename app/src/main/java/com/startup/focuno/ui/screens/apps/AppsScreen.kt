package com.startup.focuno.ui.screens.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.domain.model.AppCategory
import com.startup.focuno.ui.components.AppIcon
import com.startup.focuno.ui.components.EmptyState
import com.startup.focuno.ui.components.ErrorState
import com.startup.focuno.ui.components.LoadingState
import com.startup.focuno.ui.components.Panel
import com.startup.focuno.ui.components.RefreshWhileResumed
import com.startup.focuno.ui.components.categoryColor
import com.startup.focuno.ui.components.categoryLabel
import com.startup.focuno.ui.components.durationText
import com.startup.focuno.ui.theme.FocunoTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AppsScreen(
    onOpenHealth: () -> Unit,
    onScheduleApp: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshWhileResumed(intervalMs = 30_000L, onRefresh = viewModel::refresh)
    AppsContent(
        state = state,
        onSelectDay = viewModel::selectDay,
        onSetCategory = viewModel::setCategory,
        onSchedule = onScheduleApp,
        onRetry = viewModel::refresh,
        onOpenHealth = onOpenHealth,
        modifier = modifier,
    )
}

@Composable
fun AppsContent(
    state: AppsUiState,
    onSelectDay: (Int) -> Unit,
    onSetCategory: (String, AppCategory) -> Unit,
    onSchedule: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenHealth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        if (state.days.isNotEmpty()) DaySelector(state.days, state.dayOffset, onSelectDay)
        when {
            state.isLoading -> LoadingState()
            !state.hasUsageAccess -> EmptyState(
                icon = Icons.Rounded.Apps,
                title = stringResource(R.string.apps_no_access_title),
                message = "",
                actionLabel = stringResource(R.string.focus_allow_usage_access),
                onAction = onOpenHealth,
            )
            state.hasError -> ErrorState(stringResource(R.string.error_generic), onRetry)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { DaySummary(state) }
                if (state.rows.isEmpty()) {
                    item { EmptyState(icon = Icons.Rounded.Apps, title = stringResource(R.string.apps_empty_title), message = "") }
                }
                items(state.rows, key = { it.packageName }) { row -> AppRowItem(row, onSetCategory, onSchedule) }
            }
        }
    }
}

@Composable
private fun DaySelector(days: List<LocalDate>, selectedOffset: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        items(days.size) { offset ->
            val date = days[offset]
            val label = when (offset) {
                0 -> stringResource(R.string.day_today)
                1 -> stringResource(R.string.day_yesterday)
                else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + date.dayOfMonth
            }
            FilterChip(selected = offset == selectedOffset, onClick = { onSelect(offset) }, label = { Text(label) })
        }
    }
}

/** The big number for the day, and one bar split into helpful, okay and time-eater time. */
@Composable
private fun DaySummary(state: AppsUiState) {
    Panel(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = durationText(state.totalMs),
                style = MaterialTheme.typography.displaySmall,
                color = FocunoTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            state.focusScore?.let { score ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(score.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(stringResource(R.string.gauge_label), style = MaterialTheme.typography.labelSmall, color = FocunoTheme.colors.textTertiary)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val parts = listOf(
            AppCategory.PRODUCTIVE to state.productiveMs,
            AppCategory.NEUTRAL to state.neutralMs,
            AppCategory.DISTRACTING to state.distractingMs,
        )
        val total = parts.sumOf { it.second }
        Row(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(FocunoTheme.colors.trackInactive),
        ) {
            if (total > 0) {
                parts.filter { it.second > 0 }.forEach { (category, ms) ->
                    Box(Modifier.weight(ms.toFloat() / total).fillMaxHeight().background(categoryColor(category)))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            parts.forEach { (category, ms) -> Legend(categoryColor(category), categoryLabel(category), durationText(ms)) }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String, value: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Text(label, style = MaterialTheme.typography.labelMedium, color = FocunoTheme.colors.textSecondary)
        }
        Text(value, style = MaterialTheme.typography.titleSmall, color = FocunoTheme.colors.textPrimary, modifier = Modifier.padding(start = 14.dp))
    }
}

/** Tap the row to change the app's category; the lock adds a block for it. */
@Composable
private fun AppRowItem(row: AppRow, onSetCategory: (String, AppCategory) -> Unit, onSchedule: (String) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { menuOpen = true }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppIcon(row.packageName, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = FocunoTheme.colors.textPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Text(durationText(row.foregroundMs), style = MaterialTheme.typography.titleSmall, color = FocunoTheme.colors.textSecondary)
                }
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
                            .fillMaxWidth(row.fraction.coerceIn(0.02f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(categoryColor(row.category)),
                    )
                }
            }
            IconButton(onClick = { onSchedule(row.packageName) }) {
                Icon(Icons.Rounded.Lock, contentDescription = stringResource(R.string.apps_schedule_block, row.label), tint = FocunoTheme.colors.textTertiary)
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            AppCategory.entries.forEach { category ->
                DropdownMenuItem(
                    leadingIcon = { Box(Modifier.size(10.dp).background(categoryColor(category), CircleShape)) },
                    text = { Text(categoryLabel(category)) },
                    onClick = {
                        menuOpen = false
                        onSetCategory(row.packageName, category)
                    },
                )
            }
        }
    }
}
