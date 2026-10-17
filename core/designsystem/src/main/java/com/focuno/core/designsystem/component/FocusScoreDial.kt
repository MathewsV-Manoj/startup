package com.focuno.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Reusable 0..100 focus score dial used across Focus and Analytics screens. */
@Composable
fun FocusScoreDial(
    score: Int,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 180.dp,
) {
    val target = (score.coerceIn(0, 100) / 100f)
    val animated by animateFloatAsState(targetValue = target, label = "focusScore")

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { animated },
            modifier = Modifier.size(size),
            strokeWidth = 14.dp,
            strokeCap = StrokeCap.Round,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
