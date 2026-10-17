package com.focuno.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    OnboardingScreen(
        onFinish = { goal ->
            viewModel.finish(goal)
            onFinished()
        },
    )
}

@Composable
fun OnboardingScreen(onFinish: (Int) -> Unit, modifier: Modifier = Modifier) {
    var goal by remember { mutableIntStateOf(120) }
    val options = listOf(60, 90, 120, 180, 240)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Welcome to Focuno", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "Focuno learns your attention patterns on-device. Nothing leaves your phone.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(32.dp))
        Text("Daily focus goal", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { minutes ->
                FilterChip(
                    selected = goal == minutes,
                    onClick = { goal = minutes },
                    label = { Text("$minutes min") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onFinish(goal) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start focusing")
        }
    }
}
