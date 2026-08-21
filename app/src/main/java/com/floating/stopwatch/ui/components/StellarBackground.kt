package com.floating.stopwatch.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

private data class Star(
    val normalizedX: Float,
    val normalizedY: Float,
    val radius: Float,
    val baseAlpha: Float,
    val layer: Int, // 0: Distant, 1: Mid, 2: Near
    val pulsePhaseOffset: Float
)

private data class Meteor(
    val startX: Float,
    val startY: Float,
    val dx: Float,
    val dy: Float,
    val length: Float,
    var progress: Float
)

@Composable
fun StellarBackground(
    modifier: Modifier = Modifier,
    enablePulse: Boolean = true
) {
    // Seeded deterministic star layout (3 depth layers: Distant, Mid, Near)
    val stars = remember {
        val random = Random(1337)
        val list = mutableListOf<Star>()

        // Layer 0: Distant (70 stars, tiny 0.5-1.0dp, low opacity 0.12-0.30)
        for (i in 0 until 70) {
            list.add(
                Star(
                    normalizedX = random.nextFloat(),
                    normalizedY = random.nextFloat(),
                    radius = random.nextFloat() * 0.5f + 0.5f,
                    baseAlpha = random.nextFloat() * 0.18f + 0.12f,
                    layer = 0,
                    pulsePhaseOffset = 0f
                )
            )
        }

        // Layer 1: Mid (25 stars, 1.2-1.8dp, opacity 0.25-0.55)
        for (i in 0 until 25) {
            list.add(
                Star(
                    normalizedX = random.nextFloat(),
                    normalizedY = random.nextFloat(),
                    radius = random.nextFloat() * 0.6f + 1.2f,
                    baseAlpha = random.nextFloat() * 0.3f + 0.25f,
                    layer = 1,
                    pulsePhaseOffset = random.nextFloat() * 6.283185f
                )
            )
        }

        // Layer 2: Near (5 stars, 2.0-2.5dp, opacity 0.60-0.85)
        for (i in 0 until 5) {
            list.add(
                Star(
                    normalizedX = random.nextFloat(),
                    normalizedY = random.nextFloat(),
                    radius = random.nextFloat() * 0.5f + 2.0f,
                    baseAlpha = random.nextFloat() * 0.25f + 0.60f,
                    layer = 2,
                    pulsePhaseOffset = random.nextFloat() * 6.283185f
                )
            )
        }

        list.toList()
    }

    val pulseTransition = rememberInfiniteTransition(label = "StellarPulse")
    val pulseFactor by if (enablePulse) {
        pulseTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6.283185f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "PulseFactor"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    // Rare single meteor simulator (~1 every 10 seconds)
    var activeMeteor by remember { mutableStateOf<Meteor?>(null) }

    LaunchedEffect(Unit) {
        val meteorRandom = Random(42)
        while (true) {
            delay(8000L + meteorRandom.nextInt(4000).toLong()) // ~10 seconds interval
            val startX = meteorRandom.nextFloat() * 0.8f + 0.1f
            val startY = meteorRandom.nextFloat() * 0.3f
            val angle = 0.6f + meteorRandom.nextFloat() * 0.4f // downward diagonal
            val dx = kotlin.math.cos(angle.toDouble()).toFloat() * 180f
            val dy = kotlin.math.sin(angle.toDouble()).toFloat() * 180f
            val length = 90f + meteorRandom.nextFloat() * 50f

            val meteor = Meteor(startX, startY, dx, dy, length, 0f)
            val steps = 24
            for (step in 0..steps) {
                meteor.progress = step.toFloat() / steps.toFloat()
                activeMeteor = meteor
                delay(25L)
            }
            activeMeteor = null
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Pure AMOLED Black Background (NO blue/navy tint)
        drawRect(Color(0xFF000000))

        // Draw cached stars
        stars.forEach { star ->
            val px = star.normalizedX * w
            val py = star.normalizedY * h

            val alpha = if (star.layer > 0 && enablePulse) {
                val sinVal = kotlin.math.sin((pulseFactor + star.pulsePhaseOffset).toDouble()).toFloat()
                (star.baseAlpha + sinVal * 0.12f).coerceIn(0.08f, 0.90f)
            } else {
                star.baseAlpha
            }

            drawCircle(
                color = Color(0xFFF7F5F0).copy(alpha = alpha),
                radius = star.radius,
                center = Offset(px, py)
            )
        }

        // Draw active meteor trail if present
        activeMeteor?.let { m ->
            val originX = m.startX * w
            val originY = m.startY * h
            val currentX = originX + m.dx * m.progress
            val currentY = originY + m.dy * m.progress
            val tailX = currentX - (m.dx * 0.35f)
            val tailY = currentY - (m.dy * 0.35f)

            val trailAlpha = (1f - m.progress) * 0.45f

            drawLine(
                color = Color(0xFFFFFFFF).copy(alpha = trailAlpha.coerceIn(0f, 0.5f)),
                start = Offset(tailX, tailY),
                end = Offset(currentX, currentY),
                strokeWidth = 1.2.dp.toPx()
            )
        }
    }
}
