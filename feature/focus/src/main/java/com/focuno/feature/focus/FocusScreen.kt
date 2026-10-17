package com.focuno.feature.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focuno.core.designsystem.component.FocusScoreDial
import com.focuno.core.designsystem.theme.FocunoTheme
import com.focuno.core.model.AppCategory
import com.focuno.core.model.AppUsage
import java.time.Duration
import java.time.LocalDate

@Composable
fun FocusRoute(
    modifier: Modifier = Modifier,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FocusScreen(state = state, modifier = modifier)
}

@Composable
fun FocusScreen(state: FocusUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Today", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        FocusScoreDial(score = state.score)
        Spacer(Modifier.height(8.dp))
        Text(text = state.band.name.lowercase().replaceFirstChar { it.uppercase() })
        Spacer(Modifier.height(24.dp))
        Text(
            text = "${state.screenMinutes} min screen · goal ${state.goalMinutes} min",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Top apps",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.topApps, key = { it.packageName }) { usage ->
                AppUsageRow(usage)
            }
        }
    }
}

@Composable
private fun AppUsageRow(usage: AppUsage) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = usage.appLabel ?: usage.packageName)
            Text(text = "${usage.totalTimeInForeground.toMinutes()} min")
        }
    }
}

@Preview
@Composable
private fun FocusScreenPreview() {
    FocunoTheme(dynamicColor = false) {
        FocusScreen(
            state = FocusUiState(
                loading = false,
                score = 72,
                screenMinutes = 143,
                topApps = listOf(
                    AppUsage("com.notion.id", "Notion", LocalDate.now(), Duration.ofMinutes(60), 12, AppCategory.PRODUCTIVE),
                    AppUsage("com.instagram.android", "Instagram", LocalDate.now(), Duration.ofMinutes(40), 30, AppCategory.DISTRACTING),
                ),
            ),
        )
    }
}
