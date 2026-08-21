package com.floating.stopwatch.domain

import java.util.UUID

object LegacyMilestoneDetector {

    fun detectNewMilestones(legacy: TimeLegacy, currentTimeMillis: Long = System.currentTimeMillis()): List<LegacyMoment> {
        val existingTitles = legacy.moments.map { it.title }.toSet()
        val newMoments = mutableListOf<LegacyMoment>()

        val progress = LegacyProgressCalculator.calculateProgress(legacy, currentTimeMillis)

        // Start milestone
        if ("Legacy Journey Initialized" !in existingTitles) {
            newMoments.add(
                LegacyMoment(
                    id = UUID.randomUUID().toString(),
                    legacyId = legacy.id,
                    title = "Legacy Journey Initialized",
                    timestamp = legacy.createdAt,
                    message = "Commenced ${legacy.title}"
                )
            )
        }

        // 25% progress
        if (progress.progressFraction >= 0.25f && "Quarter Milestone (25%)" !in existingTitles) {
            newMoments.add(
                LegacyMoment(
                    id = UUID.randomUUID().toString(),
                    legacyId = legacy.id,
                    title = "Quarter Milestone (25%)",
                    timestamp = currentTimeMillis,
                    message = "Achieved 25% of overall target time"
                )
            )
        }

        // 50% progress
        if (progress.progressFraction >= 0.50f && "Halfway Milestone (50%)" !in existingTitles) {
            newMoments.add(
                LegacyMoment(
                    id = UUID.randomUUID().toString(),
                    legacyId = legacy.id,
                    title = "Halfway Milestone (50%)",
                    timestamp = currentTimeMillis,
                    message = "Halfway through your legacy journey"
                )
            )
        }

        // 75% progress
        if (progress.progressFraction >= 0.75f && "Three-Quarter Milestone (75%)" !in existingTitles) {
            newMoments.add(
                LegacyMoment(
                    id = UUID.randomUUID().toString(),
                    legacyId = legacy.id,
                    title = "Three-Quarter Milestone (75%)",
                    timestamp = currentTimeMillis,
                    message = "75% completion achieved"
                )
            )
        }

        // 100% completion milestone
        if (progress.progressFraction >= 1.0f && "Legacy Completion (100%)" !in existingTitles) {
            newMoments.add(
                LegacyMoment(
                    id = UUID.randomUUID().toString(),
                    legacyId = legacy.id,
                    title = "Legacy Completion (100%)",
                    timestamp = currentTimeMillis,
                    message = "Target time fully accomplished!"
                )
            )
        }

        return newMoments
    }
}
