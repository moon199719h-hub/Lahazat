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
import com.floating.stopwatch.domain.TimeLegacy
import com.floating.stopwatch.ui.theme.LuxuryColors

@Composable
fun LegacyPlanScreen(
    legacy: TimeLegacy,
    onBack: () -> Unit
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
                    text = "LEGACY PLAN",
                    style = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = 4.sp)
                )

                Text(
                    text = "[BACK]",
                    style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 11.sp, letterSpacing = 2.sp),
                    modifier = Modifier.clickable { onBack() }.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = legacy.title.uppercase(),
                style = TextStyle(color = LuxuryColors.AccentGold, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "PHASES & GOALS",
                style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (legacy.phases.isEmpty() && legacy.goals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO PHASES OR GOALS CONFIGURED FOR THIS LEGACY",
                        style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 12.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(legacy.goals) { goal ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = goal.title,
                                    style = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val targetH = goal.targetDurationMillis / 3600000L
                                val actualH = goal.actualDurationMillis / 3600000L
                                Text(
                                    text = "Target: ${targetH}h | Progress: ${actualH}h",
                                    style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
