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
import androidx.compose.ui.graphics.StrokeCap
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
    val totalLength: Float,
    var progress: Float
)

@Composable
fun StellarBackground(
    modifier: Modifier = Modifier,
    enablePulse: Boolean = true
) {
    // Seeded deterministic star layout across 3 depth layers
    val stars = remember {
        val random = Random(1337)
        val list = mutableListOf<Star>()

        // Layer 0: Distant (100 stars, tiny 0.4-0.9dp, low opacity 0.10-0.28)
        for (i in 0 until 100) {
            list.add(
                Star(
                    normalizedX = random.nextFloat(),
                    normalizedY = random.nextFloat(),
                    radius = random.nextFloat() * 0.5f + 0.4f,
                    baseAlpha = random.nextFloat() * 0.18f + 0.10f,
                    layer = 0,
                    pulsePhaseOffset = 0f
                )
            )
        }

        // Layer 1: Mid (35 stars, 1.1-1.7dp, opacity 0.22-0.52)
        for (i in 0 until 35) {
            list.add(
                Star(
                    normalizedX = random.nextFloat(),
                    normalizedY = random.nextFloat(),
                    radius = random.nextFloat() * 0.6f + 1.1f,
                    baseAlpha = random.nextFloat() * 0.3f + 0.22f,
                    layer = 1,
                    pulsePhaseOffset = random.nextFloat() * 6.283185f
                )
            )
        }

        // Layer 2: Near (8 signature stars, 1.8-2.4dp, opacity 0.55-0.82)
        for (i in 0 until 8) {
            list.add(
                Star(
                    normalizedX = random.nextFloat(),
                    normalizedY = random.nextFloat(),
                    radius = random.nextFloat() * 0.6f + 1.8f,
                    baseAlpha = random.nextFloat() * 0.27f + 0.55f,
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
            delay(8000L + meteorRandom.nextInt(4000).toLong()) // ~10s interval
            val startX = meteorRandom.nextFloat() * 0.7f + 0.1f
            val startY = meteorRandom.nextFloat() * 0.25f
            val angle = 0.65f + meteorRandom.nextFloat() * 0.35f // downward diagonal
            val dx = kotlin.math.cos(angle.toDouble()).toFloat() * 220f
            val dy = kotlin.math.sin(angle.toDouble()).toFloat() * 220f
            val totalLength = 120f + meteorRandom.nextFloat() * 60f

            val meteor = Meteor(startX, startY, dx, dy, totalLength, 0f)
            val steps = 28
            for (step in 0..steps) {
                meteor.progress = step.toFloat() / steps.toFloat()
                activeMeteor = meteor
                delay(20L)
            }
            activeMeteor = null
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Pure AMOLED Black Background (NO blue/navy/purple tint)
        drawRect(Color(0xFF000000))

        val currentMeteor = activeMeteor

        // Draw cached stars
        stars.forEach { star ->
            val px = star.normalizedX * w
            val py = star.normalizedY * h

            var alpha = if (star.layer > 0 && enablePulse) {
                val sinVal = kotlin.math.sin((pulseFactor + star.pulsePhaseOffset).toDouble()).toFloat()
                (star.baseAlpha + sinVal * 0.12f).coerceIn(0.08f, 0.88f)
            } else {
                star.baseAlpha
            }

            // Subtle nearby-star brightness response during meteor pass
            if (currentMeteor != null) {
                val meteorHeadX = currentMeteor.startX * w + currentMeteor.dx * currentMeteor.progress
                val meteorHeadY = currentMeteor.startY * h + currentMeteor.dy * currentMeteor.progress
                val distSq = (px - meteorHeadX) * (px - meteorHeadX) + (py - meteorHeadY) * (py - meteorHeadY)
                if (distSq < 10000f) { // Within 100px
                    alpha = (alpha + 0.15f * (1f - distSq / 10000f)).coerceAtMost(0.95f)
                }
            }

            drawCircle(
                color = Color(0xFFF7F5F0).copy(alpha = alpha),
                radius = star.radius,
                center = Offset(px, py)
            )
        }

        // Draw active meteor trail if present
        currentMeteor?.let { m ->
            val headX = m.startX * w + m.dx * m.progress
            val headY = m.startY * h + m.dy * m.progress

            val alphaFade = (1f - m.progress).coerceIn(0.1f, 1.0f)

            // Tapered multi-layer tail
            val tail1X = headX - m.dx * 0.22f
            val tail1Y = headY - m.dy * 0.22f

            val tail2X = headX - m.dx * 0.45f
            val tail2Y = headY - m.dy * 0.45f

            // Outer soft halo line
            drawLine(
                color = Color(0xFFFFFFFF).copy(alpha = 0.15f * alphaFade),
                start = Offset(tail2X, tail2Y),
                end = Offset(headX, headY),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Inner bright core trail line
            drawLine(
                color = Color(0xFFF7F5F0).copy(alpha = 0.60f * alphaFade),
                start = Offset(tail1X, tail1Y),
                end = Offset(headX, headY),
                strokeWidth = 1.0.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Luminous meteor head core
            drawCircle(
                color = Color(0xFFFFFFFF).copy(alpha = 0.90f * alphaFade),
                radius = 1.8.dp.toPx(),
                center = Offset(headX, headY)
            )
        }
    }
}
