package com.startup.focuno.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.startup.focuno.R
import com.startup.focuno.ui.theme.FocunoTheme
import kotlin.math.roundToInt

private const val START_ANGLE = 135f
private const val SWEEP_ANGLE = 270f

/** Circular score gauge. Counts up on load. A null score shows a dash, never a perfect-looking 100. */
@Composable
fun FocusGauge(score: Int?, modifier: Modifier = Modifier, size: Dp = 224.dp, compact: Boolean = false) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(score) {
        progress.animateTo(
            targetValue = (score ?: 0).toFloat(),
            animationSpec = tween(durationMillis = 1_400, easing = EaseOutCubic),
        )
    }
    val colors = FocunoTheme.colors
    val description = if (score == null) stringResource(R.string.gauge_no_data) else stringResource(R.string.gauge_description, score)

    Box(modifier = modifier.size(size).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = (if (compact) 11.dp else 18.dp).toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = colors.trackInactive,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            val sweep = SWEEP_ANGLE * (progress.value / 100f)
            if (score != null && sweep > 0.5f) {
                // The sweep gradient starts at 3 o'clock, so rotate the canvas to start the arc at 135 degrees.
                rotate(degrees = START_ANGLE, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colorStops = arrayOf(0f to colors.gaugeStart, 0.75f to colors.gaugeEnd, 1f to colors.gaugeEnd),
                            center = center,
                        ),
                        startAngle = 0f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (score == null) stringResource(R.string.gauge_dash) else progress.value.roundToInt().toString(),
                style = if (compact) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayLarge,
                color = colors.textPrimary,
            )
            if (!compact) {
                Text(
                    text = stringResource(R.string.gauge_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0417)
@Composable
private fun FocusGaugePreview() {
    FocunoTheme { FocusGauge(score = 78) }
}
