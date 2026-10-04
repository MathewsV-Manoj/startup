package com.startup.focuno.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.random.Random

private val ConfettiColors = listOf(
    Color(0xFF9D4EDD),
    Color(0xFFC77DFF),
    Color(0xFF64FFDA),
    Color(0xFFFFD166),
    Color(0xFFFF5C7A),
)

private data class Piece(val x: Float, val speed: Float, val offset: Float, val size: Float, val color: Color, val spin: Float, val round: Boolean)

/** Falling confetti, drawn with a handful of shapes and one looping animation. */
@Composable
fun Confetti(modifier: Modifier = Modifier, count: Int = 42) {
    val pieces = remember {
        val random = Random(7)
        List(count) {
            Piece(
                x = random.nextFloat(),
                speed = 0.6f + random.nextFloat() * 0.8f,
                offset = random.nextFloat(),
                size = 6f + random.nextFloat() * 8f,
                color = ConfettiColors[random.nextInt(ConfettiColors.size)],
                spin = random.nextFloat() * 360f,
                round = random.nextBoolean(),
            )
        }
    }
    val progress by rememberInfiniteTransition(label = "confetti").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_200, easing = LinearEasing), RepeatMode.Restart),
        label = "fall",
    )
    Canvas(modifier) {
        pieces.forEach { p ->
            val y = ((progress * p.speed + p.offset) % 1f) * (size.height + 40f) - 20f
            val x = p.x * size.width
            val sizePx = p.size * density
            if (p.round) {
                drawCircle(p.color, sizePx / 2f, Offset(x, y))
            } else {
                rotate(p.spin + progress * 360f, pivot = Offset(x, y)) {
                    drawRect(p.color, Offset(x - sizePx / 2f, y - sizePx / 4f), Size(sizePx, sizePx / 2f))
                }
            }
        }
    }
}

/** A happy pop-up with the buddy, confetti and one big button. */
@Composable
fun CelebrationDialog(
    title: String,
    message: String,
    buttonLabel: String,
    level: Int,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val scale = remember { Animatable(0.6f) }
        LaunchedEffect(Unit) {
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Confetti(Modifier.fillMaxSize())
            Panel(
                modifier = Modifier.padding(28.dp).fillMaxWidth().scale(scale.value),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    BuddyView(mood = BuddyMood.EXCITED, level = level, size = 150.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text(buttonLabel, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
