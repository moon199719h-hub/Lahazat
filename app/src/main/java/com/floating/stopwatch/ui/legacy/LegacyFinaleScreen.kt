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
import com.floating.stopwatch.domain.LegacyProgressCalculator
import com.floating.stopwatch.domain.TimeLegacy
import com.floating.stopwatch.ui.theme.LuxuryColors

@Composable
fun LegacyFinaleScreen(
    legacy: TimeLegacy,
    onBack: () -> Unit
) {
    val progress = LegacyProgressCalculator.calculateProgress(legacy)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryColors.WarmBlack)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LEGACY FINALE",
                    style = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = 4.sp)
                )

                Text(
                    text = "[CLOSE]",
                    style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 11.sp, letterSpacing = 2.sp),
                    modifier = Modifier.clickable { onBack() }.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Pure Static Legacy Portrait Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, LuxuryColors.AccentGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LEGACY PORTRAIT",
                        style = TextStyle(color = LuxuryColors.AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = legacy.title.uppercase(),
                        style = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "TOTAL LOGGED: ${progress.actualTimeMillis / 3600000L} HOURS",
                        style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 12.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "COMPLETION: ${(progress.progressFraction * 100).toInt()}%",
                        style = TextStyle(color = LuxuryColors.AccentGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
