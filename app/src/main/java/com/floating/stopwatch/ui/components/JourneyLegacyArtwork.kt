package com.floating.stopwatch.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun JourneyLegacyArtwork(
    progressFraction: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val lineY = h * 0.65f
        val startX = w * 0.12f
        val endX = w * 0.88f
        val currentX = startX + (endX - startX) * progressFraction.coerceIn(0f, 1f)

        // Pure black background
        drawRect(Color(0xFF000000))

        // Total Journey Path Line (Quiet track)
        drawLine(
            color = accentColor.copy(alpha = 0.15f),
            start = Offset(startX, lineY),
            end = Offset(endX, lineY),
            strokeWidth = 1.dp.toPx()
        )

        // Completed Journey Path Line (Active gold track)
        drawLine(
            color = accentColor.copy(alpha = 0.85f),
            start = Offset(startX, lineY),
            end = Offset(currentX, lineY),
            strokeWidth = 1.5.dp.toPx()
        )

        // Start node
        drawCircle(
            color = accentColor.copy(alpha = 0.5f),
            radius = 3.dp.toPx(),
            center = Offset(startX, lineY)
        )

        // Milestone nodes along journey line (at 25%, 50%, 75%)
        val milestones = listOf(0.25f, 0.50f, 0.75f)
        milestones.forEach { m ->
            val mX = startX + (endX - startX) * m
            val isPassed = progressFraction >= m
            drawCircle(
                color = if (isPassed) accentColor else accentColor.copy(alpha = 0.2f),
                radius = 2.5.dp.toPx(),
                center = Offset(mX, lineY)
            )
        }

        // Target End node
        drawCircle(
            color = accentColor.copy(alpha = 0.5f),
            radius = 3.5.dp.toPx(),
            center = Offset(endX, lineY),
            style = Stroke(width = 1.dp.toPx())
        )

        // Current progress traveler node
        drawCircle(
            color = accentColor,
            radius = 5.dp.toPx(),
            center = Offset(currentX, lineY)
        )
    }
}
