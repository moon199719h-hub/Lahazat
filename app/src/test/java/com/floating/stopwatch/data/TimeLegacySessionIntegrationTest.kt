package com.floating.stopwatch.data

import com.floating.stopwatch.domain.LegacyGoal
import com.floating.stopwatch.domain.LegacyMilestoneDetector
import com.floating.stopwatch.domain.LegacyProgressCalculator
import com.floating.stopwatch.domain.LegacyProgressStatus
import com.floating.stopwatch.domain.TimeLegacy
import org.junit.Assert.*
import org.junit.Test

class TimeLegacySessionIntegrationTest {

    @Test
    fun testSessionRecordingUpdatesLegacyProgressAndMilestones() {
        val legacy = TimeLegacy(
            id = "leg_1",
            title = "Test Session Legacy",
            targetDurationMillis = 36000000L, // 10 hours
            goals = listOf(LegacyGoal(id = "g1", title = "Main Target", targetDurationMillis = 36000000L, actualDurationMillis = 0L))
        )

        val sessionDuration = 18000000L // 5 hours session
        val updatedGoals = legacy.goals.map { it.copy(actualDurationMillis = it.actualDurationMillis + sessionDuration) }
        val updatedLegacy = legacy.copy(goals = updatedGoals)

        val progress = LegacyProgressCalculator.calculateProgress(updatedLegacy)

        assertEquals(0.5f, progress.progressFraction, 0.01f)
        assertEquals(18000000L, progress.actualTimeMillis)

        val milestones = LegacyMilestoneDetector.detectNewMilestones(updatedLegacy)
        val milestoneTitles = milestones.map { it.title }

        assertTrue(milestoneTitles.contains("Quarter Milestone (25%)"))
        assertTrue(milestoneTitles.contains("Halfway Milestone (50%)"))
    }
}
