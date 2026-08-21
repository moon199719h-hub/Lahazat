package com.floating.stopwatch.domain

data class LegacyProgressResult(
    val plannedTimeMillis: Long,
    val actualTimeMillis: Long,
    val timerSessionsTimeMillis: Long,
    val manualEntriesTimeMillis: Long,
    val remainingTimeMillis: Long,
    val progressFraction: Float,
    val expectedProgressFraction: Float,
    val varianceMillis: Long,
    val totalDays: Int,
    val elapsedDays: Int,
    val remainingDays: Int,
    val requiredDailyTargetMillis: Long,
    val status: LegacyProgressStatus
)

data class LegacyRecoveryResult(
    val hoursBehind: Float,
    val daysRemaining: Int,
    val requiredDailyAverageMillis: Long,
    val currentDailyAverageMillis: Long,
    val suggestion: String
)

object LegacyProgressCalculator {

    fun calculateProgress(legacy: TimeLegacy, currentTimeMillis: Long = System.currentTimeMillis()): LegacyProgressResult {
        val planned = legacy.targetDurationMillis.coerceAtLeast(0L)
        val timerSessionTime = legacy.moments.filter { it.durationMillis > 0L }.sumOf { it.durationMillis }
        val manualTime = legacy.manualEntries.sumOf { it.durationMillis }
        val goalsTime = legacy.goals.sumOf { it.actualDurationMillis }

        val actual = (timerSessionTime + manualTime + goalsTime).coerceAtLeast(0L)
        val remaining = (planned - actual).coerceAtLeast(0L)

        val progressFraction = if (planned > 0L) {
            (actual.toFloat() / planned.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        val addedPostponeDays = legacy.postponements.sumOf { it.addedDays }
        val totalDays = (legacy.daysCount + addedPostponeDays).coerceAtLeast(1)

        val start = legacy.startAt ?: legacy.createdAt
        val end = legacy.endAt ?: (start + 86400000L * totalDays)
        val totalSpan = (end - start).coerceAtLeast(1L)
        val elapsedSpan = (currentTimeMillis - start).coerceIn(0L, totalSpan)

        val expectedProgressFraction = (elapsedSpan.toFloat() / totalSpan.toFloat()).coerceIn(0f, 1f)
        val expectedActual = (planned * expectedProgressFraction).toLong()
        val varianceMillis = actual - expectedActual

        val elapsedDays = (elapsedSpan / 86400000L).toInt().coerceIn(0, totalDays)
        val remainingDays = (totalDays - elapsedDays).coerceAtLeast(0)

        val requiredDailyTargetMillis = if (remainingDays > 0) {
            remaining / remainingDays
        } else {
            remaining
        }

        val status = when {
            actual >= planned && planned > 0L -> LegacyProgressStatus.COMPLETED
            currentTimeMillis > end && actual < planned -> LegacyProgressStatus.OVERDUE
            varianceMillis >= 3600000L -> LegacyProgressStatus.ON_TRACK
            varianceMillis >= -3600000L -> LegacyProgressStatus.IN_PROGRESS
            else -> LegacyProgressStatus.AT_RISK
        }

        return LegacyProgressResult(
            plannedTimeMillis = planned,
            actualTimeMillis = actual,
            timerSessionsTimeMillis = timerSessionTime,
            manualEntriesTimeMillis = manualTime,
            remainingTimeMillis = remaining,
            progressFraction = progressFraction,
            expectedProgressFraction = expectedProgressFraction,
            varianceMillis = varianceMillis,
            totalDays = totalDays,
            elapsedDays = elapsedDays,
            remainingDays = remainingDays,
            requiredDailyTargetMillis = requiredDailyTargetMillis,
            status = status
        )
    }

    fun calculateRecovery(legacy: TimeLegacy, currentTimeMillis: Long = System.currentTimeMillis()): LegacyRecoveryResult {
        val progress = calculateProgress(legacy, currentTimeMillis)
        val remainingDays = progress.remainingDays.coerceAtLeast(1)
        val elapsedDays = progress.elapsedDays.coerceAtLeast(1)

        val hoursBehind = if (progress.varianceMillis < 0) {
            Math.abs(progress.varianceMillis).toFloat() / 3600000f
        } else {
            0f
        }

        val requiredDailyAverageMillis = progress.remainingTimeMillis / remainingDays
        val currentDailyAverageMillis = progress.actualTimeMillis / elapsedDays

        val suggestion = when {
            progress.status == LegacyProgressStatus.COMPLETED -> "Goal achieved! Outstanding execution."
            hoursBehind == 0f -> "On track! Maintain your current daily pace."
            hoursBehind <= 2f -> "Slightly behind. Add ~${String.format("%.1f", hoursBehind)} hours this week to recover."
            else -> "Behind schedule. Aim for ${requiredDailyAverageMillis / 3600000L}h/day to reach your target."
        }

        return LegacyRecoveryResult(
            hoursBehind = hoursBehind,
            daysRemaining = remainingDays,
            requiredDailyAverageMillis = requiredDailyAverageMillis,
            currentDailyAverageMillis = currentDailyAverageMillis,
            suggestion = suggestion
        )
    }
}
