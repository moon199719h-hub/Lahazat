package com.floating.stopwatch.ui.legacy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.floating.stopwatch.domain.LegacyGoal
import com.floating.stopwatch.domain.TimeLegacy
import com.floating.stopwatch.ui.theme.LuxuryColors
import java.util.UUID

@Composable
fun LegacyCreateScreen(
    initialLegacy: TimeLegacy? = null,
    onBack: () -> Unit,
    onSaveLegacy: (TimeLegacy) -> Unit,
    onDeleteLegacy: (String) -> Unit = {}
) {
    var title by remember { mutableStateOf(initialLegacy?.title ?: "") }
    var description by remember { mutableStateOf(initialLegacy?.description ?: "") }
    var daysText by remember { mutableStateOf((initialLegacy?.daysCount ?: 30).toString()) }
    var dailyHoursText by remember { mutableStateOf((initialLegacy?.dailyTargetMinutes?.div(60) ?: 2).toString()) }
    var dailyMinsText by remember { mutableStateOf((initialLegacy?.dailyTargetMinutes?.rem(60) ?: 30).toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val days = daysText.toIntOrNull() ?: 0
    val dHours = dailyHoursText.toIntOrNull() ?: 0
    val dMins = dailyMinsText.toIntOrNull() ?: 0

    val dailyTargetMinutes = (dHours * 60 + dMins).coerceAtLeast(0)
    val totalMinutes = days * dailyTargetMinutes
    val totalHoursCalculated = totalMinutes / 60
    val remainingMinsCalculated = totalMinutes % 60
    val calculatedTargetMs = totalMinutes * 60000L

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
                    text = if (initialLegacy == null) "CREATE LEGACY" else "EDIT LEGACY",
                    style = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = 4.sp)
                )

                Text(
                    text = "[CANCEL]",
                    style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 11.sp, letterSpacing = 2.sp),
                    modifier = Modifier.clickable { onBack() }.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = TextStyle(color = Color(0xFFC94A4A), fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it; errorMessage = null },
                label = { Text("Legacy Name / Title *", color = LuxuryColors.WarmGray, fontSize = 11.sp) },
                textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Intention", color = LuxuryColors.WarmGray, fontSize = 11.sp) },
                textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it; errorMessage = null },
                    label = { Text("Days Count *", color = LuxuryColors.WarmGray, fontSize = 10.sp) },
                    textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 12.sp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = dailyHoursText,
                    onValueChange = { dailyHoursText = it; errorMessage = null },
                    label = { Text("Daily Hours *", color = LuxuryColors.WarmGray, fontSize = 10.sp) },
                    textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 12.sp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = dailyMinsText,
                    onValueChange = { dailyMinsText = it; errorMessage = null },
                    label = { Text("Daily Mins *", color = LuxuryColors.WarmGray, fontSize = 10.sp) },
                    textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 12.sp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calculated Target Preview Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LuxuryColors.AccentGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "CALCULATED TARGET TIME",
                        style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$days days × ${dHours}h ${dMins}m/day = ${totalHoursCalculated}h ${remainingMinsCalculated}m total",
                        style = TextStyle(color = LuxuryColors.AccentGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (initialLegacy != null) {
                    Text(
                        text = "DELETE LEGACY",
                        color = Color(0xFFC94A4A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onDeleteLegacy(initialLegacy.id) }
                            .padding(8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = {
                        if (title.isBlank()) {
                            errorMessage = "Title is required and cannot be empty."
                        } else if (days <= 0) {
                            errorMessage = "Number of days must be at least 1."
                        } else if (dailyTargetMinutes <= 0) {
                            errorMessage = "Daily target must be greater than 0."
                        } else {
                            val defaultGoal = LegacyGoal(
                                id = UUID.randomUUID().toString(),
                                title = "Main Target",
                                targetDurationMillis = calculatedTargetMs,
                                actualDurationMillis = initialLegacy?.goals?.firstOrNull()?.actualDurationMillis ?: 0L
                            )
                            val startMs = initialLegacy?.startAt ?: System.currentTimeMillis()
                            val endMs = startMs + 86400000L * days
                            val legacyToSave = TimeLegacy(
                                id = initialLegacy?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(),
                                description = description.trim(),
                                daysCount = days,
                                dailyTargetMinutes = dailyTargetMinutes,
                                startAt = startMs,
                                endAt = endMs,
                                targetDurationMillis = calculatedTargetMs,
                                goals = listOf(defaultGoal),
                                moments = initialLegacy?.moments ?: emptyList(),
                                journalEntries = initialLegacy?.journalEntries ?: emptyList(),
                                manualEntries = initialLegacy?.manualEntries ?: emptyList(),
                                postponements = initialLegacy?.postponements ?: emptyList(),
                                createdAt = initialLegacy?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            onSaveLegacy(legacyToSave)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuxuryColors.AccentGold),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("SAVE LEGACY", color = LuxuryColors.WarmBlack, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
