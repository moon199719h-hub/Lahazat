package com.floating.stopwatch.ui.legacy

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
import com.floating.stopwatch.domain.LegacyJournalEntry
import com.floating.stopwatch.domain.LegacyMilestoneDetector
import com.floating.stopwatch.domain.LegacyProgressCalculator
import com.floating.stopwatch.domain.LegacyProgressStatus
import com.floating.stopwatch.domain.TimeLegacy
import com.floating.stopwatch.ui.theme.LuxuryColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

sealed class LegacySubScreen {
    object ListDashboard : LegacySubScreen()
    data class CreateEdit(val legacy: TimeLegacy? = null) : LegacySubScreen()
    data class DetailView(val legacy: TimeLegacy) : LegacySubScreen()
    data class Plan(val legacy: TimeLegacy) : LegacySubScreen()
    data class Journey(val legacy: TimeLegacy) : LegacySubScreen()
    data class Moments(val legacy: TimeLegacy) : LegacySubScreen()
    data class Insights(val legacy: TimeLegacy) : LegacySubScreen()
    data class Finale(val legacy: TimeLegacy) : LegacySubScreen()
}

@Composable
fun TimeLegacyScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentSubScreen by remember { mutableStateOf<LegacySubScreen>(LegacySubScreen.ListDashboard) }
    var legacies by remember { mutableStateOf<List<TimeLegacy>>(emptyList()) }

    fun loadLegacies() {
        scope.launch {
            val json = settingsRepository.timeLegaciesJson.first()
            legacies = TimeLegacy.parseListJson(json)
        }
    }

    fun saveLegacies(updatedList: List<TimeLegacy>) {
        legacies = updatedList
        scope.launch {
            val json = TimeLegacy.serializeListJson(updatedList)
            settingsRepository.saveTimeLegaciesJson(json)
        }
    }

    LaunchedEffect(Unit) {
        loadLegacies()
    }

    when (val screen = currentSubScreen) {
        is LegacySubScreen.ListDashboard -> {
            TimeLegacyDashboardView(
                legacies = legacies,
                onBack = onBack,
                onCreateNewLegacy = { currentSubScreen = LegacySubScreen.CreateEdit(null) },
                onSelectLegacy = { currentSubScreen = LegacySubScreen.DetailView(it) }
            )
        }
        is LegacySubScreen.CreateEdit -> {
            LegacyCreateScreen(
                initialLegacy = screen.legacy,
                onBack = { currentSubScreen = LegacySubScreen.ListDashboard },
                onSaveLegacy = { newOrUpdated ->
                    val newList = if (screen.legacy != null) {
                        legacies.map { if (it.id == newOrUpdated.id) newOrUpdated else it }
                    } else {
                        val milestones = LegacyMilestoneDetector.detectNewMilestones(newOrUpdated)
                        val withMilestones = newOrUpdated.copy(moments = newOrUpdated.moments + milestones)
                        legacies + withMilestones
                    }
                    saveLegacies(newList)
                    currentSubScreen = LegacySubScreen.ListDashboard
                },
                onDeleteLegacy = { idToDelete ->
                    val newList = legacies.filterNot { it.id == idToDelete }
                    saveLegacies(newList)
                    currentSubScreen = LegacySubScreen.ListDashboard
                }
            )
        }
        is LegacySubScreen.DetailView -> {
            LegacyDetailView(
                legacy = screen.legacy,
                onBack = { currentSubScreen = LegacySubScreen.ListDashboard },
                onEdit = { currentSubScreen = LegacySubScreen.CreateEdit(screen.legacy) },
                onOpenPlan = { currentSubScreen = LegacySubScreen.Plan(screen.legacy) },
                onOpenJourney = { currentSubScreen = LegacySubScreen.Journey(screen.legacy) },
                onOpenMoments = { currentSubScreen = LegacySubScreen.Moments(screen.legacy) },
                onOpenInsights = { currentSubScreen = LegacySubScreen.Insights(screen.legacy) },
                onOpenFinale = { currentSubScreen = LegacySubScreen.Finale(screen.legacy) }
            )
        }
        is LegacySubScreen.Plan -> {
            LegacyPlanScreen(legacy = screen.legacy, onBack = { currentSubScreen = LegacySubScreen.DetailView(screen.legacy) })
        }
        is LegacySubScreen.Journey -> {
            LegacyJourneyScreen(legacy = screen.legacy, onBack = { currentSubScreen = LegacySubScreen.DetailView(screen.legacy) })
        }
        is LegacySubScreen.Moments -> {
            LegacyMomentsScreen(
                legacy = screen.legacy,
                onBack = { currentSubScreen = LegacySubScreen.DetailView(screen.legacy) },
                onAddJournalEntry = { text ->
                    val newEntry = LegacyJournalEntry(id = UUID.randomUUID().toString(), legacyId = screen.legacy.id, text = text)
                    val updated = screen.legacy.copy(journalEntries = screen.legacy.journalEntries + newEntry)
                    val newList = legacies.map { if (it.id == updated.id) updated else it }
                    saveLegacies(newList)
                    currentSubScreen = LegacySubScreen.Moments(updated)
                }
            )
        }
        is LegacySubScreen.Insights -> {
            LegacyInsightsScreen(legacy = screen.legacy, onBack = { currentSubScreen = LegacySubScreen.DetailView(screen.legacy) })
        }
        is LegacySubScreen.Finale -> {
            LegacyFinaleScreen(legacy = screen.legacy, onBack = { currentSubScreen = LegacySubScreen.DetailView(screen.legacy) })
        }
    }
}

