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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ChronoNoirArtwork(
    elapsedTimeMs: Long,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val baseRadius = size.width.coerceAtMost(size.height) * 0.38f

        // Deep black background
        drawRect(Color(0xFF000000))

        // Thin outer precision ring
        drawCircle(
            color = accentColor.copy(alpha = 0.12f),
            radius = baseRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.dp.toPx())
        )

        // Subtle 60 fine tick marks
        for (i in 0 until 60) {
            val angleRad = Math.toRadians((i * 6 - 90).toDouble()).toFloat()
            val isMajor = i % 5 == 0
            val tickLength = if (isMajor) 10.dp.toPx() else 4.dp.toPx()
            val strokeW = if (isMajor) 1.5.dp.toPx() else 0.8.dp.toPx()
            val alpha = if (isMajor) 0.25f else 0.1f

            val startX = centerX + (baseRadius - tickLength) * cos(angleRad)
            val startY = centerY + (baseRadius - tickLength) * sin(angleRad)
            val endX = centerX + baseRadius * cos(angleRad)
            val endY = centerY + baseRadius * sin(angleRad)

            drawLine(
                color = accentColor.copy(alpha = alpha),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeW
            )
        }

        // Sub-second precision sweep arc based on current second progress
        val secondProgress = (elapsedTimeMs % 60000L) / 60000f
        val sweepAngle = secondProgress * 360f

        drawArc(
            color = accentColor.copy(alpha = 0.4f),
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(centerX - baseRadius, centerY - baseRadius),
            size = Size(baseRadius * 2f, baseRadius * 2f),
            style = Stroke(width = 1.2.dp.toPx())
        )

        // Micro indicator dot
        val dotAngleRad = Math.toRadians((sweepAngle - 90f).toDouble()).toFloat()
        val dotX = centerX + baseRadius * cos(dotAngleRad)
        val dotY = centerY + baseRadius * sin(dotAngleRad)

        drawCircle(
            color = accentColor.copy(alpha = 0.85f),
            radius = 2.5.dp.toPx(),
            center = Offset(dotX, dotY)
        )
    }
}
