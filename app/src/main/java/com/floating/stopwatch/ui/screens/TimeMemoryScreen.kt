package com.floating.stopwatch.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floating.stopwatch.data.SettingsRepository
import com.floating.stopwatch.domain.Goal
import com.floating.stopwatch.domain.TimeMemory
import com.floating.stopwatch.ui.theme.LuxuryColors
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

fun parseTimeMemoriesJson(json: String): List<TimeMemory> {
    if (json.isBlank()) return emptyList()
    val list = mutableListOf<TimeMemory>()
    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                TimeMemory(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    mode = obj.getString("mode"),
                    startTimeMs = obj.getLong("startTimeMs"),
                    endTimeMs = obj.getLong("endTimeMs"),
                    durationMs = obj.getLong("durationMs"),
                    note = obj.optString("note", ""),
                    sceneName = obj.optString("sceneName", ""),
                    linkedGoalId = if (obj.has("linkedGoalId") && !obj.isNull("linkedGoalId")) obj.getString("linkedGoalId") else null,
                    createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis())
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

fun serializeTimeMemoriesJson(memories: List<TimeMemory>): String {
    val array = JSONArray()
    for (memory in memories) {
        val obj = JSONObject().apply {
            put("id", memory.id)
            put("title", memory.title)
            put("mode", memory.mode)
            put("startTimeMs", memory.startTimeMs)
            put("endTimeMs", memory.endTimeMs)
            put("durationMs", memory.durationMs)
            put("note", memory.note)
            put("sceneName", memory.sceneName)
            if (memory.linkedGoalId != null) put("linkedGoalId", memory.linkedGoalId)
            put("createdAtMs", memory.createdAtMs)
        }
        array.put(obj)
    }
    return array.toString()
}

@Composable
fun TimeMemoryScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val rawMemoriesJson by settingsRepository.timeMemoriesJson.collectAsState(initial = "")
    val memories = remember(rawMemoriesJson) { parseTimeMemoriesJson(rawMemoriesJson) }

    val rawGoalsJson by settingsRepository.goalsJson.collectAsState(initial = "")
    val goals = remember(rawGoalsJson) { parseGoalsJson(rawGoalsJson) }

    var selectedMemoryForDetail by remember { mutableStateOf<TimeMemory?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryColors.WarmBlack)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TIME MEMORY",
                    style = TextStyle(
                        color = LuxuryColors.CreamyWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraLight,
                        letterSpacing = 3.sp
                    )
                )
                Text(
                    text = "CLOSE",
                    style = TextStyle(
                        color = LuxuryColors.WarmGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.5.sp
                    ),
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(8.dp)
                )
            }

            if (memories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NO TIME MEMORIES RECORDED",
                            style = TextStyle(
                                color = LuxuryColors.WarmGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 2.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Completed Stopwatch, Countdown, or Interval sessions will appear here.",
                            style = TextStyle(
                                color = LuxuryColors.WarmGray.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(memories.sortedByDescending { it.createdAtMs }, key = { it.id }) { memory ->
                        val linkedGoal = goals.firstOrNull { g -> g.id == memory.linkedGoalId }
                        TimeMemoryCard(
                            memory = memory,
                            linkedGoalTitle = linkedGoal?.title,
                            onClick = { selectedMemoryForDetail = memory },
                            onDelete = {
                                val updated = memories.filter { m -> m.id != memory.id }
                                scope.launch { settingsRepository.setTimeMemoriesJson(serializeTimeMemoriesJson(updated)) }
                            }
                        )
                    }
                }
            }
        }

        selectedMemoryForDetail?.let { memory ->
            TimeMemoryDetailDialog(
                memory = memory,
                goals = goals,
                onDismiss = { selectedMemoryForDetail = null },
                onLinkGoal = { goalId ->
                    val updated = memories.map { m ->
                        if (m.id == memory.id) m.copy(linkedGoalId = goalId) else m
                    }
                    scope.launch { settingsRepository.setTimeMemoriesJson(serializeTimeMemoriesJson(updated)) }
                    selectedMemoryForDetail = null
                }
            )
        }
    }
}

@Composable
fun TimeMemoryCard(
    memory: TimeMemory,
    linkedGoalTitle: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM — h:mm a", Locale.getDefault()) }
    val formattedDate = remember(memory.startTimeMs) { dateFormat.format(Date(memory.startTimeMs)) }
    val durationSecs = memory.durationMs / 1000
    val mins = durationSecs / 60
    val secs = durationSecs % 60
    val formattedDur = String.format("%d min %02d sec", mins, secs)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = memory.title.uppercase(),
                        style = TextStyle(
                            color = LuxuryColors.CreamyWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    if (memory.sceneName.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[${memory.sceneName.uppercase()}]",
                            style = TextStyle(color = LuxuryColors.AccentGold, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$formattedDur • $formattedDate",
                    style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 11.sp, fontWeight = FontWeight.Light)
                )
                if (!linkedGoalTitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Linked Goal: $linkedGoalTitle",
                        style = TextStyle(color = Color(0xFF4AC98F), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    )
                }
            }

            Text(
                text = "✕",
                style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 12.sp),
                modifier = Modifier
                    .clickable { onDelete() }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun TimeMemoryDetailDialog(
    memory: TimeMemory,
    goals: List<Goal>,
    onDismiss: () -> Unit,
    onLinkGoal: (String?) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEEE, dd MMMM yyyy — h:mm a", Locale.getDefault()) }
    val formattedDate = remember(memory.startTimeMs) { dateFormat.format(Date(memory.startTimeMs)) }
    val durationSecs = memory.durationMs / 1000
    val mins = durationSecs / 60
    val secs = durationSecs % 60
    val formattedDur = String.format("%d minutes, %d seconds", mins, secs)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "MEMORY DETAILS",
                    style = TextStyle(
                        color = LuxuryColors.AccentGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "TITLE: ${memory.title}", color = LuxuryColors.CreamyWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "MODE: ${memory.mode.uppercase()}", color = LuxuryColors.WarmGray, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "DURATION: $formattedDur", color = LuxuryColors.WarmGray, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "DATE: $formattedDate", color = LuxuryColors.WarmGray, fontSize = 11.sp)

                if (memory.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "NOTE: ${memory.note}", color = LuxuryColors.CreamyWhite, fontSize = 11.sp, fontWeight = FontWeight.Normal)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "LINK TO TIME GOAL",
                    style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 10.sp, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (goals.isEmpty()) {
                    Text(text = "No Goals created yet.", color = LuxuryColors.WarmGray.copy(alpha = 0.6f), fontSize = 11.sp)
                } else {
                    goals.forEach { goal ->
                        val isLinked = memory.linkedGoalId == goal.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLinkGoal(if (isLinked) null else goal.id) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = goal.title, color = LuxuryColors.CreamyWhite, fontSize = 12.sp)
                            Text(
                                text = if (isLinked) "LINKED ✓" else "+ LINK",
                                color = if (isLinked) Color(0xFF4AC98F) else LuxuryColors.AccentGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = LuxuryColors.AccentGold),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("CLOSE", color = LuxuryColors.WarmBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
