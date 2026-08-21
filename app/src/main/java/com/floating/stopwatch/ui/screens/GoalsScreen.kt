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
import com.floating.stopwatch.domain.GoalPeriod
import com.floating.stopwatch.ui.theme.LuxuryColors
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

fun parseGoalsJson(json: String): List<Goal> {
    if (json.isBlank()) return emptyList()
    val list = mutableListOf<Goal>()
    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                Goal(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    targetDurationMs = obj.getLong("targetDurationMs"),
                    accumulatedDurationMs = obj.optLong("accumulatedDurationMs", 0L),
                    period = try { GoalPeriod.valueOf(obj.getString("period")) } catch (e: Exception) { GoalPeriod.DAILY },
                    startTimestamp = obj.optLong("startTimestamp", System.currentTimeMillis()),
                    endTimestamp = if (obj.has("endTimestamp") && !obj.isNull("endTimestamp")) obj.getLong("endTimestamp") else null,
                    isCompleted = obj.optBoolean("isCompleted", false)
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

fun serializeGoalsJson(goals: List<Goal>): String {
    val array = JSONArray()
    for (goal in goals) {
        val obj = JSONObject().apply {
            put("id", goal.id)
            put("title", goal.title)
            put("targetDurationMs", goal.targetDurationMs)
            put("accumulatedDurationMs", goal.accumulatedDurationMs)
            put("period", goal.period.name)
            put("startTimestamp", goal.startTimestamp)
            if (goal.endTimestamp != null) put("endTimestamp", goal.endTimestamp)
            put("isCompleted", goal.isCompleted)
        }
        array.put(obj)
    }
    return array.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val rawGoalsJson by settingsRepository.goalsJson.collectAsState(initial = "")
    val goals = remember(rawGoalsJson) { parseGoalsJson(rawGoalsJson) }

    var showCreateDialog by remember { mutableStateOf(false) }

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
                    text = "TIME GOALS",
                    style = TextStyle(
                        color = LuxuryColors.CreamyWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraLight,
                        letterSpacing = 3.sp
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "+ NEW GOAL",
                        style = TextStyle(
                            color = LuxuryColors.AccentGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        modifier = Modifier
                            .clickable { showCreateDialog = true }
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
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
            }

            if (goals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NO ACTIVE TIME GOALS",
                            style = TextStyle(
                                color = LuxuryColors.WarmGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 2.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Set a daily, weekly, or monthly goal to track structured progress.",
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(goals, key = { it.id }) { goal ->
                        GoalCard(
                            goal = goal,
                            onDelete = {
                                val updated = goals.filter { g -> g.id != goal.id }
                                scope.launch { settingsRepository.setGoalsJson(serializeGoalsJson(updated)) }
                            }
                        )
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateGoalDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { newGoal ->
                    val updated = goals + newGoal
                    scope.launch { settingsRepository.setGoalsJson(serializeGoalsJson(updated)) }
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
fun GoalCard(
    goal: Goal,
    onDelete: () -> Unit
) {
    val accHours = (goal.accumulatedDurationMs / 3600000f)
    val targetHours = (goal.targetDurationMs / 3600000f)
    val percent = (goal.progressFraction * 100).toInt()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (goal.isCompleted) Color(0xFF4AC98F).copy(alpha = 0.4f) else LuxuryColors.AccentGold.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.title.uppercase(),
                    style = TextStyle(
                        color = LuxuryColors.CreamyWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (goal.isCompleted) "COMPLETED" else goal.period.name,
                        style = TextStyle(
                            color = if (goal.isCompleted) Color(0xFF4AC98F) else LuxuryColors.AccentGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "✕",
                        style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 12.sp),
                        modifier = Modifier
                            .clickable { onDelete() }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { goal.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (goal.isCompleted) Color(0xFF4AC98F) else LuxuryColors.AccentGold,
                trackColor = Color.White.copy(alpha = 0.08f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format("%.1fh / %.1fh (%d%%)", accHours, targetHours, percent),
                    style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 11.sp)
                )
                val remHours = (goal.remainingDurationMs / 3600000f)
                Text(
                    text = if (goal.isCompleted) "GOAL MET" else String.format("%.1fh left", remHours),
                    style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 11.sp)
                )
            }
        }
    }
}

@Composable
fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onCreate: (Goal) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetHoursText by remember { mutableStateOf("2") }
    var selectedPeriod by remember { mutableStateOf(GoalPeriod.DAILY) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "CREATE TIME GOAL",
                    style = TextStyle(
                        color = LuxuryColors.AccentGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title (e.g. Study, Work)", color = LuxuryColors.WarmGray, fontSize = 10.sp) },
                    textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetHoursText,
                    onValueChange = { targetHoursText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Target Duration (Hours)", color = LuxuryColors.WarmGray, fontSize = 10.sp) },
                    textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "PERIOD",
                    style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 10.sp, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GoalPeriod.values().forEach { period ->
                        val isSel = selectedPeriod == period
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) LuxuryColors.AccentGold.copy(alpha = 0.15f) else Color(0xFF141414)
                            ),
                            border = BorderStroke(1.dp, if (isSel) LuxuryColors.AccentGold else Color.White.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPeriod = period }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = period.name.take(3),
                                    style = TextStyle(
                                        color = if (isSel) LuxuryColors.CreamyWhite else LuxuryColors.WarmGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "CANCEL",
                        color = LuxuryColors.WarmGray,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val hrs = targetHoursText.toFloatOrNull() ?: 2f
                            val targetMs = (hrs * 3600000L).toLong().coerceAtLeast(60000L)
                            val goal = Goal(
                                id = UUID.randomUUID().toString(),
                                title = title.ifBlank { "Daily Focus" },
                                targetDurationMs = targetMs,
                                period = selectedPeriod
                            )
                            onCreate(goal)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryColors.AccentGold)
                    ) {
                        Text("CREATE", color = LuxuryColors.WarmBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
