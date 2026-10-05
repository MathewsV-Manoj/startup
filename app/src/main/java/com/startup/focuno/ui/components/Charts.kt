package com.startup.focuno.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.startup.focuno.ui.theme.FocunoTheme

/** Line chart of nullable values (a null is a gap, not a zero). Y axis is fixed to [minValue, maxValue]. */
@Composable
fun LineChart(
    values: List<Float?>,
    color: Color,
    modifier: Modifier = Modifier,
    minValue: Float = 0f,
    maxValue: Float = 100f,
    height: Dp = 140.dp,
    startLabel: String = "",
    endLabel: String = "",
) {
    val track = FocunoTheme.colors.trackInactive
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val strokeWidth = 3.dp.toPx()
            val pad = 6.dp.toPx()
            val w = size.width - pad * 2
            val h = size.height - pad * 2
            for (i in 0..2) {
                val y = pad + h * i / 2f
                drawLine(track, Offset(pad, y), Offset(size.width - pad, y), strokeWidth = 1.dp.toPx())
            }
            if (values.size < 2) return@Canvas
            fun x(index: Int) = pad + w * index / (values.size - 1)
            fun y(value: Float) = pad + h * (1f - ((value - minValue) / (maxValue - minValue)).coerceIn(0f, 1f))

            var path = Path()
            var open = false
            values.forEachIndexed { index, value ->
                if (value == null) {
                    if (open) {
                        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                        path = Path()
                        open = false
                    }
                } else {
                    if (!open) {
                        path.moveTo(x(index), y(value))
                        open = true
                    } else {
                        path.lineTo(x(index), y(value))
                    }
                }
            }
            if (open) drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round))
            values.forEachIndexed { index, value ->
                if (value != null) drawCircle(color, radius = 4.dp.toPx(), center = Offset(x(index), y(value)))
            }
        }
        if (startLabel.isNotEmpty() || endLabel.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(startLabel, style = MaterialTheme.typography.labelSmall, color = FocunoTheme.colors.textTertiary, modifier = Modifier.weight(1f))
                Text(endLabel, style = MaterialTheme.typography.labelSmall, color = FocunoTheme.colors.textTertiary)
            }
        }
    }
}

/** Simple bars. [highlightIndex] is drawn in [highlightColor]. */
@Composable
fun BarChart(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    highlightIndex: Int? = null,
    highlightColor: Color = color,
    startLabel: String = "",
    endLabel: String = "",
) {
    val track = FocunoTheme.colors.trackInactive
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            if (values.isEmpty()) return@Canvas
            val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
            val gap = 3.dp.toPx()
            val barWidth = (size.width - gap * (values.size - 1)) / values.size
            values.forEachIndexed { index, value ->
                val barHeight = (size.height * (value / max)).coerceAtLeast(if (value > 0f) 3.dp.toPx() else 0f)
                val left = index * (barWidth + gap)
                drawRoundRect(track, Offset(left, 0f), Size(barWidth, size.height), CornerRadius(4.dp.toPx()), alpha = 0.35f)
                if (barHeight > 0f) {
                    drawRoundRect(
                        color = if (index == highlightIndex) highlightColor else color,
                        topLeft = Offset(left, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                }
            }
        }
        if (startLabel.isNotEmpty() || endLabel.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(startLabel, style = MaterialTheme.typography.labelSmall, color = FocunoTheme.colors.textTertiary, modifier = Modifier.weight(1f))
                Text(endLabel, style = MaterialTheme.typography.labelSmall, color = FocunoTheme.colors.textTertiary)
            }
        }
    }
}
