package com.floating.stopwatch.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp

@Composable
fun HorizonCountdownArtwork(
    remainingMs: Long,
    initialMs: Long,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val horizonY = h * 0.58f

        // Deep black background
        drawRect(Color(0xFF000000))

        // Thin horizontal horizon line
        drawLine(
            color = accentColor.copy(alpha = 0.2f),
            start = Offset(0f, horizonY),
            end = Offset(w, horizonY),
            strokeWidth = 1.dp.toPx()
        )

        // Calculate progress convergence (0f = start, 1f = complete)
        val progress = if (initialMs > 0L) {
            (1f - (remainingMs.toFloat() / initialMs.toFloat())).coerceIn(0f, 1f)
        } else 0f

        // Converging perspective lines toward distant center target
        val centerX = w / 2f
        val startLeftX = w * (0.1f + progress * 0.35f)
        val startRightX = w * (0.9f - progress * 0.35f)

        // Left perspective guide
        drawLine(
            color = accentColor.copy(alpha = 0.15f),
            start = Offset(startLeftX, h),
            end = Offset(centerX, horizonY),
            strokeWidth = 1.dp.toPx()
        )

        // Right perspective guide
        drawLine(
            color = accentColor.copy(alpha = 0.15f),
            start = Offset(startRightX, h),
            end = Offset(centerX, horizonY),
            strokeWidth = 1.dp.toPx()
        )

        // Distant minimal target marker
        drawCircle(
            color = accentColor.copy(alpha = 0.6f + progress * 0.35f),
            radius = (2f + progress * 2f).dp.toPx(),
            center = Offset(centerX, horizonY)
        )
    }
}
