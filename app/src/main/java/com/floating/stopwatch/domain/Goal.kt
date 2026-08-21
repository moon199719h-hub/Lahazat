package com.floating.stopwatch.domain

enum class GoalPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    CUSTOM
}

data class Goal(
    val id: String,
    val title: String,
    val targetDurationMs: Long,
    val accumulatedDurationMs: Long = 0L,
    val period: GoalPeriod = GoalPeriod.DAILY,
    val startTimestamp: Long = System.currentTimeMillis(),
    val endTimestamp: Long? = null,
    val isCompleted: Boolean = false
) {
    val progressFraction: Float
        get() = if (targetDurationMs <= 0) 0f else (accumulatedDurationMs.toFloat() / targetDurationMs.toFloat()).coerceIn(0f, 1f)

    val remainingDurationMs: Long
        get() = (targetDurationMs - accumulatedDurationMs).coerceAtLeast(0L)
}
