package com.startup.focuno.ui.screens.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.startup.focuno.R
import com.startup.focuno.ui.components.ScreenTitle
import com.startup.focuno.ui.components.Segmented
import com.startup.focuno.ui.screens.apps.AppsScreen
import com.startup.focuno.ui.screens.insights.InsightsScreen

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
