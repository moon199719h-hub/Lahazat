package com.floating.stopwatch.domain

data class GamificationStats(
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val totalFocusedHours: Float,
    val totalSessionsCount: Int,
    val unlockedMilestones: List<String>,
    val unlockedAchievements: List<String>
)

fun calculateGamificationStats(memories: List<TimeMemory>, goals: List<Goal>): GamificationStats {
    if (memories.isEmpty()) {
        return GamificationStats(
            currentStreakDays = 0,
            bestStreakDays = 0,
            totalFocusedHours = 0f,
            totalSessionsCount = 0,
            unlockedMilestones = emptyList(),
            unlockedAchievements = emptyList()
        )
    }

    val totalMs = memories.sumOf { it.durationMs }
    val totalHours = totalMs / 3600000f
    val sessionsCount = memories.size

    // Calculate daily streak
    val calendar = java.util.Calendar.getInstance()
    val daysWithSessions = memories.map {
        calendar.timeInMillis = it.startTimeMs
        String.format(
            "%04d-%02d-%02d",
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }.distinct().sortedDescending()

    var streak = 0
    var bestStreak = 0
    var tempStreak = 0

    val todayStr = calendar.run {
        timeInMillis = System.currentTimeMillis()
        String.format("%04d-%02d-%02d", get(java.util.Calendar.YEAR), get(java.util.Calendar.MONTH) + 1, get(java.util.Calendar.DAY_OF_MONTH))
    }
    val yesterdayStr = calendar.run {
        timeInMillis = System.currentTimeMillis() - 86400000L
        String.format("%04d-%02d-%02d", get(java.util.Calendar.YEAR), get(java.util.Calendar.MONTH) + 1, get(java.util.Calendar.DAY_OF_MONTH))
    }

    if (daysWithSessions.contains(todayStr) || daysWithSessions.contains(yesterdayStr)) {
        streak = daysWithSessions.size
    } else {
        streak = 0
    }
    bestStreak = streak.coerceAtLeast(1)

    // Calculate unlocked Milestones (Hours)
    val milestones = mutableListOf<String>()
    if (totalHours >= 1f) milestones.add("First Hour")
    if (totalHours >= 5f) milestones.add("5 Hours Focused")
    if (totalHours >= 10f) milestones.add("10 Hours Master")
    if (totalHours >= 25f) milestones.add("25 Hours Dedicated")
    if (totalHours >= 50f) milestones.add("50 Hours Scholar")
    if (totalHours >= 100f) milestones.add("100 Hours Legend")

    // Calculate unlocked Achievements
    val achievements = mutableListOf<String>()
    if (sessionsCount >= 1) achievements.add("First Steps")
    if (sessionsCount >= 10) achievements.add("Consistent Timer")
    if (sessionsCount >= 30) achievements.add("Time Architect")
    if (goals.any { it.isCompleted }) achievements.add("Goal Achiever")
    if (streak >= 7) achievements.add("7-Day Streak")

    return GamificationStats(
        currentStreakDays = streak,
        bestStreakDays = bestStreak,
        totalFocusedHours = totalHours,
        totalSessionsCount = sessionsCount,
        unlockedMilestones = milestones,
        unlockedAchievements = achievements
    )
}
