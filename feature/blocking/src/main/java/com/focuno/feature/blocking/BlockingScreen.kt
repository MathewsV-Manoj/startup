package com.focuno.feature.blocking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focuno.core.model.BlockRule

@Composable
fun BlockingRoute(
    modifier: Modifier = Modifier,
    viewModel: BlockingViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    BlockingScreen(
        rules = rules,
        onToggle = viewModel::setEnabled,
        modifier = modifier,
    )
}

@Composable
fun BlockingScreen(
    rules: List<BlockRule>,
    onToggle: (BlockRule, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Text(text = "App blocking", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Blocking is opt-in. Focuno never restricts an app unless you ask it to.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        if (rules.isEmpty()) {
            Text("No rules yet. Add one to get started.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rules, key = { it.packageName }) { rule ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(rule.packageName)
                                Text("${rule.startTime} – ${rule.endTime}")
                            }
                            Switch(
                                checked = rule.enabled,
                                onCheckedChange = { onToggle(rule, it) },
                            )
                        }
                    }
                }
            }
        }
    }
}
