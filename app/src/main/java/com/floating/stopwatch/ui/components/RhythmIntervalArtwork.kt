package com.floating.stopwatch.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun RhythmIntervalArtwork(
    currentRound: Int,
    totalRounds: Int,
    isWorkPhase: Boolean,
    stageProgressFraction: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.width.coerceAtMost(size.height) * 0.36f

        // Deep black background
        drawRect(Color(0xFF000000))

        if (totalRounds <= 0) return@Canvas

        // Segmented ring representing interval rounds
        val segmentAngle = 360f / totalRounds
        val gapAngle = 4f
        val drawAngle = segmentAngle - gapAngle

        for (i in 0 until totalRounds) {
            val startAngle = -90f + i * segmentAngle + (gapAngle / 2f)
            val isCurrent = (i + 1) == currentRound
            val isPassed = (i + 1) < currentRound

            val alpha = when {
                isCurrent -> 0.85f
                isPassed -> 0.35f
                else -> 0.12f
            }

            val strokeWidth = if (isCurrent) 2.5.dp.toPx() else 1.2.dp.toPx()
            val color = if (isCurrent && !isWorkPhase) Color(0xFF6B6661) else accentColor

            drawArc(
                color = color.copy(alpha = alpha),
                startAngle = startAngle,
                sweepAngle = drawAngle,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokeWidth)
            )

            // Draw current round active phase progress sweep
            if (isCurrent) {
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = drawAngle * stageProgressFraction.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth * 1.5f)
                )
            }
        }
    }
}
