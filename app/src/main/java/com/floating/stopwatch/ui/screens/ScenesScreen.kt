package com.floating.stopwatch.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.floating.stopwatch.domain.TimeScene
import com.floating.stopwatch.ui.theme.LuxuryColors
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

fun parseCustomScenesJson(json: String): List<TimeScene> {
    if (json.isBlank()) return emptyList()
    val list = mutableListOf<TimeScene>()
    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                TimeScene(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    mode = obj.getString("mode"),
                    presetDurationMs = obj.optLong("presetDurationMs", 1800000L),
                    soundBehavior = obj.optString("soundBehavior", "Click"),
                    hapticBehavior = obj.optString("hapticBehavior", "Medium"),
                    floatingOpacity = obj.optDouble("floatingOpacity", 0.85).toFloat(),
                    linkedGoalId = if (obj.has("linkedGoalId") && !obj.isNull("linkedGoalId")) obj.getString("linkedGoalId") else null
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

fun serializeCustomScenesJson(scenes: List<TimeScene>): String {
    val array = JSONArray()
    for (scene in scenes) {
        val obj = JSONObject().apply {
            put("id", scene.id)
            put("name", scene.name)
            put("mode", scene.mode)
            put("presetDurationMs", scene.presetDurationMs)
            put("soundBehavior", scene.soundBehavior)
            put("hapticBehavior", scene.hapticBehavior)
            put("floatingOpacity", scene.floatingOpacity)
            if (scene.linkedGoalId != null) put("linkedGoalId", scene.linkedGoalId)
        }
        array.put(obj)
    }
    return array.toString()
}

@Composable
fun ScenesScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
    onSelectScene: (TimeScene) -> Unit
) {
    val scope = rememberCoroutineScope()
    val activeSceneId by settingsRepository.activeSceneId.collectAsState(initial = "")
    val rawCustomScenesJson by settingsRepository.customScenesJson.collectAsState(initial = "")
    val customScenes = remember(rawCustomScenesJson) { parseCustomScenesJson(rawCustomScenesJson) }

    val allScenes = remember(customScenes) { TimeScene.PREDEFINED_SCENES + customScenes }
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
                    text = "TIME SCENES",
                    style = TextStyle(
                        color = LuxuryColors.CreamyWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraLight,
                        letterSpacing = 3.sp
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "+ CUSTOM",
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

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(allScenes, key = { it.id }) { scene ->
                    val isActive = scene.id == activeSceneId
                    SceneCard(
                        scene = scene,
                        isActive = isActive,
                        onSelect = {
                            scope.launch { settingsRepository.setActiveSceneId(scene.id) }
                            onSelectScene(scene)
                            onBack()
                        }
                    )
                }
            }
        }

        if (showCreateDialog) {
            CreateCustomSceneDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { newScene ->
                    val updated = customScenes + newScene
                    scope.launch {
                        settingsRepository.setCustomScenesJson(serializeCustomScenesJson(updated))
                        settingsRepository.setActiveSceneId(newScene.id)
                    }
                    onSelectScene(newScene)
                    showCreateDialog = false
                    onBack()
                }
            )
        }
    }
}

@Composable
fun SceneCard(
    scene: TimeScene,
    isActive: Boolean,
    onSelect: () -> Unit
) {
    val durationMins = scene.presetDurationMs / 60000

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) LuxuryColors.AccentGold.copy(alpha = 0.15f) else Color(0xFF111111)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isActive) LuxuryColors.AccentGold else Color.White.copy(alpha = 0.08f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = scene.name.uppercase(),
                    style = TextStyle(
                        color = LuxuryColors.CreamyWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )
                if (isActive) {
                    Text(
                        text = "ACTIVE",
                        style = TextStyle(
                            color = LuxuryColors.AccentGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            Column {
                Text(
                    text = "${scene.mode.uppercase()} • ${durationMins} MINS",
                    style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 10.sp, fontWeight = FontWeight.Light)
                )
                Text(
                    text = "Haptic: ${scene.hapticBehavior}",
                    style = TextStyle(color = LuxuryColors.WarmGray.copy(alpha = 0.7f), fontSize = 9.sp)
                )
            }
        }
    }
}

@Composable
fun CreateCustomSceneDialog(
    onDismiss: () -> Unit,
    onCreate: (TimeScene) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("countdown") }
    var durationMinsText by remember { mutableStateOf("30") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "CREATE CUSTOM SCENE",
                    style = TextStyle(
                        color = LuxuryColors.AccentGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Scene Name (e.g. Deep Work)", color = LuxuryColors.WarmGray, fontSize = 10.sp) },
                    textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = durationMinsText,
                    onValueChange = { durationMinsText = it.filter { c -> c.isDigit() } },
                    label = { Text("Duration (Minutes)", color = LuxuryColors.WarmGray, fontSize = 10.sp) },
                    textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "MODE", style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 10.sp, letterSpacing = 1.sp))
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("stopwatch", "countdown", "interval").forEach { mode ->
                        val isSel = selectedMode == mode
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) LuxuryColors.AccentGold.copy(alpha = 0.15f) else Color(0xFF141414)
                            ),
                            border = BorderStroke(1.dp, if (isSel) LuxuryColors.AccentGold else Color.White.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedMode = mode }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.uppercase(),
                                    style = TextStyle(
                                        color = if (isSel) LuxuryColors.CreamyWhite else LuxuryColors.WarmGray,
                                        fontSize = 9.sp,
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
                            val mins = durationMinsText.toLongOrNull() ?: 30L
                            val scene = TimeScene(
                                id = UUID.randomUUID().toString(),
                                name = name.ifBlank { "CUSTOM SCENE" },
                                mode = selectedMode,
                                presetDurationMs = mins * 60000L
                            )
                            onCreate(scene)
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