@Composable
fun TimeLegacyDashboardView(
    legacies: List<TimeLegacy>,
    onBack: () -> Unit,
    onCreateNewLegacy: () -> Unit,
    onSelectLegacy: (TimeLegacy) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryColors.WarmBlack)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TIME LEGACY",
                    style = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = 4.sp)
                )

                Text(
                    text = "[CLOSE]",
                    style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 11.sp, letterSpacing = 2.sp),
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE LEGACIES (${legacies.size})",
                    style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                )

                Text(
                    text = "+ CREATE LEGACY",
                    style = TextStyle(color = LuxuryColors.AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
                    modifier = Modifier
                        .clickable { onCreateNewLegacy() }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (legacies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO LEGACIES CREATED YET\nTAP + CREATE LEGACY TO BEGIN YOUR JOURNEY",
                        style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 12.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(legacies) { legacy ->
                        LegacyCardItem(
                            legacy = legacy,
                            onClick = { onSelectLegacy(legacy) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegacyCardItem(
    legacy: TimeLegacy,
    onClick: () -> Unit
) {
    val progress = LegacyProgressCalculator.calculateProgress(legacy)
    val recovery = LegacyProgressCalculator.calculateRecovery(legacy)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = legacy.title.uppercase(),
                    style = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                )

                val statusColor = when (progress.status) {
                    LegacyProgressStatus.COMPLETED -> Color(0xFF4AC98F)
                    LegacyProgressStatus.ON_TRACK -> LuxuryColors.AccentGold
                    LegacyProgressStatus.AT_RISK, LegacyProgressStatus.OVERDUE -> Color(0xFFC94A4A)
                    else -> LuxuryColors.WarmGray
                }

                Text(
                    text = progress.status.name,
                    style = TextStyle(color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                )
            }

            if (legacy.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = legacy.description,
                    style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 11.sp, fontWeight = FontWeight.Light)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val targetHours = legacy.targetDurationMillis / 3600000L
            val actualHours = progress.actualTimeMillis / 3600000L

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PROGRESS: ${actualHours}h / ${targetHours}h",
                    style = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                )

                Text(
                    text = "${(progress.progressFraction * 100).toInt()}%",
                    style = TextStyle(color = LuxuryColors.AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = LuxuryColors.AccentGold,
                trackColor = Color(0xFF2C2C2E)
            )

            if (recovery.hoursBehind > 0f) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "RECOVERY: ${recovery.suggestion}",
                    style = TextStyle(color = Color(0xFFC94A4A), fontSize = 10.sp, fontWeight = FontWeight.Light)
                )
            }
        }
    }
}

@Composable
fun LegacyDetailView(
    legacy: TimeLegacy,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenJourney: () -> Unit,
    onOpenMoments: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenFinale: () -> Unit
) {
    val progress = LegacyProgressCalculator.calculateProgress(legacy)
    val recovery = LegacyProgressCalculator.calculateRecovery(legacy)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryColors.WarmBlack)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = legacy.title.uppercase(),
                    style = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                )

                Row {
                    Text(
                        text = "[EDIT]",
                        style = TextStyle(color = LuxuryColors.AccentGold, fontSize = 11.sp, letterSpacing = 1.sp),
                        modifier = Modifier.clickable { onEdit() }.padding(6.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "[BACK]",
                        style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 11.sp, letterSpacing = 1.sp),
                        modifier = Modifier.clickable { onBack() }.padding(6.dp)
                    )
                }
            }

            if (legacy.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = legacy.description,
                    style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 12.sp, fontWeight = FontWeight.Light)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation Links for Sub-views
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("PLAN", color = LuxuryColors.AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onOpenPlan() }.padding(4.dp))
                Text("•", color = LuxuryColors.WarmGray, fontSize = 11.sp)
                Text("JOURNEY", color = LuxuryColors.AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onOpenJourney() }.padding(4.dp))
                Text("•", color = LuxuryColors.WarmGray, fontSize = 11.sp)
                Text("MOMENTS", color = LuxuryColors.AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onOpenMoments() }.padding(4.dp))
                Text("•", color = LuxuryColors.WarmGray, fontSize = 11.sp)
                Text("INSIGHTS", color = LuxuryColors.AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onOpenInsights() }.padding(4.dp))
                Text("•", color = LuxuryColors.WarmGray, fontSize = 11.sp)
                Text("FINALE", color = LuxuryColors.AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onOpenFinale() }.padding(4.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "PROGRESS STATUS", color = LuxuryColors.WarmGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text(text = progress.status.name, color = LuxuryColors.AccentGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(12.dp))

                    val targetH = legacy.targetDurationMillis / 3600000L
                    val actualH = progress.actualTimeMillis / 3600000L
                    Text(text = "LOGGED TIME: ${actualH}h / ${targetH}h", color = LuxuryColors.CreamyWhite, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress.progressFraction },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = LuxuryColors.AccentGold,
                        trackColor = Color(0xFF2C2C2E)
                    )

                    if (recovery.hoursBehind > 0f) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "RECOVERY MODE", color = Color(0xFFC94A4A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = recovery.suggestion, color = LuxuryColors.CreamyWhite, fontSize = 11.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
        }
    }
}
