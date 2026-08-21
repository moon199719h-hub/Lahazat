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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

private data class Star(
    val normalizedX: Float,
    val normalizedY: Float,
    val radius: Float,
    val baseAlpha: Float,
    val isSignature: Boolean,
    val pulsePhaseOffset: Float
)

@Composable
fun StellarBackground(
    modifier: Modifier = Modifier,
    enablePulse: Boolean = true
) {
    // Deterministic seeded star generation (80 micro-stars, 25 secondary stars, 5 signature stars)
    val stars = remember {
        val random = Random(1337)
        val list = mutableListOf<Star>()

        // Layer 1: Micro Stars (80 points, tiny, low opacity)
        for (i in 0 until 80) {
            list.add(
                Star(
                    normalizedX = random.nextFloat(),
                    normalizedY = random.nextFloat(),
                    radius = random.nextFloat() * 0.8f + 0.6f, // 0.6dp to 1.4dp
                    baseAlpha = random.nextFloat() * 0.25f + 0.15f, // 0.15 to 0.40
                    isSignature = false,
                    pulsePhaseOffset = 0f
                )
            )
        }

        // Layer 2: Secondary Stars (25 points, slightly larger, slightly brighter)
        for (i in 0 until 25) {
            list.add(
                Star(
                    normalizedX = random.nextFloat(),
                    normalizedY = random.nextFloat(),
                    radius = random.nextFloat() * 0.8f + 1.4f, // 1.4dp to 2.2dp
                    baseAlpha = random.nextFloat() * 0.35f + 0.35f, // 0.35 to 0.70
                    isSignature = false,
                    pulsePhaseOffset = 0f
                )
            )
        }

        // Layer 3: Signature Stars (5 points, soft glow)
        for (i in 0 until 5) {
            list.add(
                Star(
                    normalizedX = random.nextFloat(),
                    normalizedY = random.nextFloat(),
                    radius = random.nextFloat() * 0.6f + 2.2f, // 2.2dp to 2.8dp
                    baseAlpha = random.nextFloat() * 0.2f + 0.7f, // 0.70 to 0.90
                    isSignature = true,
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

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Deep night atmosphere background brush (AMOLED true black with charcoal/navy depth)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF030712), // Subtle charcoal navy center
                    Color(0xFF000000)  // Absolute black edge
                ),
                radius = w.coerceAtLeast(h) * 0.85f
            )
        )

        // Draw cached stars
        stars.forEach { star ->
            val px = star.normalizedX * w
            val py = star.normalizedY * h

            val alpha = if (star.isSignature && enablePulse) {
                val sinVal = kotlin.math.sin(pulseFactor + star.pulsePhaseOffset)
                (star.baseAlpha + sinVal * 0.15f).coerceIn(0.1f, 0.95f)
            } else {
                star.baseAlpha
            }

            if (star.isSignature) {
                // Soft outer glow for signature stars
                drawCircle(
                    color = Color(0xFFD4AF37).copy(alpha = alpha * 0.25f),
                    radius = star.radius * 2.8f,
                    center = androidx.compose.ui.geometry.Offset(px, py)
                )
            }

            drawCircle(
                color = Color(0xFFF7F5F0).copy(alpha = alpha),
                radius = star.radius,
                center = androidx.compose.ui.geometry.Offset(px, py)
            )
        }
    }
}
