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
    var targetHoursText by remember { mutableStateOf((initialLegacy?.targetDurationMillis?.div(3600000L) ?: 30L).toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

            Spacer(modifier = Modifier.height(20.dp))

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
                label = { Text("Legacy Title *", color = LuxuryColors.WarmGray, fontSize = 11.sp) },
                textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Intention", color = LuxuryColors.WarmGray, fontSize = 11.sp) },
                textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = targetHoursText,
                onValueChange = { targetHoursText = it; errorMessage = null },
                label = { Text("Target Hours *", color = LuxuryColors.WarmGray, fontSize = 11.sp) },
                textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PRESET DURATIONS",
                style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10L, 30L, 60L, 100L).forEach { hours ->
                    Button(
                        onClick = { targetHoursText = hours.toString(); errorMessage = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF2C2C2E))
                    ) {
                        Text("${hours}h", color = LuxuryColors.AccentGold, fontSize = 11.sp)
                    }
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
                        val hours = targetHoursText.toLongOrNull()
                        if (title.isBlank()) {
                            errorMessage = "Title is required and cannot be empty."
                        } else if (hours == null || hours <= 0L) {
                            errorMessage = "Target duration must be a positive number of hours."
                        } else {
                            val targetMs = hours * 3600000L
                            val defaultGoal = LegacyGoal(
                                id = UUID.randomUUID().toString(),
                                title = "Main Target",
                                targetDurationMillis = targetMs,
                                actualDurationMillis = initialLegacy?.goals?.firstOrNull()?.actualDurationMillis ?: 0L
                            )
                            val legacyToSave = TimeLegacy(
                                id = initialLegacy?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(),
                                description = description.trim(),
                                targetDurationMillis = targetMs,
                                goals = listOf(defaultGoal),
                                moments = initialLegacy?.moments ?: emptyList(),
                                journalEntries = initialLegacy?.journalEntries ?: emptyList(),
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
