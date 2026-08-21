package com.floating.stopwatch.domain

import org.junit.Assert.*
import org.junit.Test

class LegacyProgressCalculatorTest {

    @Test
    fun testNormalProgressCalculation() {
        val start = 1000000L
        val end = start + 86400000L * 10 // 10 days
        val legacy = TimeLegacy(
            id = "test_1",
            title = "Test Legacy",
            startAt = start,
            endAt = end,
            targetDurationMillis = 36000000L, // 10 hours
            goals = listOf(
                LegacyGoal(id = "g1", title = "Goal 1", targetDurationMillis = 36000000L, actualDurationMillis = 18000000L) // 5 hours done
            )
        )

        val midPoint = start + 86400000L * 5 // Day 5
        val result = LegacyProgressCalculator.calculateProgress(legacy, currentTimeMillis = midPoint)

        assertEquals(36000000L, result.plannedTimeMillis)
        assertEquals(18000000L, result.actualTimeMillis)
        assertEquals(18000000L, result.remainingTimeMillis)
        assertEquals(0.5f, result.progressFraction, 0.01f)
        assertEquals(0.5f, result.expectedProgressFraction, 0.01f)
        assertEquals(0L, result.varianceMillis)
        assertEquals(LegacyProgressStatus.IN_PROGRESS, result.status)
    }

    @Test
    fun testCompletedStatus() {
        val legacy = TimeLegacy(
            id = "test_comp",
            title = "Completed Legacy",
            targetDurationMillis = 10000L,
            goals = listOf(
                LegacyGoal(id = "g1", title = "Goal 1", targetDurationMillis = 10000L, actualDurationMillis = 10000L)
            )
        )

        val result = LegacyProgressCalculator.calculateProgress(legacy)
        assertEquals(LegacyProgressStatus.COMPLETED, result.status)
        assertEquals(1.0f, result.progressFraction, 0.01f)
    }

    @Test
    fun testZeroTargetEdgeCase() {
        val legacy = TimeLegacy(
            id = "test_zero",
            title = "Zero Legacy",
            targetDurationMillis = 0L
        )

        val result = LegacyProgressCalculator.calculateProgress(legacy)
        assertEquals(0L, result.plannedTimeMillis)
        assertEquals(0f, result.progressFraction, 0.01f)
    }

    @Test
    fun testRecoveryCalculatorWhenBehind() {
        val start = 1000000L
        val end = start + 86400000L * 10
        val legacy = TimeLegacy(
            id = "test_rec",
            title = "Behind Legacy",
            startAt = start,
            endAt = end,
            targetDurationMillis = 36000000L, // 10h target
            goals = listOf(
                LegacyGoal(id = "g1", title = "Goal 1", targetDurationMillis = 36000000L, actualDurationMillis = 3600000L) // 1h done
            )
        )

        val midPoint = start + 86400000L * 5 // Should be at 5h by Day 5
        val recovery = LegacyProgressCalculator.calculateRecovery(legacy, currentTimeMillis = midPoint)

        assertTrue(recovery.hoursBehind > 3f)
        assertEquals(5, recovery.daysRemaining)
        assertTrue(recovery.suggestion.contains("Behind schedule"))
    }
}
