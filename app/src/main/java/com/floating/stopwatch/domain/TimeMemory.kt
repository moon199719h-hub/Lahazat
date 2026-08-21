package com.floating.stopwatch.domain

data class TimeMemory(
    val id: String,
    val title: String,
    val mode: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val note: String = "",
    val sceneName: String = "",
    val linkedGoalId: String? = null,
    val createdAtMs: Long = System.currentTimeMillis()
)
