package com.floating.stopwatch.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.floating.stopwatch.ui.components.DragAdjustField
import com.floating.stopwatch.ui.theme.LuxuryColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var activeDialogCategory by remember { mutableStateOf<String?>(null) }
    var isClosingMain by remember { mutableStateOf(false) }
    var isClosingSub by remember { mutableStateOf(false) }

    val stylePreset by settingsRepository.stylePreset.collectAsState(initial = "Glass Premium")
    val colorPreset by settingsRepository.colorPreset.collectAsState(initial = "Gold")
    val customColorHex by settingsRepository.customColorHex.collectAsState(initial = "#C9A66B")
    val hapticIntensity by settingsRepository.hapticIntensity.collectAsState(initial = "Medium")
    val themeMode by settingsRepository.themeMode.collectAsState(initial = "Midnight")
    val mainDisplayScale by settingsRepository.mainDisplayScale.collectAsState(initial = 1.0f)

    val shapes = listOf("rounded", "capsule", "circle", "sharp", "glass")
    val themeModes = listOf("Midnight Dark", "Warm Paper Light", "Obsidian Dark", "Pure White Light")
    val presets = listOf("Glass Premium", "Obsidian", "Titanium", "Ultra Minimal")
    val intensities = listOf("Off", "Light", "Medium", "Strong")
    val colorPresets = listOf("Gold", "Galaxy Blue", "Titanium", "Emerald", "Sapphire", "Violet", "Rose", "Ice", "Amber", "Pure White", "Custom")

    val categories = listOf(
        "Appearance", "Stopwatch", "Countdown", "Counter", "Interval",
        "Sounds & Haptics", "Floating Widgets", "Advanced"
    )

    fun dismissSettings() {
        if (activeDialogCategory != null) {
            isClosingSub = true
        } else {
            isClosingMain = true
        }
    }

    fun dismissCategory() {
        isClosingSub = true
    }

    LaunchedEffect(isClosingMain) {
        if (isClosingMain) {
            delay(180)
            onBack()
        }
    }

    LaunchedEffect(isClosingSub) {
        if (isClosingSub) {
            delay(150)
            activeDialogCategory = null
            isClosingSub = false
        }
    }

    BackHandler(enabled = true) {
        dismissSettings()
    }

    val activeAccentColor = if (colorPreset == "Custom") {
        try { Color(android.graphics.Color.parseColor(customColorHex)) } catch (e: Exception) { LuxuryColors.AccentGold }
    } else {
        LuxuryColors.fromName(colorPreset)
    }

    // Main Floating Settings Window Overlay
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable { dismissSettings() }
    ) {
        val panelMaxHeight = (maxHeight - 48.dp).coerceAtMost(640.dp)

        // Category Detail Submenu Floating Window Overlay
        if (activeDialogCategory != null) {
            val category = activeDialogCategory!!
            AnimatedVisibility(
                visible = !isClosingSub,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                enter = slideInVertically(
                    animationSpec = tween(180),
                    initialOffsetY = { -it / 2 }
                ) + fadeIn(animationSpec = tween(150)),
                exit = slideOutVertically(
                    animationSpec = tween(150),
                    targetOffsetY = { -it / 2 }
                ) + fadeOut(animationSpec = tween(120))
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 680.dp)
                        .heightIn(max = panelMaxHeight)
                        .wrapContentHeight()
                        .statusBarsPadding()
                        .imePadding(),
                    shape = RoundedCornerShape(20.dp),
                    color = LuxuryColors.WarmBlack,
                    border = BorderStroke(1.dp, activeAccentColor.copy(alpha = 0.35f)),
                    tonalElevation = 0.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "‹ BACK",
                                    color = activeAccentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier
                                        .clickable { dismissCategory() }
                                        .padding(vertical = 6.dp, horizontal = 4.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = category.uppercase(),
                                    style = TextStyle(
                                        color = LuxuryColors.CreamyWhite,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 2.5.sp
                                    )
                                )
                            }
                            Text(
                                text = "✕",
                                color = LuxuryColors.WarmGray,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .clickable { dismissCategory() }
                                    .padding(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        when (category) {
                            "Appearance" -> {
                                Text(
                                    text = "ILLUMINATION MODE",
                                    color = LuxuryColors.WarmGray,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.8.sp
                                )
                                ResponsiveOptionGrid(
                                    options = themeModes,
                                    selectedOption = themeMode,
                                    accentColor = activeAccentColor,
                                    onOptionSelected = { mode -> scope.launch { settingsRepository.setThemeMode(mode) } }
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "STYLE PRESET",
                                    color = LuxuryColors.WarmGray,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.8.sp
                                )
                                ResponsiveOptionGrid(
                                    options = presets,
                                    selectedOption = stylePreset,
                                    accentColor = activeAccentColor,
                                    onOptionSelected = { preset -> scope.launch { settingsRepository.setStylePreset(preset) } }
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "COLOR ACCENT PRESET",
                                    color = LuxuryColors.WarmGray,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.8.sp
                                )
                                ResponsiveOptionGrid(
                                    options = colorPresets,
                                    selectedOption = colorPreset,
                                    accentColor = activeAccentColor,
                                    onOptionSelected = { color -> scope.launch { settingsRepository.setColorPreset(color) } }
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                DragAdjustField(
                                    label = "MAIN DISPLAY SIZE",
                                    value = mainDisplayScale,
                                    minValue = 0.7f,
                                    maxValue = 1.3f,
                                    pixelsPerUnit = 180f,
                                    accentColor = activeAccentColor,
                                    valueFormatter = { String.format("%.2fx", it) },
                                    onValueChange = { scope.launch { settingsRepository.setMainDisplayScale(it) } }
                                )
                            }
                            "Stopwatch" -> WidgetCategorySettings(
                                index = 0,
                                widgetTitle = "STOPWATCH",
                                settingsRepository = settingsRepository,
                                scope = scope
                            )
                            "Countdown" -> WidgetCategorySettings(
                                index = 1,
                                widgetTitle = "COUNTDOWN",
                                settingsRepository = settingsRepository,
                                scope = scope
                            )
                            "Counter" -> WidgetCategorySettings(
                                index = 2,
                                widgetTitle = "COUNTER",
                                settingsRepository = settingsRepository,
                                scope = scope
                            )
                            "Interval" -> {
                                val intervalName by settingsRepository.intervalName.collectAsState(initial = "HIT")
                                val workMs by settingsRepository.intervalWorkMs.collectAsState(initial = 40000L)
                                val restMs by settingsRepository.intervalRestMs.collectAsState(initial = 20000L)
                                val rounds by settingsRepository.intervalRounds.collectAsState(initial = 8)

                                var nameInput by remember(intervalName) { mutableStateOf(intervalName) }
                                var workSecs by remember(workMs) { mutableIntStateOf((workMs / 1000).toInt()) }
                                var restSecs by remember(restMs) { mutableIntStateOf((restMs / 1000).toInt()) }
                                var roundsVal by remember(rounds) { mutableIntStateOf(rounds) }

                                Text(
                                    text = "INTERVAL CONFIGURATION",
                                    color = LuxuryColors.WarmGray,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.8.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Interval Name", color = LuxuryColors.WarmGray, fontSize = 10.sp) },
                                    textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 12.sp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                DragAdjustField(
                                    label = "WORK DURATION",
                                    value = workSecs.toFloat(),
                                    minValue = 1f,
                                    maxValue = 18000f,
                                    pixelsPerUnit = 4f,
                                    accentColor = activeAccentColor,
                                    valueFormatter = { formatSettingsDuration(it.toInt()) },
                                    onValueChange = { workSecs = it.toInt().coerceIn(1, 18000) }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                DragAdjustField(
                                    label = "REST DURATION",
                                    value = restSecs.toFloat(),
                                    minValue = 1f,
                                    maxValue = 3600f,
                                    pixelsPerUnit = 4f,
                                    accentColor = activeAccentColor,
                                    valueFormatter = { formatSettingsDuration(it.toInt()) },
                                    onValueChange = { restSecs = it.toInt().coerceIn(1, 3600) }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ROUNDS: $roundsVal",
                                        color = LuxuryColors.CreamyWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row {
                                        Box(
                                            modifier = Modifier
                                                .clickable { if (roundsVal > 1) roundsVal -= 1 }
                                                .padding(8.dp)
                                        ) {
                                            Text("-", color = activeAccentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clickable { roundsVal += 1 }
                                                .padding(8.dp)
                                        ) {
                                            Text("+", color = activeAccentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        scope.launch {
                                            settingsRepository.setIntervalConfig(
                                                name = nameInput.ifBlank { "HIT" },
                                                workMs = workSecs * 1000L,
                                                restMs = restSecs * 1000L,
                                                rounds = roundsVal
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "SAVE CONFIGURATION",
                                        color = LuxuryColors.WarmBlack,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                WidgetCategorySettings(
                                    index = 3,
                                    widgetTitle = "INTERVAL",
                                    settingsRepository = settingsRepository,
                                    scope = scope
                                )
                            }
                            "Floating Widgets" -> {
                                val shapePreset by settingsRepository.shapePreset.collectAsState(initial = "rounded")
                                val floatingPadding by settingsRepository.floatingPadding.collectAsState(initial = 6.0f)
                                val floatingOpacity by settingsRepository.floatingOpacity.collectAsState(initial = 0.85f)

                                Text(
                                    text = "SHAPE PRESET",
                                    color = LuxuryColors.WarmGray,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.8.sp
                                )
                                ResponsiveOptionGrid(
                                    options = shapes,
                                    selectedOption = shapePreset,
                                    accentColor = activeAccentColor,
                                    onOptionSelected = { shape -> scope.launch { settingsRepository.setShapePreset(shape) } },
                                    displayName = { it.uppercase() }
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                DragAdjustField(
                                    label = "PADDING",
                                    value = floatingPadding,
                                    minValue = 0f,
                                    maxValue = 32f,
                                    pixelsPerUnit = 8f,
                                    accentColor = activeAccentColor,
                                    valueFormatter = { "${it.toInt()}dp" },
                                    onValueChange = { scope.launch { settingsRepository.setFloatingPadding(it) } }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                DragAdjustField(
                                    label = "OPACITY",
                                    value = floatingOpacity,
                                    minValue = 0f,
                                    maxValue = 1f,
                                    pixelsPerUnit = 180f,
                                    accentColor = activeAccentColor,
                                    valueFormatter = { "${(it * 100).toInt()}%" },
                                    onValueChange = { scope.launch { settingsRepository.setFloatingOpacity(it) } }
                                )
                            }
                            "Sounds & Haptics" -> {
                                Text(
                                    text = "HAPTIC FEEDBACK INTENSITY",
                                    color = LuxuryColors.WarmGray,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.8.sp
                                )
                                ResponsiveOptionGrid(
                                    options = intensities,
                                    selectedOption = hapticIntensity,
                                    accentColor = activeAccentColor,
                                    onOptionSelected = { intensity -> scope.launch { settingsRepository.setHapticIntensity(intensity) } }
                                )
                            }
                            "Advanced" -> {
                                val volumeCounterScreenOffEnabled by settingsRepository.volumeCounterScreenOffEnabled.collectAsState(initial = false)
                                val layoutOrientation by settingsRepository.layoutOrientation.collectAsState(initial = "horizontal")

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "VOLUME KEYS COUNTER (SCREEN OFF)",
                                        color = LuxuryColors.CreamyWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    com.floating.stopwatch.ui.components.LuxuryToggle(
                                        checked = volumeCounterScreenOffEnabled,
                                        onCheckedChange = { scope.launch { settingsRepository.setVolumeCounterScreenOffEnabled(it) } },
                                        accentColor = LuxuryColors.AccentGold
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "VERTICAL DISPLAY ORIENTATION",
                                        color = LuxuryColors.CreamyWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    com.floating.stopwatch.ui.components.LuxuryToggle(
                                        checked = layoutOrientation == "vertical",
                                        onCheckedChange = { scope.launch { settingsRepository.setLayoutOrientation(if (it) "vertical" else "horizontal") } },
                                        accentColor = LuxuryColors.AccentGold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            AnimatedVisibility(
                visible = !isClosingMain,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                enter = slideInVertically(
                    animationSpec = tween(180),
                    initialOffsetY = { -it }
                ) + fadeIn(animationSpec = tween(150)),
                exit = slideOutVertically(
                    animationSpec = tween(150),
                    targetOffsetY = { -it }
                ) + fadeOut(animationSpec = tween(120))
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                        .padding(horizontal = 12.dp)
                        .heightIn(max = panelMaxHeight)
                        .wrapContentHeight()
                        .statusBarsPadding()
                        .imePadding(),
                    shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
                    color = LuxuryColors.WarmBlack,
                    border = BorderStroke(1.dp, activeAccentColor.copy(alpha = 0.22f)),
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .widthIn(max = 720.dp)
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SETTINGS",
                                    color = LuxuryColors.CreamyWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = 3.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "SETTINGS CATEGORIES",
                                    color = LuxuryColors.WarmGray,
                                    fontSize = 10.sp,
                                    letterSpacing = 2.sp
                                )
                            }
                            Text(
                                text = "CLOSE",
                                color = LuxuryColors.WarmGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier
                                    .clickable { dismissSettings() }
                                    .padding(10.dp)
                            )
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 148.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    min = 220.dp,
                                    max = (panelMaxHeight - 100.dp).coerceAtLeast(220.dp)
                                ),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                            contentPadding = PaddingValues(bottom = 4.dp)
                        ) {
                            items(categories, key = { it }) { categoryName ->
                                val isPriority = categoryName == "Appearance" || categoryName == "Stopwatch"
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isPriority) Color(0xFF151412) else Color(0xFF111111)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isPriority) activeAccentColor.copy(alpha = 0.2f)
                                        else Color.White.copy(alpha = 0.08f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .clickable { activeDialogCategory = categoryName }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp, vertical = 9.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    activeAccentColor.copy(alpha = 0.8f),
                                                    RoundedCornerShape(3.dp)
                                                )
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Text(
                                                text = categoryName.uppercase(),
                                                color = LuxuryColors.CreamyWhite,
                                                fontSize = 11.sp,
                                                lineHeight = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.8.sp,
                                                maxLines = 2,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "▸",
                                                color = activeAccentColor.copy(alpha = 0.8f),
                                                fontSize = 13.sp
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
    }
}

@Composable
private fun ResponsiveOptionGrid(
    options: List<String>,
    selectedOption: String,
    accentColor: Color,
    onOptionSelected: (String) -> Unit,
    displayName: (String) -> String = { it }
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gridGap = 8.dp
        val minCardWidth = 148.dp
        val columns = when {
            maxWidth >= minCardWidth * 3 + gridGap * 2 -> 3
            maxWidth >= minCardWidth * 2 + gridGap -> 2
            else -> 1
        }

        Column(verticalArrangement = Arrangement.spacedBy(gridGap)) {
            options.chunked(columns).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gridGap)
                ) {
                    rowOptions.forEach { option ->
                        val isSelected = selectedOption == option
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) accentColor.copy(alpha = 0.12f) else Color(0xFF121212)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) accentColor.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.07f)
                            ),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .clickable { onOptionSelected(option) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onOptionSelected(option) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = accentColor,
                                        unselectedColor = LuxuryColors.WarmGray.copy(alpha = 0.65f)
                                    )
                                )
                                Text(
                                    text = displayName(option),
                                    color = if (isSelected) LuxuryColors.CreamyWhite else LuxuryColors.WarmGray,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    maxLines = 2,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    repeat(columns - rowOptions.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun formatSettingsDuration(totalSeconds: Int): String {
    return String.format("%02d:%02d", totalSeconds / 3600, (totalSeconds % 3600) / 60)
}

@Composable
fun WidgetCategorySettings(
    index: Int,
    widgetTitle: String,
    settingsRepository: SettingsRepository,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val isWidgetActive by settingsRepository.isWidgetActive(index).collectAsState(initial = index == 0)
    val wWidth by settingsRepository.getWidgetWidth(index).collectAsState(initial = 170.0f)
    val wHeight by settingsRepository.getWidgetHeight(index).collectAsState(initial = 56.0f)
    val saveDimensions by settingsRepository.getWidgetSaveDimensions(index).collectAsState(initial = true)
    val fontSizeScale by settingsRepository.getWidgetFontSizeScale(index).collectAsState(initial = 1.0f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ENABLE $widgetTitle OVERLAY",
            color = LuxuryColors.CreamyWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        com.floating.stopwatch.ui.components.LuxuryToggle(
            checked = isWidgetActive,
            onCheckedChange = { scope.launch { settingsRepository.setWidgetActive(index, it) } },
            accentColor = LuxuryColors.AccentGold
        )
    }

    if (isWidgetActive) {
        Spacer(modifier = Modifier.height(8.dp))

        DragAdjustField(
            label = "WIDTH",
            value = wWidth,
            minValue = 1f,
            maxValue = 320f,
            pixelsPerUnit = 1.5f,
            accentColor = LuxuryColors.AccentGold,
            valueFormatter = { "${it.toInt()}dp" },
            onValueChange = { scope.launch { settingsRepository.setWidgetWidth(index, it) } }
        )

        Spacer(modifier = Modifier.height(8.dp))

        DragAdjustField(
            label = "HEIGHT",
            value = wHeight,
            minValue = 1f,
            maxValue = 120f,
            pixelsPerUnit = 2.5f,
            accentColor = LuxuryColors.AccentGold,
            valueFormatter = { "${it.toInt()}dp" },
            onValueChange = { scope.launch { settingsRepository.setWidgetHeight(index, it) } }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SAVE FLOATING DIMENSIONS",
                color = LuxuryColors.CreamyWhite,
                fontSize = 10.sp
            )
            com.floating.stopwatch.ui.components.LuxuryToggle(
                checked = saveDimensions,
                onCheckedChange = { scope.launch { settingsRepository.setWidgetSaveDimensions(index, it) } },
                accentColor = LuxuryColors.AccentGold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        DragAdjustField(
            label = "FONT SIZE SCALE",
            value = fontSizeScale,
            minValue = 0.5f,
            maxValue = 1.5f,
            pixelsPerUnit = 180f,
            accentColor = LuxuryColors.AccentGold,
            valueFormatter = { String.format("%.2f", it) },
            onValueChange = { scope.launch { settingsRepository.setWidgetFontSizeScale(index, it) } }
        )
    }
}
