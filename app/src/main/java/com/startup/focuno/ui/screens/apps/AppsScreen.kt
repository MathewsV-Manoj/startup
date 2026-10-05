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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startup.focuno.R
import com.startup.focuno.domain.model.AppCategory
import com.startup.focuno.ui.components.AppIcon
import com.startup.focuno.ui.components.BarChart
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
        onSchedule = onScheduleApp,
        onRetry = viewModel::refresh,
        onOpenHealth = onOpenHealth,
        onOpenDetail = viewModel::openDetail,
        modifier = modifier,
    )
    state.detail?.let { detail ->
        AppDetailSheet(
            detail = detail,
            onDismiss = viewModel::closeDetail,
            onSetCategory = viewModel::setCategory,
            onSetMindful = viewModel::setMindful,
            onLimitOrBlock = {
                viewModel.closeDetail()
                onScheduleApp(detail.packageName)
            },
        )
    }
}

@Composable
fun AppsContent(
    state: AppsUiState,
    onSelectDay: (Int) -> Unit,
    onSchedule: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenHealth: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenDetail: (AppRow) -> Unit = {},
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
                items(state.rows, key = { it.packageName }) { row -> AppRowItem(row, onOpenDetail, onSchedule) }
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
            Column(Modifier.weight(1f)) {
                Text(
                    text = durationText(state.totalMs),
                    style = MaterialTheme.typography.displaySmall,
                    color = FocunoTheme.colors.textPrimary,
                )
                state.previousTotalMs?.let { previous -> ChangeLine(state.totalMs, previous, today = state.dayOffset == 0) }
            }
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

/** Tap the row for the app's week and options; the lock goes straight to limiting or blocking it. */
@Composable
private fun AppRowItem(row: AppRow, onOpen: (AppRow) -> Unit, onSchedule: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onOpen(row) }
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
}

/** "↓ 20% vs yesterday" in green, or "↑ 15% vs yesterday" in amber. */
@Composable
private fun ChangeLine(totalMs: Long, previousMs: Long, today: Boolean) {
    val percent = ((totalMs - previousMs) * 100 / previousMs).toInt()
    val down = percent <= 0
    Text(
        stringResource(
            if (down) {
                if (today) R.string.change_down_yesterday else R.string.change_down_previous
            } else {
                if (today) R.string.change_up_yesterday else R.string.change_up_previous
            },
            kotlin.math.abs(percent),
        ),
        style = MaterialTheme.typography.labelLarge,
        color = if (down) FocunoTheme.colors.productive else FocunoTheme.colors.warning,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDetailSheet(
    detail: AppDetail,
    onDismiss: () -> Unit,
    onSetCategory: (String, AppCategory) -> Unit,
    onSetMindful: (String, Boolean) -> Unit,
    onLimitOrBlock: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FocunoTheme.colors.surfaceElevated,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AppIcon(detail.packageName, size = 48.dp)
                Column(Modifier.weight(1f)) {
                    Text(detail.label, style = MaterialTheme.typography.titleLarge, color = FocunoTheme.colors.textPrimary, maxLines = 1)
                    Text(
                        pluralStringResource(R.plurals.app_opens, detail.dayOpens, detail.dayOpens),
                        style = MaterialTheme.typography.labelLarge,
                        color = FocunoTheme.colors.textSecondary,
                    )
                }
                Text(durationText(detail.dayMs), style = MaterialTheme.typography.headlineSmall, color = FocunoTheme.colors.textPrimary)
            }
            Column {
                Text(stringResource(R.string.detail_week), style = MaterialTheme.typography.labelLarge, color = FocunoTheme.colors.textTertiary)
                Spacer(Modifier.height(8.dp))
                BarChart(
                    values = detail.week.map { it.second / 60_000f },
                    color = categoryColor(detail.category).copy(alpha = 0.5f),
                    height = 96.dp,
                    highlightIndex = detail.week.lastIndex,
                    highlightColor = categoryColor(detail.category),
                    startLabel = detail.week.first().first.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    endLabel = stringResource(R.string.day_today),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(AppCategory.PRODUCTIVE, AppCategory.NEUTRAL, AppCategory.DISTRACTING).forEach { category ->
                    val selected = category == detail.category
                    val color = categoryColor(category)
                    Text(
                        categoryLabel(category),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) FocunoTheme.colors.textPrimary else FocunoTheme.colors.textSecondary,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) color.copy(alpha = 0.35f) else FocunoTheme.colors.trackInactive)
                            .clickable { onSetCategory(detail.packageName, category) }
                            .padding(vertical = 10.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.when_mindful),
                    style = MaterialTheme.typography.titleMedium,
                    color = FocunoTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = detail.mindful, onCheckedChange = { onSetMindful(detail.packageName, it) })
            }
            Button(onClick = onLimitOrBlock, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Rounded.Lock, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.detail_limit_or_block))
            }
        }
    }
}
