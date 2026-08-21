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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun AtmosphereArtwork(
    artMode: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "AtmosphereMotion")

    val motionPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "MotionPhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (artMode.uppercase()) {
            "HERITAGE" -> {
                // Near-black base with delicate geometric engraving frame
                drawRect(Color(0xFF030303))
                val margin = 28.dp.toPx()
                val alpha = 0.15f

                // Outer border line
                drawLine(
                    color = Color(0xFFC9A66B).copy(alpha = alpha),
                    start = Offset(margin, margin),
                    end = Offset(w - margin, margin),
                    strokeWidth = 0.8.dp.toPx()
                )
                drawLine(
                    color = Color(0xFFC9A66B).copy(alpha = alpha),
                    start = Offset(margin, margin),
                    end = Offset(margin, h - margin),
                    strokeWidth = 0.8.dp.toPx()
                )
                drawLine(
                    color = Color(0xFFC9A66B).copy(alpha = alpha),
                    start = Offset(w - margin, margin),
                    end = Offset(w - margin, h - margin),
                    strokeWidth = 0.8.dp.toPx()
                )
                drawLine(
                    color = Color(0xFFC9A66B).copy(alpha = alpha),
                    start = Offset(margin, h - margin),
                    end = Offset(w - margin, h - margin),
                    strokeWidth = 0.8.dp.toPx()
                )
            }
            "INK" -> {
                // Black base with ultra-subtle organic ink trace wave
                drawRect(Color(0xFF000000))
                val waveY = h * 0.5f + kotlin.math.sin(motionPhase.toDouble()).toFloat() * 12.dp.toPx()
                drawCircle(
                    color = Color(0xFF18181B).copy(alpha = 0.4f),
                    radius = w * 0.45f,
                    center = Offset(w * 0.5f, waveY)
                )
            }
            "SILK" -> {
                // Black silk surface with ultra-fine tonal highlight shift
                drawRect(Color(0xFF020202))
                val highlightAlpha = (0.04f + kotlin.math.sin(motionPhase.toDouble()).toFloat() * 0.02f).coerceIn(0.01f, 0.08f)
                drawCircle(
                    color = Color(0xFF27272A).copy(alpha = highlightAlpha),
                    radius = w * 0.65f,
                    center = Offset(w * 0.3f, h * 0.3f)
                )
            }
            "PAPER" -> {
                // Warm paper background surface
                drawRect(Color(0xFFF7F5F0))
            }
            "OBSIDIAN" -> {
                // Pure deep obsidian black
                drawRect(Color(0xFF000000))
            }
            else -> {
                // Default Void black
                drawRect(Color(0xFF000000))
            }
        }
    }
}
