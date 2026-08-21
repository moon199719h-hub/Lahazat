package com.floating.stopwatch.domain

enum class LegacyStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    ARCHIVED
}

enum class LegacyProgressStatus {
    NOT_STARTED,
    IN_PROGRESS,
    ON_TRACK,
    AT_RISK,
    COMPLETED,
    OVERDUE
}

data class LegacyPhase(
    val id: String,
    val title: String,
    val startAt: Long? = null,
    val endAt: Long? = null,
    val targetDurationMillis: Long = 0L,
    val order: Int = 0
)

data class LegacyGoal(
    val id: String,
    val title: String,
    val targetDurationMillis: Long = 0L,
    val actualDurationMillis: Long = 0L,
    val phaseId: String? = null
)

data class LegacyMoment(
    val id: String,
    val legacyId: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String = "",
    val durationMillis: Long = 0L
)

data class LegacyJournalEntry(
    val id: String,
    val legacyId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val text: String
)

data class TimeLegacy(
    val id: String,
    val title: String,
    val description: String = "",
    val startAt: Long? = null,
    val endAt: Long? = null,
    val targetDurationMillis: Long = 0L,
    val status: LegacyStatus = LegacyStatus.ACTIVE,
    val phases: List<LegacyPhase> = emptyList(),
    val goals: List<LegacyGoal> = emptyList(),
    val moments: List<LegacyMoment> = emptyList(),
    val journalEntries: List<LegacyJournalEntry> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
