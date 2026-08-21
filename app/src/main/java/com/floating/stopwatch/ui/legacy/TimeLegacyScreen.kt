package com.floating.stopwatch.ui.legacy

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.floating.stopwatch.ui.components.TimeDisplay
import com.floating.stopwatch.ui.theme.LuxuryColors
import kotlinx.coroutines.delay
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

    androidx.activity.compose.BackHandler(enabled = true) {
        when (currentSubScreen) {
            is LegacySubScreen.ListDashboard -> onBack()
            is LegacySubScreen.CreateEdit -> currentSubScreen = LegacySubScreen.ListDashboard
            is LegacySubScreen.DetailView -> currentSubScreen = LegacySubScreen.ListDashboard
            is LegacySubScreen.Plan -> currentSubScreen = LegacySubScreen.DetailView((currentSubScreen as LegacySubScreen.Plan).legacy)
            is LegacySubScreen.Journey -> currentSubScreen = LegacySubScreen.DetailView((currentSubScreen as LegacySubScreen.Journey).legacy)
            is LegacySubScreen.Moments -> currentSubScreen = LegacySubScreen.DetailView((currentSubScreen as LegacySubScreen.Moments).legacy)
            is LegacySubScreen.Insights -> currentSubScreen = LegacySubScreen.DetailView((currentSubScreen as LegacySubScreen.Insights).legacy)
            is LegacySubScreen.Finale -> currentSubScreen = LegacySubScreen.DetailView((currentSubScreen as LegacySubScreen.Finale).legacy)
        }
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
            val activeLegacy = legacies.find { it.id == screen.legacy.id } ?: screen.legacy
            LegacyDetailView(
                legacy = activeLegacy,
                settingsRepository = settingsRepository,
                onReload = { loadLegacies() },
                onBack = { currentSubScreen = LegacySubScreen.ListDashboard },
                onEdit = { currentSubScreen = LegacySubScreen.CreateEdit(activeLegacy) },
                onOpenPlan = { currentSubScreen = LegacySubScreen.Plan(activeLegacy) },
                onOpenJourney = { currentSubScreen = LegacySubScreen.Journey(activeLegacy) },
                onOpenMoments = { currentSubScreen = LegacySubScreen.Moments(activeLegacy) },
                onOpenInsights = { currentSubScreen = LegacySubScreen.Insights(activeLegacy) },
                onOpenFinale = { currentSubScreen = LegacySubScreen.Finale(activeLegacy) }
            )
        }
        is LegacySubScreen.Plan -> {
            val activeLegacy = legacies.find { it.id == screen.legacy.id } ?: screen.legacy
            LegacyPlanScreen(legacy = activeLegacy, onBack = { currentSubScreen = LegacySubScreen.DetailView(activeLegacy) })
        }
        is LegacySubScreen.Journey -> {
            val activeLegacy = legacies.find { it.id == screen.legacy.id } ?: screen.legacy
            LegacyJourneyScreen(legacy = activeLegacy, onBack = { currentSubScreen = LegacySubScreen.DetailView(activeLegacy) })
        }
        is LegacySubScreen.Moments -> {
            val activeLegacy = legacies.find { it.id == screen.legacy.id } ?: screen.legacy
            LegacyMomentsScreen(
                legacy = activeLegacy,
                onBack = { currentSubScreen = LegacySubScreen.DetailView(activeLegacy) },
                onAddJournalEntry = { text ->
                    val newEntry = LegacyJournalEntry(id = UUID.randomUUID().toString(), legacyId = activeLegacy.id, text = text)
                    val updated = activeLegacy.copy(journalEntries = activeLegacy.journalEntries + newEntry)
                    val newList = legacies.map { if (it.id == updated.id) updated else it }
                    saveLegacies(newList)
                    currentSubScreen = LegacySubScreen.Moments(updated)
                }
            )
        }
        is LegacySubScreen.Insights -> {
            val activeLegacy = legacies.find { it.id == screen.legacy.id } ?: screen.legacy
            LegacyInsightsScreen(legacy = activeLegacy, onBack = { currentSubScreen = LegacySubScreen.DetailView(activeLegacy) })
        }
        is LegacySubScreen.Finale -> {
            val activeLegacy = legacies.find { it.id == screen.legacy.id } ?: screen.legacy
            LegacyFinaleScreen(legacy = activeLegacy, onBack = { currentSubScreen = LegacySubScreen.DetailView(activeLegacy) })
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
    settingsRepository: SettingsRepository,
    onReload: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenJourney: () -> Unit,
    onOpenMoments: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenFinale: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val progress = LegacyProgressCalculator.calculateProgress(legacy)
    val recovery = LegacyProgressCalculator.calculateRecovery(legacy)

    // Dedicated Legacy Session Timer State (Monotonic timing)
    var isTimerRunning by remember { mutableStateOf(false) }
    var sessionElapsedMs by remember { mutableLongStateOf(0L) }
    var startTimeMs by remember { mutableLongStateOf(0L) }
    var accumulatedTimeMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            startTimeMs = SystemClock.elapsedRealtime()
            while (isTimerRunning) {
                sessionElapsedMs = accumulatedTimeMs + (SystemClock.elapsedRealtime() - startTimeMs)
                delay(50L)
            }
        }
    }

    // Modal dialog states
    var showManualModal by remember { mutableStateOf(false) }
    var showPostponeModal by remember { mutableStateOf(false) }

    if (showManualModal || showPostponeModal) {
        androidx.activity.compose.BackHandler(enabled = true) {
            showManualModal = false
            showPostponeModal = false
        }
    }

    var customManualHoursText by remember { mutableStateOf("") }
    var customManualMinsText by remember { mutableStateOf("") }
    var manualNote by remember { mutableStateOf("") }

    var postponeDaysText by remember { mutableStateOf("7") }
    var postponeReason by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryColors.WarmBlack)
            .padding(20.dp)
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = legacy.description,
                    style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 11.sp, fontWeight = FontWeight.Light)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Navigation bar for Legacy Sub-views
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("PLAN", color = LuxuryColors.AccentGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onOpenPlan() }.padding(4.dp))
                Text("•", color = LuxuryColors.WarmGray, fontSize = 10.sp)
                Text("JOURNEY", color = LuxuryColors.AccentGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onOpenJourney() }.padding(4.dp))
                Text("•", color = LuxuryColors.WarmGray, fontSize = 10.sp)
                Text("MOMENTS", color = LuxuryColors.AccentGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onOpenMoments() }.padding(4.dp))
                Text("•", color = LuxuryColors.WarmGray, fontSize = 10.sp)
                Text("INSIGHTS", color = LuxuryColors.AccentGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onOpenInsights() }.padding(4.dp))
                Text("•", color = LuxuryColors.WarmGray, fontSize = 10.sp)
                Text("FINALE", color = LuxuryColors.AccentGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onOpenFinale() }.padding(4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dedicated Legacy Session Timer Display
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, LuxuryColors.AccentGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LEGACY TIMER SESSION",
                        style = TextStyle(color = LuxuryColors.AccentGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TimeDisplay(
                        elapsedTimeMs = sessionElapsedMs,
                        showCentiseconds = true,
                        baseStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 42.sp),
                        scaleFactor = 1.0f,
                        accentColor = LuxuryColors.AccentGold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Start / Pause button
                        Button(
                            onClick = {
                                if (isTimerRunning) {
                                    isTimerRunning = false
                                    accumulatedTimeMs = sessionElapsedMs
                                } else {
                                    isTimerRunning = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTimerRunning) Color(0xFF9E2A2B) else LuxuryColors.AccentGold
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = if (isTimerRunning) "PAUSE" else "START LEGACY",
                                color = LuxuryColors.WarmBlack,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Stop & Record button
                        if (sessionElapsedMs > 0L) {
                            Button(
                                onClick = {
                                    isTimerRunning = false
                                    val finalMs = sessionElapsedMs
                                    sessionElapsedMs = 0L
                                    accumulatedTimeMs = 0L
                                    scope.launch {
                                        settingsRepository.recordLegacyTimerSession(legacy.id, finalMs)
                                        onReload()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, LuxuryColors.WarmGray)
                            ) {
                                Text("STOP & LOG", color = LuxuryColors.CreamyWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Row: Add Manual Time & Postpone Legacy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showManualModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+ ADD MANUAL TIME", color = LuxuryColors.AccentGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showPostponeModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+ POSTPONE LEGACY", color = LuxuryColors.CreamyWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Overview Dashboard Stats
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("DAYS REMAINING", color = LuxuryColors.WarmGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("${progress.remainingDays} / ${progress.totalDays} DAYS", color = LuxuryColors.CreamyWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TOTAL TARGET", color = LuxuryColors.WarmGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("${progress.plannedTimeMillis / 3600000L} HOURS", color = LuxuryColors.CreamyWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TIMER SESSIONS", color = LuxuryColors.WarmGray, fontSize = 10.sp)
                                Text("${progress.timerSessionsTimeMillis / 3600000L}h ${(progress.timerSessionsTimeMillis % 3600000L) / 60000L}m", color = LuxuryColors.AccentGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("MANUAL TIME", color = LuxuryColors.WarmGray, fontSize = 10.sp)
                                Text("${progress.manualEntriesTimeMillis / 3600000L}h ${(progress.manualEntriesTimeMillis % 3600000L) / 60000L}m", color = LuxuryColors.CreamyWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("RECALCULATED DAILY TARGET", color = LuxuryColors.WarmGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                val dailyTargetHours = progress.requiredDailyTargetMillis.toFloat() / 3600000f
                                Text(String.format("%.1fh / day", dailyTargetHours), color = LuxuryColors.AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (recovery.hoursBehind > 0f) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("RECOVERY STATUS", color = Color(0xFFC94A4A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(recovery.suggestion, color = LuxuryColors.CreamyWhite, fontSize = 11.sp, fontWeight = FontWeight.Light)
                            }
                        }
                    }
                }
            }
        }
    }

    // Manual Time Modal
    if (showManualModal) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showManualModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("+ ADD MANUAL TIME", style = TextStyle(color = LuxuryColors.AccentGold, fontSize = 14.sp, fontWeight = FontWeight.Bold))

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    settingsRepository.addLegacyManualTime(legacy.id, 1800000L, "Quick +30m")
                                    onReload()
                                    showManualModal = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+30 MINS", color = LuxuryColors.AccentGold, fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    settingsRepository.addLegacyManualTime(legacy.id, 3600000L, "Quick +1h")
                                    onReload()
                                    showManualModal = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+1 HOUR", color = LuxuryColors.AccentGold, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customManualHoursText,
                            onValueChange = { customManualHoursText = it },
                            label = { Text("Hours", color = LuxuryColors.WarmGray, fontSize = 10.sp) },
                            textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 12.sp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = customManualMinsText,
                            onValueChange = { customManualMinsText = it },
                            label = { Text("Minutes", color = LuxuryColors.WarmGray, fontSize = 10.sp) },
                            textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 12.sp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualNote,
                        onValueChange = { manualNote = it },
                        label = { Text("Optional Note / Activity", color = LuxuryColors.WarmGray, fontSize = 10.sp) },
                        textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("CANCEL", color = LuxuryColors.WarmGray, fontSize = 11.sp, modifier = Modifier.clickable { showManualModal = false }.padding(10.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val hrs = customManualHoursText.toLongOrNull() ?: 0L
                                val mins = customManualMinsText.toLongOrNull() ?: 0L
                                val totalMs = (hrs * 60 + mins) * 60000L
                                if (totalMs > 0L) {
                                    scope.launch {
                                        settingsRepository.addLegacyManualTime(legacy.id, totalMs, manualNote.ifBlank { "Manual Addition" })
                                        onReload()
                                        showManualModal = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryColors.AccentGold)
                        ) {
                            Text("LOG TIME", color = LuxuryColors.WarmBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Postpone Legacy Modal
    if (showPostponeModal) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showPostponeModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("POSTPONE LEGACY", style = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold))

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Add days to your legacy timeline. Existing progress and session logs will be fully preserved.", style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 11.sp))

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = postponeDaysText,
                        onValueChange = { postponeDaysText = it },
                        label = { Text("Days to Add", color = LuxuryColors.WarmGray, fontSize = 10.sp) },
                        textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = postponeReason,
                        onValueChange = { postponeReason = it },
                        label = { Text("Reason for Postponing", color = LuxuryColors.WarmGray, fontSize = 10.sp) },
                        textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("CANCEL", color = LuxuryColors.WarmGray, fontSize = 11.sp, modifier = Modifier.clickable { showPostponeModal = false }.padding(10.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val addedDays = postponeDaysText.toIntOrNull() ?: 0
                                if (addedDays > 0) {
                                    scope.launch {
                                        settingsRepository.postponeLegacy(legacy.id, addedDays, postponeReason.ifBlank { "Postponed timeline" })
                                        onReload()
                                        showPostponeModal = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryColors.AccentGold)
                        ) {
                            Text("CONFIRM POSTPONE", color = LuxuryColors.WarmBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
