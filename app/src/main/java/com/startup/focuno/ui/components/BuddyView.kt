package com.startup.focuno.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.startup.focuno.ui.theme.ElectricCyan
import com.startup.focuno.ui.theme.FocunoTheme
import com.startup.focuno.ui.theme.NeonMagenta
import com.startup.focuno.ui.theme.NeonViolet

enum class BuddyMood { HAPPY, OKAY, SLEEPY, EXCITED }

private val Ink = Color(0xFF1A0F2E)
private val Gold = Color(0xFFFFD166)
private val Cheek = Color(0x66FF8FB1)

/**
 * The Focuno buddy, drawn from shapes. It bounces and blinks, its face follows [mood], and it gains a sprout
 * (level 3), a star antenna (level 6) and a crown (level 9) as the level grows.
 */
@Composable
fun BuddyView(mood: BuddyMood, level: Int, modifier: Modifier = Modifier, size: Dp = 180.dp, description: String = "") {
    val transition = rememberInfiniteTransition(label = "buddy")
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bounce",
    )
    val blink by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3_600
                0f at 0
                0f at 3_300
                1f at 3_400
                0f at 3_520
            },
        ),
        label = "blink",
    )

    Canvas(modifier.size(size).semantics { if (description.isNotEmpty()) contentDescription = description }) {
        val w = this.size.width
        val cx = w / 2f
        val bodyR = w * 0.30f
        val cy = w * 0.60f - bounce * w * 0.025f

        drawOval(Color(0x33000000), Offset(cx - bodyR * 0.85f, w * 0.90f), Size(bodyR * 1.7f, w * 0.055f))

        when {
            level >= 9 -> drawCrown(cx, cy - bodyR, bodyR)
            level >= 6 -> drawStarAntenna(cx, cy - bodyR, bodyR)
            level >= 3 -> drawSprout(cx, cy - bodyR, bodyR)
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NeonMagenta, NeonViolet),
                center = Offset(cx - bodyR * 0.35f, cy - bodyR * 0.45f),
                radius = bodyR * 1.7f,
            ),
            radius = bodyR,
            center = Offset(cx, cy),
        )
        drawOval(Color(0x2EFFFFFF), Offset(cx - bodyR * 0.55f, cy + bodyR * 0.05f), Size(bodyR * 1.1f, bodyR * 0.85f))

        drawFace(mood, cx, cy, bodyR, blink)
    }
}

private fun DrawScope.drawFace(mood: BuddyMood, cx: Float, cy: Float, bodyR: Float, blink: Float) {
    val eyeY = cy - bodyR * 0.12f
    val eyeDx = bodyR * 0.42f
    val eyeR = bodyR * 0.2f
    val stroke = Stroke(width = bodyR * 0.07f, cap = StrokeCap.Round)

    for (side in listOf(-1f, 1f)) {
        val ex = cx + side * eyeDx
        if (mood == BuddyMood.SLEEPY || blink > 0.6f) {
            drawArc(Ink, 0f, 180f, false, Offset(ex - eyeR, eyeY - eyeR * 0.5f), Size(eyeR * 2f, eyeR), style = stroke)
        } else {
            drawOval(Color.White, Offset(ex - eyeR, eyeY - eyeR), Size(eyeR * 2f, eyeR * 2f))
            val look = if (mood == BuddyMood.EXCITED) 0f else side * eyeR * 0.1f
            drawCircle(Ink, eyeR * 0.58f, Offset(ex + look, eyeY + eyeR * 0.08f))
            drawCircle(Color.White, eyeR * 0.2f, Offset(ex + look + eyeR * 0.2f, eyeY - eyeR * 0.15f))
        }
        drawCircle(Cheek, eyeR * 0.75f, Offset(cx + side * bodyR * 0.62f, cy + bodyR * 0.22f))
    }

    val mouthY = cy + bodyR * 0.28f
    val mouthW = bodyR * 0.3f
    when (mood) {
        BuddyMood.HAPPY -> drawPath(
            Path().apply {
                moveTo(cx - mouthW, mouthY)
                quadraticTo(cx, mouthY + mouthW * 1.1f, cx + mouthW, mouthY)
            },
            Ink,
            style = stroke,
        )
        BuddyMood.EXCITED -> drawArc(Ink, 0f, 180f, true, Offset(cx - mouthW, mouthY - mouthW * 0.4f), Size(mouthW * 2f, mouthW * 1.5f))
        BuddyMood.OKAY -> drawLine(Ink, Offset(cx - mouthW * 0.7f, mouthY + mouthW * 0.2f), Offset(cx + mouthW * 0.7f, mouthY + mouthW * 0.2f), bodyR * 0.07f, StrokeCap.Round)
        BuddyMood.SLEEPY -> drawCircle(Ink, mouthW * 0.35f, Offset(cx, mouthY + mouthW * 0.35f))
    }
}

private fun DrawScope.drawSprout(cx: Float, topY: Float, bodyR: Float) {
    drawLine(ElectricCyan, Offset(cx, topY + bodyR * 0.05f), Offset(cx, topY - bodyR * 0.25f), bodyR * 0.06f, StrokeCap.Round)
    for (side in listOf(-1f, 1f)) {
        rotate(degrees = side * 35f, pivot = Offset(cx, topY - bodyR * 0.25f)) {
            drawOval(ElectricCyan, Offset(cx - bodyR * 0.1f + side * bodyR * 0.14f, topY - bodyR * 0.52f), Size(bodyR * 0.2f, bodyR * 0.34f))
        }
    }
}

private fun DrawScope.drawStarAntenna(cx: Float, topY: Float, bodyR: Float) {
    drawLine(Color.White, Offset(cx, topY + bodyR * 0.05f), Offset(cx, topY - bodyR * 0.3f), bodyR * 0.05f, StrokeCap.Round)
    drawCircle(Gold, bodyR * 0.15f, Offset(cx, topY - bodyR * 0.38f))
    drawCircle(Color.White, bodyR * 0.05f, Offset(cx - bodyR * 0.04f, topY - bodyR * 0.42f))
}

private fun DrawScope.drawCrown(cx: Float, topY: Float, bodyR: Float) {
    val base = topY + bodyR * 0.08f
    val h = bodyR * 0.42f
    val half = bodyR * 0.38f
    drawPath(
        Path().apply {
            moveTo(cx - half, base)
            lineTo(cx - half, base - h * 0.55f)
            lineTo(cx - half * 0.5f, base - h * 0.2f)
            lineTo(cx, base - h)
            lineTo(cx + half * 0.5f, base - h * 0.2f)
            lineTo(cx + half, base - h * 0.55f)
            lineTo(cx + half, base)
            close()
        },
        Gold,
    )
    drawCircle(Color(0xFFFF5C7A), bodyR * 0.045f, Offset(cx, base - h * 0.55f))
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0417)
@Composable
private fun BuddyPreview() {
    FocunoTheme { BuddyView(mood = BuddyMood.HAPPY, level = 9) }
}
