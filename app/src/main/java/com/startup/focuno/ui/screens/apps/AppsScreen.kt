package com.startup.focuno.ui.screens.apps

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.domain.model.AppCategory
import com.startup.focuno.ui.components.AppIcon
import com.startup.focuno.ui.components.CategoryChip
import com.startup.focuno.ui.components.EmptyState
import com.startup.focuno.ui.components.ErrorState
import com.startup.focuno.ui.components.GlassCard
import com.startup.focuno.ui.components.LoadingState
import com.startup.focuno.ui.components.RefreshWhileResumed
import com.startup.focuno.ui.components.ScreenTitle
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
    RefreshWhileResumed(intervalMs = 60_000L, onRefresh = viewModel::refresh)
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
        ScreenTitle(stringResource(R.string.tab_apps))
        if (state.days.isNotEmpty()) DaySelector(state.days, state.dayOffset, onSelectDay)
        when {
            state.isLoading -> LoadingState()
            !state.hasUsageAccess -> EmptyState(
                icon = Icons.Rounded.Apps,
                title = stringResource(R.string.apps_no_access_title),
                message = stringResource(R.string.apps_no_access_message),
                actionLabel = stringResource(R.string.focus_allow_usage_access),
                onAction = onOpenHealth,
            )
            state.hasError -> ErrorState(stringResource(R.string.error_generic), onRetry)
            state.rows.isEmpty() -> EmptyState(
                icon = Icons.Rounded.Apps,
                title = stringResource(R.string.apps_empty_title),
                message = stringResource(R.string.apps_empty_message),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.rows, key = { it.packageName }) { row ->
                    AppRowCard(row, onSetCategory, onSchedule)
                }
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

@Composable
private fun AppRowCard(row: AppRow, onSetCategory: (String, AppCategory) -> Unit, onSchedule: (String) -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppIcon(row.packageName, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(row.label, style = MaterialTheme.typography.titleSmall, color = FocunoTheme.colors.textPrimary, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategorySelector(row, onSetCategory)
                    Text(
                        text = pluralStringResource(R.plurals.opens_count, row.openCount, row.openCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = FocunoTheme.colors.textSecondary,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(durationText(row.foregroundMs), style = MaterialTheme.typography.titleSmall, color = FocunoTheme.colors.textPrimary)
            }
            IconButton(onClick = { onSchedule(row.packageName) }) {
                Icon(Icons.Rounded.Lock, contentDescription = stringResource(R.string.apps_schedule_block, row.label), tint = FocunoTheme.colors.textSecondary)
            }
        }
        Spacer(Modifier.height(10.dp))
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
}

@Composable
private fun CategorySelector(row: AppRow, onSetCategory: (String, AppCategory) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        CategoryChip(row.category, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(categoryLabel(category)) },
                    onClick = {
                        expanded = false
                        onSetCategory(row.packageName, category)
                    },
                )
            }
        }
    }
}
