package com.focuno.feature.analytics

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focuno.core.model.FocusScore

@Composable
fun AnalyticsRoute(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AnalyticsScreen(state = state, modifier = modifier)
}

@Composable
fun AnalyticsScreen(state: AnalyticsUiState, modifier: Modifier = Modifier) {
    val today = state.today
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Text(text = "Analytics", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        if (today == null) {
            Text("No score yet today. Open the app and let usage sync.")
        } else {
            Text("Score breakdown", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            ScoreComponent("Deep work", today.deepWorkRatio)
            ScoreComponent("Switch stability", today.switchStability)
            ScoreComponent("Distraction resistance", today.distractionResistance)
            ScoreComponent("Consistency", today.consistency)
        }

        Spacer(Modifier.height(24.dp))
        Text("Last 30 days", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.recent) { score ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(score.date.toString())
                    Text(score.score.toString())
                }
            }
        }
    }
}

@Composable
private fun ScoreComponent(label: String, value: Float) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text("${(value * 100).toInt()}%")
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
