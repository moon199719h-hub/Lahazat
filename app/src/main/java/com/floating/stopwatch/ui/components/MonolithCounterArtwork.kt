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
fun MonolithCounterArtwork(
    counterValue: Long,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Pure black background
        drawRect(Color(0xFF000000))

        // Thin elegant framing corner lines (Monument geometry)
        val frameMargin = 40.dp.toPx()
        val cornerLength = 24.dp.toPx()
        val strokeW = 1.dp.toPx()
        val frameAlpha = 0.18f

        // Top-Left corner
        drawLine(
            color = accentColor.copy(alpha = frameAlpha),
            start = Offset(frameMargin, frameMargin),
            end = Offset(frameMargin + cornerLength, frameMargin),
            strokeWidth = strokeW
        )
        drawLine(
            color = accentColor.copy(alpha = frameAlpha),
            start = Offset(frameMargin, frameMargin),
            end = Offset(frameMargin, frameMargin + cornerLength),
            strokeWidth = strokeW
        )

        // Top-Right corner
        drawLine(
            color = accentColor.copy(alpha = frameAlpha),
            start = Offset(w - frameMargin, frameMargin),
            end = Offset(w - frameMargin - cornerLength, frameMargin),
            strokeWidth = strokeW
        )
        drawLine(
            color = accentColor.copy(alpha = frameAlpha),
            start = Offset(w - frameMargin, frameMargin),
            end = Offset(w - frameMargin, frameMargin + cornerLength),
            strokeWidth = strokeW
        )

        // Bottom-Left corner
        drawLine(
            color = accentColor.copy(alpha = frameAlpha),
            start = Offset(frameMargin, h - frameMargin),
            end = Offset(frameMargin + cornerLength, h - frameMargin),
            strokeWidth = strokeW
        )
        drawLine(
            color = accentColor.copy(alpha = frameAlpha),
            start = Offset(frameMargin, h - frameMargin),
            end = Offset(frameMargin, h - frameMargin - cornerLength),
            strokeWidth = strokeW
        )

        // Bottom-Right corner
        drawLine(
            color = accentColor.copy(alpha = frameAlpha),
            start = Offset(w - frameMargin, h - frameMargin),
            end = Offset(w - frameMargin - cornerLength, h - frameMargin),
            strokeWidth = strokeW
        )
        drawLine(
            color = accentColor.copy(alpha = frameAlpha),
            start = Offset(w - frameMargin, h - frameMargin),
            end = Offset(w - frameMargin, h - frameMargin - cornerLength),
            strokeWidth = strokeW
        )

        // Subtle center alignment cross
        val centerX = w / 2f
        val centerY = h / 2f
        drawLine(
            color = accentColor.copy(alpha = 0.08f),
            start = Offset(centerX - 12.dp.toPx(), centerY),
            end = Offset(centerX + 12.dp.toPx(), centerY),
            strokeWidth = 0.8.dp.toPx()
        )
        drawLine(
            color = accentColor.copy(alpha = 0.08f),
            start = Offset(centerX, centerY - 12.dp.toPx()),
            end = Offset(centerX, centerY + 12.dp.toPx()),
            strokeWidth = 0.8.dp.toPx()
        )
    }
}
