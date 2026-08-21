package com.floating.stopwatch.domain

import org.junit.Assert.*
import org.junit.Test

class LegacyMilestoneDetectorTest {

    @Test
    fun testMilestoneDetectionAndIdempotency() {
        val legacy = TimeLegacy(
            id = "legacy_test",
            title = "Testing Milestones",
            targetDurationMillis = 100000L,
            goals = listOf(LegacyGoal(id = "g1", title = "G1", targetDurationMillis = 100000L, actualDurationMillis = 50000L))
        )

        val moments = LegacyMilestoneDetector.detectNewMilestones(legacy)
        val initialTitles = moments.map { it.title }

        assertTrue(initialTitles.contains("Legacy Journey Initialized"))
        assertTrue(initialTitles.contains("Halfway Milestone (50%)"))

        // Re-detecting on legacy that already includes these moments must return NO new duplicate moments
        val updatedLegacy = legacy.copy(moments = legacy.moments + moments)
        val newMoments = LegacyMilestoneDetector.detectNewMilestones(updatedLegacy)

        assertTrue(newMoments.isEmpty())
    }
}
