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
import com.floating.stopwatch.domain.LegacyJournalEntry
import com.floating.stopwatch.domain.LegacyMoment
import com.floating.stopwatch.domain.TimeLegacy
import com.floating.stopwatch.ui.theme.LuxuryColors

@Composable
fun LegacyMomentsScreen(
    legacy: TimeLegacy,
    onBack: () -> Unit,
    onAddJournalEntry: (String) -> Unit = {}
) {
    var newJournalText by remember { mutableStateOf("") }

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
                    text = "MOMENTS & JOURNAL",
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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = newJournalText,
                onValueChange = { newJournalText = it },
                label = { Text("Log Journal Note", color = LuxuryColors.WarmGray, fontSize = 11.sp) },
                textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 12.sp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = {
                        if (newJournalText.isNotBlank()) {
                            onAddJournalEntry(newJournalText)
                            newJournalText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuxuryColors.AccentGold)
                ) {
                    Text("ADD NOTE", color = LuxuryColors.WarmBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(legacy.journalEntries.reversed()) { entry ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = entry.text,
                                style = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 12.sp, fontWeight = FontWeight.Light)
                            )
                        }
                    }
                }
            }
        }
    }
}
