package com.floating.stopwatch.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.floating.stopwatch.ui.theme.LuxuryColors
import kotlin.math.roundToInt

@Composable
fun LuxuryToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = LuxuryColors.AccentGold,
    trackWidth: Dp = 36.dp,
    trackHeight: Dp = 18.dp,
    thumbSize: Dp = 12.dp
) {
    val interactionSource = remember { MutableInteractionSource() }

    val trackColor by animateColorAsState(
        targetValue = if (checked) accentColor.copy(alpha = 0.85f) else Color(0xFF202022),
        animationSpec = tween(durationMillis = 150),
        label = "TrackColor"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) LuxuryColors.WarmBlack else Color(0xFF8E8E93),
        animationSpec = tween(durationMillis = 150),
        label = "ThumbColor"
    )

    val thumbOffsetPx by animateFloatAsState(
        targetValue = if (checked) 18f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "ThumbOffset"
    )

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(trackHeight / 2))
            .background(trackColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffsetPx.roundToInt(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}
