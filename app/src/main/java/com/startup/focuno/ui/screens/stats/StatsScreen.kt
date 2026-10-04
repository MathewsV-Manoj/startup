package com.startup.focuno.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.startup.focuno.R
import com.startup.focuno.ui.components.ScreenTitle
import com.startup.focuno.ui.screens.apps.AppsScreen
import com.startup.focuno.ui.screens.insights.InsightsScreen
import com.startup.focuno.ui.theme.FocunoTheme

/** Day: the day's total and every app. Week: trends. */
@Composable
fun StatsScreen(
    onOpenHealth: () -> Unit,
    onScheduleApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var section by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize()) {
        ScreenTitle(stringResource(R.string.tab_stats))
        Segmented(
            options = listOf(stringResource(R.string.stats_day), stringResource(R.string.stats_trends)),
            selected = section,
            onSelect = { section = it },
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        )
        when (section) {
            0 -> AppsScreen(onOpenHealth = onOpenHealth, onScheduleApp = onScheduleApp)
            else -> InsightsScreen()
        }
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(FocunoTheme.colors.surfaceElevated)
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) FocunoTheme.colors.textPrimary else FocunoTheme.colors.textSecondary,
                )
            }
        }
    }
}
