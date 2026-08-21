package com.floating.stopwatch.domain

data class TimeScene(
    val id: String,
    val name: String,
    val mode: String, // "stopwatch", "countdown", "interval"
    val presetDurationMs: Long = 1800000L, // default 30 mins
    val soundBehavior: String = "Click",
    val hapticBehavior: String = "Medium",
    val floatingOpacity: Float = 0.85f,
    val linkedGoalId: String? = null
) {
    companion object {
        val PREDEFINED_SCENES = listOf(
            TimeScene(id = "focus", name = "FOCUS", mode = "countdown", presetDurationMs = 1500000L, soundBehavior = "Click", hapticBehavior = "Medium"),
            TimeScene(id = "study", name = "STUDY", mode = "countdown", presetDurationMs = 3000000L, soundBehavior = "Click", hapticBehavior = "Light"),
            TimeScene(id = "workout", name = "WORKOUT", mode = "interval", presetDurationMs = 2400000L, soundBehavior = "Click", hapticBehavior = "Strong"),
            TimeScene(id = "relax", name = "RELAX", mode = "stopwatch", presetDurationMs = 600000L, soundBehavior = "None", hapticBehavior = "Light")
        )
    }
}
