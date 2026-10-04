package com.startup.focuno.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.startup.focuno.R
import com.startup.focuno.ui.theme.FocunoTheme

/** Root composable. Becomes the bottom-nav host in Phase 5. */
@Composable
fun FocunoApp(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Phase0Placeholder(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun Phase0Placeholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.placeholder_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = FocunoTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        PaletteSwatches()
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.placeholder_status),
            style = MaterialTheme.typography.labelMedium,
            color = FocunoTheme.colors.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PaletteSwatches() {
    val swatches = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        FocunoTheme.colors.productive,
        FocunoTheme.colors.warning,
        FocunoTheme.colors.distracting,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        swatches.forEach { color ->
            Box(
                Modifier
                    .size(20.dp)
                    .background(color, CircleShape),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0417)
@Composable
private fun FocunoAppPreview() {
    FocunoTheme {
        FocunoApp()
    }
}
