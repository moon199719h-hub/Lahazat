package com.floating.stopwatch.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.floating.stopwatch.domain.calculateGamificationStats
import com.floating.stopwatch.ui.theme.LuxuryColors

@Composable
fun GamificationScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit
) {
    val rawMemoriesJson by settingsRepository.timeMemoriesJson.collectAsState(initial = "")
    val memories = remember(rawMemoriesJson) { parseTimeMemoriesJson(rawMemoriesJson) }

    val rawGoalsJson by settingsRepository.goalsJson.collectAsState(initial = "")
    val goals = remember(rawGoalsJson) { parseGoalsJson(rawGoalsJson) }

    val stats = remember(memories, goals) { calculateGamificationStats(memories, goals) }

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
                    text = "PROGRESS & ACHIEVEMENTS",
                    style = TextStyle(
                        color = LuxuryColors.CreamyWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraLight,
                        letterSpacing = 2.5.sp
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Streak Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LuxuryColors.AccentGold.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "DAILY STREAK",
                                    style = TextStyle(
                                        color = LuxuryColors.WarmGray,
                                        fontSize = 10.sp,
                                        letterSpacing = 2.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${stats.currentStreakDays} DAYS",
                                    style = TextStyle(
                                        color = LuxuryColors.AccentGold,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "TOTAL FOCUSED",
                                    style = TextStyle(
                                        color = LuxuryColors.WarmGray,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format("%.1f HOURS", stats.totalFocusedHours),
                                    style = TextStyle(
                                        color = LuxuryColors.CreamyWhite,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }

                // Milestones Section
                item {
                    Text(
                        text = "TIME MILESTONES",
                        style = TextStyle(
                            color = LuxuryColors.WarmGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val allMilestones = listOf("First Hour", "5 Hours Focused", "10 Hours Master", "25 Hours Dedicated", "50 Hours Scholar", "100 Hours Legend")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allMilestones.forEach { milestone ->
                            val isUnlocked = stats.unlockedMilestones.contains(milestone)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUnlocked) Color(0xFF161512) else Color(0xFF0E0E0E)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isUnlocked) Color(0xFF4AC98F).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = milestone.uppercase(),
                                        style = TextStyle(
                                            color = if (isUnlocked) LuxuryColors.CreamyWhite else LuxuryColors.WarmGray.copy(alpha = 0.5f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    Text(
                                        text = if (isUnlocked) "UNLOCKED ✓" else "LOCKED",
                                        style = TextStyle(
                                            color = if (isUnlocked) Color(0xFF4AC98F) else LuxuryColors.WarmGray.copy(alpha = 0.4f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Achievements Section
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ACHIEVEMENTS",
                        style = TextStyle(
                            color = LuxuryColors.WarmGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val allAchievements = listOf("First Steps", "Consistent Timer", "Time Architect", "Goal Achiever", "7-Day Streak")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allAchievements.forEach { achievement ->
                            val isUnlocked = stats.unlockedAchievements.contains(achievement)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUnlocked) Color(0xFF161512) else Color(0xFF0E0E0E)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isUnlocked) LuxuryColors.AccentGold.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = achievement.uppercase(),
                                        style = TextStyle(
                                            color = if (isUnlocked) LuxuryColors.CreamyWhite else LuxuryColors.WarmGray.copy(alpha = 0.5f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    Text(
                                        text = if (isUnlocked) "EARNED ★" else "LOCKED",
                                        style = TextStyle(
                                            color = if (isUnlocked) LuxuryColors.AccentGold else LuxuryColors.WarmGray.copy(alpha = 0.4f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
