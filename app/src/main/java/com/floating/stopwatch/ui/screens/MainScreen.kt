package com.floating.stopwatch.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import android.content.Intent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floating.stopwatch.domain.CompletionSoundPlayer
import com.floating.stopwatch.domain.IntervalEngine
import com.floating.stopwatch.domain.IntervalStage
import com.floating.stopwatch.domain.IntervalStageType
import com.floating.stopwatch.domain.IntervalState
import com.floating.stopwatch.domain.IntervalTemplate
import com.floating.stopwatch.domain.Lap
import com.floating.stopwatch.domain.StopwatchState
import com.floating.stopwatch.ui.AppMode
import com.floating.stopwatch.ui.MainViewModel
import com.floating.stopwatch.ui.components.TimeDisplay
import com.floating.stopwatch.ui.components.DragAdjustField
import com.floating.stopwatch.data.SettingsRepository
import com.floating.stopwatch.ui.theme.LuxuryColors
import com.floating.stopwatch.domain.HapticController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    settingsRepository: SettingsRepository,
    hapticController: HapticController,
    hapticIntensity: String,
    showCentiseconds: Boolean,
    mainSize: Float,
    accentColor: Color,
    themeMode: String,
    onNavigateToSettings: () -> Unit,
    onNavigateToLegacy: () -> Unit = {}
) {
    val currentMode by viewModel.currentMode.collectAsState()
    val state by viewModel.state.collectAsState()
    val elapsedTimeMs by viewModel.elapsedTimeMs.collectAsState()
    val countdownRemainingMs by viewModel.countdownRemainingMs.collectAsState()
    val isCountdownRunning by viewModel.isCountdownRunning.collectAsState()
    val counterValue by viewModel.counterValue.collectAsState()
    val laps by viewModel.laps.collectAsState()

    val countdownDigitSize = (54f * mainSize).sp
    val counterDigitSize = (72f * mainSize).sp

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    val intervalEngine = viewModel.intervalEngine
    val intervalState by intervalEngine.state.collectAsState()

    val isCurrentlyRunning = when (currentMode) {
        AppMode.Stopwatch -> state == StopwatchState.Running
        AppMode.Countdown -> isCountdownRunning
        AppMode.Counter -> false
        AppMode.Intervals -> intervalState == IntervalState.RUNNING
        AppMode.TimeLegacy -> false
    }

    // Controls and Secondary Information Auto-Hide State
    var areControlsVisible by remember { mutableStateOf(true) }
    var isSecondaryVisible by remember { mutableStateOf(true) }
    var autoHideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun resetAutoHideTimer() {
        areControlsVisible = true
        isSecondaryVisible = true
        autoHideJob?.cancel()
        if (isCurrentlyRunning) {
            autoHideJob = scope.launch {
                kotlinx.coroutines.delay(3000L)
                areControlsVisible = false
                isSecondaryVisible = false
            }
        }
    }

    LaunchedEffect(isCurrentlyRunning, currentMode) {
        resetAutoHideTimer()
    }

    DisposableEffect(Unit) {
        onDispose {
            autoHideJob?.cancel()
        }
    }

    // Milestone Haptics for Counter (11, 33, 66, 99, 100)
    val milestoneSet = remember { setOf(11L, 33L, 66L, 99L, 100L) }
    var lastSignalledMilestone by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(counterValue) {
        if (counterValue in milestoneSet && counterValue != lastSignalledMilestone) {
            hapticController.trigger(hapticIntensity, "CounterMilestone")
            lastSignalledMilestone = counterValue
        } else if (counterValue !in milestoneSet) {
            lastSignalledMilestone = null
        }
    }

    // Completion Sound & Session Persistence Triggers
    var lastCompletedCountdownTime by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(countdownRemainingMs, isCountdownRunning) {
        if (countdownRemainingMs == 0L && !isCountdownRunning && lastCompletedCountdownTime != 0L && lastCompletedCountdownTime != null) {
            CompletionSoundPlayer.playCompletionClick()
            val configured = viewModel.countdownInitialMs.value
            if (configured >= 1000L) {
                scope.launch {
                    settingsRepository.recordCompletedSession("Countdown Focus", "countdown", configured)
                }
            }
            lastCompletedCountdownTime = 0L
        } else if (countdownRemainingMs > 0L) {
            lastCompletedCountdownTime = countdownRemainingMs
        }
    }

    var lastSignalledIntervalState by remember { mutableStateOf<IntervalState?>(null) }

    LaunchedEffect(intervalState) {
        if (intervalState == IntervalState.COMPLETED && lastSignalledIntervalState != IntervalState.COMPLETED) {
            CompletionSoundPlayer.playCompletionClick()
            val totalConfigured = intervalEngine.activeTemplate.value?.let {
                (it.workDurationMs + it.restDurationMs) * it.repetitions
            } ?: 0L
            if (totalConfigured >= 1000L) {
                scope.launch {
                    settingsRepository.recordCompletedSession("Interval Training", "interval", totalConfigured)
                }
            }
            lastSignalledIntervalState = IntervalState.COMPLETED
        } else if (intervalState != IntervalState.COMPLETED) {
            lastSignalledIntervalState = intervalState
        }
    }

    val controlsAlpha by animateFloatAsState(
        targetValue = if (areControlsVisible) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 250),
        label = "ControlsAlpha"
    )

    val secondaryAlpha by animateFloatAsState(
        targetValue = if (isSecondaryVisible) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 250),
        label = "SecondaryAlpha"
    )

    // Start/Stop pulse animations
    var triggerPulse by remember { mutableStateOf(false) }
    val scalePulse by animateFloatAsState(
        targetValue = if (triggerPulse) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        finishedListener = {
            if (triggerPulse) triggerPulse = false
        },
        label = "Pulse"
    )

    var isCounterAmbientDim by remember { mutableStateOf(false) }

    if (currentMode == AppMode.Counter && isCounterAmbientDim) {
        val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
        DisposableEffect(isCounterAmbientDim) {
            val window = activity?.window
            val prevBrightness = window?.attributes?.screenBrightness ?: android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window?.attributes = window?.attributes?.apply {
                screenBrightness = 0.01f
            }
            onDispose {
                window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                window?.attributes = window?.attributes?.apply {
                    screenBrightness = prevBrightness
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            isCounterAmbientDim = false
                        }
                    )
                }
        )
        return
    }

    // Layout configuration based on the illumination Mode
    val currentBgColor = when (themeMode) {
        "Midnight", "Midnight Dark", "Obsidian Dark" -> Color(0xFF000000)
        "Warm Paper", "Warm Paper Light" -> Color(0xFFF7F5F0)
        "Pure White Light" -> Color(0xFFFFFFFF)
        else -> LuxuryColors.WarmBlack
    }

    val currentTextColor = when (themeMode) {
        "Warm Paper", "Warm Paper Light", "Pure White Light" -> Color(0xFF1C1A17)
        else -> LuxuryColors.CreamyWhite
    }

    val currentGrayColor = when (themeMode) {
        "Warm Paper", "Warm Paper Light", "Pure White Light" -> Color(0xFF6B6661)
        else -> LuxuryColors.WarmGray
    }

    var totalDragY by remember { mutableFloatStateOf(0f) }
    var totalDragX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentBgColor)
            .padding(24.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        resetAutoHideTimer()
                    }
                )
            }
            .pointerInput(currentMode) {
                detectDragGestures(
                    onDragStart = {
                        totalDragY = 0f
                        totalDragX = 0f
                        resetAutoHideTimer()
                    },
                    onDrag = { _, dragAmount ->
                        totalDragY += dragAmount.y
                        totalDragX += dragAmount.x
                    },
                    onDragEnd = {
                        if (kotlin.math.abs(totalDragY) > 80f && kotlin.math.abs(totalDragY) > 1.5f * kotlin.math.abs(totalDragX)) {
                            if (totalDragY < 0) {
                                hapticController.trigger(hapticIntensity, "Lap")
                                viewModel.cycleMode()
                            } else {
                                hapticController.trigger(hapticIntensity, "Lap")
                                viewModel.previousMode()
                            }
                        }
                        totalDragY = 0f
                        totalDragX = 0f
                    },
                    onDragCancel = {
                        totalDragY = 0f
                        totalDragX = 0f
                    }
                )
            }
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current

        var isSettingsMenuOpen by remember { mutableStateOf(false) }
        var activeCategory by remember { mutableStateOf<String?>(null) }

        if (activeCategory != null || isSettingsMenuOpen) {
            androidx.activity.compose.BackHandler(enabled = true) {
                if (activeCategory != null) {
                    activeCategory = null
                } else {
                    isSettingsMenuOpen = false
                }
            }
        }

        val settingsCategoryList = remember {
            listOf(
                "APPEARANCE", "STOPWATCH", "COUNTDOWN", "COUNTER", "INTERVAL",
                "SOUNDS & HAPTICS", "FLOATING WIDGETS", "ADVANCED"
            )
        }

        // Top Right: Floating Quick Access & Settings
        // Fixed absolute top layout so FLOAT stays at exactly 32.dp top padding in both open and closed states
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .graphicsLayer { alpha = controlsAlpha }
        ) {
            if (!isSettingsMenuOpen) {
                Text(
                    text = "SETTINGS",
                    style = TextStyle(
                        color = currentGrayColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp)
                        .clickable {
                            resetAutoHideTimer()
                            isSettingsMenuOpen = true
                        }
                        .padding(8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "FLOAT ↗",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier
                        .clickable {
                            resetAutoHideTimer()
                            if (android.provider.Settings.canDrawOverlays(context)) {
                                val intent = Intent(context, com.floating.stopwatch.service.StopwatchService::class.java)
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                                val targetIndex = when (currentMode) {
                                    AppMode.Stopwatch -> 0
                                    AppMode.Countdown -> 1
                                    AppMode.Counter -> 2
                                    AppMode.Intervals -> 3
                                    AppMode.TimeLegacy -> 0
                                }
                                val targetType = when (currentMode) {
                                    AppMode.Stopwatch -> "stopwatch"
                                    AppMode.Countdown -> "countdown"
                                    AppMode.Counter -> "counter"
                                    AppMode.Intervals -> "intervals"
                                    AppMode.TimeLegacy -> "stopwatch"
                                }
                                scope.launch {
                                    settingsRepository.setWidgetType(targetIndex, targetType)
                                    settingsRepository.setWidgetActive(targetIndex, true)
                                }
                            } else {
                                val intent = Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                if (isSettingsMenuOpen) {
                    Spacer(modifier = Modifier.height(2.dp))
                    settingsCategoryList.forEach { cat ->
                        Text(
                            text = cat,
                            style = TextStyle(
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 2.sp
                            ),
                            modifier = Modifier
                                .clickable {
                                    resetAutoHideTimer()
                                    activeCategory = cat
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (activeCategory != null) {
            SettingsCategoryPopup(
                category = activeCategory!!,
                settingsRepository = settingsRepository,
                accentColor = accentColor,
                onDismiss = { activeCategory = null }
            )
        }

        // Top label - Tapping cycles mode (Stopwatch -> Countdown -> Counter -> Intervals -> Stopwatch)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp)
                .graphicsLayer { alpha = controlsAlpha }
        ) {
            Text(
                text = when (currentMode) {
                    AppMode.Stopwatch -> "STOPWATCH ▾"
                    AppMode.Countdown -> "COUNTDOWN ▾"
                    AppMode.Counter -> "COUNTER ▾"
                    AppMode.Intervals -> "INTERVALS ▾"
                    AppMode.TimeLegacy -> "TIME LEGACY ▾"
                },
                style = TextStyle(
                    color = currentTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = 4.sp
                ),
                modifier = Modifier
                    .clickable {
                        resetAutoHideTimer()
                        hapticController.trigger(hapticIntensity, "Lap")
                        viewModel.cycleMode()
                    }
                    .padding(4.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "LEGACY ↗",
                style = TextStyle(
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier
                    .clickable {
                        resetAutoHideTimer()
                        viewModel.currentMode.value = AppMode.TimeLegacy
                    }
                    .padding(2.dp)
            )
        }

        // Breathing pulse animation when stopwatch is at 0 for more than 5 seconds
        val isAtZeroForFiveSecs = elapsedTimeMs == 0L && state == StopwatchState.Ready
        val infiniteTransition = rememberInfiniteTransition(label = "PulseAtZero")
        val breathingScale by if (isAtZeroForFiveSecs) {
            infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.03f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "BreathingScale"
            )
        } else {
            remember { mutableStateOf(1.0f) }
        }

        // Center display & controls depending on current AppMode
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (currentMode) {
                AppMode.Stopwatch -> {
                    TimeDisplay(
                        elapsedTimeMs = elapsedTimeMs,
                        showCentiseconds = showCentiseconds,
                        baseStyle = TextStyle(color = currentTextColor, fontSize = 54.sp),
                        scaleFactor = mainSize,
                        accentColor = accentColor,
                        gradientGoldEnabled = false,
                        modifier = Modifier
                            .scale(scalePulse * breathingScale)
                            .semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = state.name.uppercase(),
                        style = TextStyle(
                            color = currentGrayColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 3.sp
                        ),
                        modifier = Modifier.graphicsLayer { alpha = secondaryAlpha }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Ambient Dim Mode",
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.sp
                        ),
                        modifier = Modifier
                            .graphicsLayer { alpha = secondaryAlpha }
                            .clickable {
                                isCounterAmbientDim = true
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                AppMode.Countdown -> {
                    var hDragAcc by remember { mutableFloatStateOf(0f) }
                    var mDragAcc by remember { mutableFloatStateOf(0f) }
                    var sDragAcc by remember { mutableFloatStateOf(0f) }

                    val totalSeconds = countdownRemainingMs / 1000
                    val hours = totalSeconds / 3600
                    val minutes = (totalSeconds % 3600) / 60
                    val seconds = totalSeconds % 60

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.scale(scalePulse * breathingScale)
                        ) {
                            // 1. Top: Countdown Digits (HH : MM : SS) - ALWAYS VISIBLE
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Hours Drag Zone
                                Box(
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { resetAutoHideTimer() },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                hDragAcc += dragAmount.y
                                                if (hDragAcc <= -25f) {
                                                    viewModel.adjustCountdownHours(1)
                                                    hDragAcc = 0f
                                                } else if (hDragAcc >= 25f) {
                                                    viewModel.adjustCountdownHours(-1)
                                                    hDragAcc = 0f
                                                }
                                            },
                                            onDragEnd = { hDragAcc = 0f }
                                        )
                                    }
                                ) {
                                    Text(
                                        text = String.format("%02d", hours),
                                        style = TextStyle(color = currentTextColor, fontSize = countdownDigitSize, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Light)
                                    )
                                }

                                Text(" : ", style = TextStyle(color = currentTextColor, fontSize = countdownDigitSize, fontWeight = FontWeight.Light))

                                // Minutes Drag Zone
                                Box(
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { resetAutoHideTimer() },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                mDragAcc += dragAmount.y
                                                if (mDragAcc <= -25f) {
                                                    viewModel.adjustCountdownMinutes(1)
                                                    mDragAcc = 0f
                                                } else if (mDragAcc >= 25f) {
                                                    viewModel.adjustCountdownMinutes(-1)
                                                    mDragAcc = 0f
                                                }
                                            },
                                            onDragEnd = { mDragAcc = 0f }
                                        )
                                    }
                                ) {
                                    Text(
                                        text = String.format("%02d", minutes),
                                        style = TextStyle(color = currentTextColor, fontSize = countdownDigitSize, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Light)
                                    )
                                }

                                Text(" : ", style = TextStyle(color = currentTextColor, fontSize = countdownDigitSize, fontWeight = FontWeight.Light))

                                // Seconds Drag Zone
                                Box(
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { resetAutoHideTimer() },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                sDragAcc += dragAmount.y
                                                if (sDragAcc <= -25f) {
                                                    viewModel.adjustCountdownSeconds(1)
                                                    sDragAcc = 0f
                                                } else if (sDragAcc >= 25f) {
                                                    viewModel.adjustCountdownSeconds(-1)
                                                    sDragAcc = 0f
                                                }
                                            },
                                            onDragEnd = { sDragAcc = 0f }
                                        )
                                    }
                                ) {
                                    Text(
                                        text = String.format("%02d", seconds),
                                        style = TextStyle(color = currentTextColor, fontSize = countdownDigitSize, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Light)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 2. Middle: Sub-Labels HOURS : MINS : SECS aligned under numbers
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.graphicsLayer { alpha = secondaryAlpha }
                            ) {
                                Text(
                                    text = "HOURS",
                                    style = TextStyle(color = currentGrayColor, fontSize = 11.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp)
                                )
                                Text(" : ", style = TextStyle(color = currentGrayColor, fontSize = 11.sp, fontWeight = FontWeight.Light))
                                Text(
                                    text = "MINS",
                                    style = TextStyle(color = currentGrayColor, fontSize = 11.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp)
                                )
                                Text(" : ", style = TextStyle(color = currentGrayColor, fontSize = 11.sp, fontWeight = FontWeight.Light))
                                Text(
                                    text = "SECS",
                                    style = TextStyle(color = currentGrayColor, fontSize = 11.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 3. Bottom: Instruction text
                            Text(
                                text = if (!isCountdownRunning) "DRAG UP/DOWN TO ADJUST" else "FOCUS COUNTDOWN",
                                style = TextStyle(
                                    color = currentGrayColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = 2.sp
                                ),
                                modifier = Modifier.graphicsLayer { alpha = secondaryAlpha }
                            )
                        }
                    }
                }
                AppMode.Counter -> {
                    Text(
                        text = "$counterValue",
                        style = TextStyle(
                            color = currentTextColor,
                            fontSize = counterDigitSize,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        modifier = Modifier.scale(scalePulse)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "TAP COUNTER",
                        style = TextStyle(
                            color = currentGrayColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 3.sp
                        ),
                        modifier = Modifier.graphicsLayer { alpha = secondaryAlpha }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Ambient Dim Mode",
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.sp
                        ),
                        modifier = Modifier
                            .graphicsLayer { alpha = secondaryAlpha }
                            .clickable {
                                isCounterAmbientDim = true
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                AppMode.Intervals -> {
                    val activeTemplate by intervalEngine.activeTemplate.collectAsState()
                    val currentRound by intervalEngine.currentRound.collectAsState()
                    val stageRemainingMs by intervalEngine.stageRemainingMs.collectAsState()
                    val currentStage = intervalEngine.getCurrentStage()
                    val nextStage = intervalEngine.getNextStage()

                    var showBuilderDialog by remember { mutableStateOf(false) }

                    if (activeTemplate != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = activeTemplate.name.uppercase(),
                                    style = TextStyle(color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "[EDIT]",
                                    style = TextStyle(color = currentGrayColor, fontSize = 10.sp, fontWeight = FontWeight.Light, letterSpacing = 1.sp),
                                    modifier = Modifier
                                        .graphicsLayer { alpha = secondaryAlpha }
                                        .clickable {
                                            resetAutoHideTimer()
                                            showBuilderDialog = true
                                        }
                                        .padding(4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = currentStage?.name?.uppercase() ?: "READY",
                                style = TextStyle(
                                    color = if (currentStage?.type == IntervalStageType.WORK) accentColor else currentTextColor,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 3.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            TimeDisplay(
                                elapsedTimeMs = stageRemainingMs,
                                showCentiseconds = true,
                                baseStyle = TextStyle(color = currentTextColor, fontSize = 48.sp),
                                scaleFactor = mainSize,
                                accentColor = accentColor,
                                modifier = Modifier.scale(scalePulse)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "ROUND $currentRound / ${activeTemplate!!.repetitions}",
                                style = TextStyle(color = currentGrayColor, fontSize = 12.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp),
                                modifier = Modifier.graphicsLayer { alpha = secondaryAlpha }
                            )

                            if (nextStage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val nextSecs = nextStage.durationMs / 1000
                                Text(
                                    text = "NEXT: ${nextStage.name} (${nextSecs}s)",
                                    style = TextStyle(color = currentGrayColor.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp),
                                    modifier = Modifier.graphicsLayer { alpha = secondaryAlpha }
                                )
                            }
                        }
                    }

                    if (showBuilderDialog && activeTemplate != null) {
                        IntervalQuickEditDialog(
                            initialTemplate = activeTemplate,
                            onDismiss = { showBuilderDialog = false },
                            onSave = { updatedTemplate ->
                                intervalEngine.loadTemplate(updatedTemplate)
                                scope.launch {
                                    settingsRepository.setIntervalConfig(
                                        name = updatedTemplate.name,
                                        workMs = updatedTemplate.workDurationMs,
                                        restMs = updatedTemplate.restDurationMs,
                                        rounds = updatedTemplate.repetitions
                                    )
                                }
                                showBuilderDialog = false
                            }
                        )
                    }
                }
                AppMode.TimeLegacy -> {
                    com.floating.stopwatch.ui.legacy.TimeLegacyScreen(
                        settingsRepository = settingsRepository,
                        onBack = { viewModel.cycleMode() }
                    )
                }
            }
        }

        // Action Buttons Row depending on AppMode
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 54.dp)
                .graphicsLayer { alpha = controlsAlpha },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (currentMode) {
                AppMode.Intervals -> {
                    // Reset / Stop button
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .clickable {
                                resetAutoHideTimer()
                                hapticController.trigger(hapticIntensity, "Reset")
                                intervalEngine.reset()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, currentGrayColor)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "RESET",
                                    style = TextStyle(color = currentTextColor, fontSize = 11.sp, letterSpacing = 1.sp)
                                )
                            }
                        }
                    }

                    val isRunning = intervalState == IntervalState.RUNNING
                    val intervalBtnColor = if (isRunning) Color(0xFF9E2A2B) else accentColor
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(intervalBtnColor)
                            .clickable {
                                resetAutoHideTimer()
                                if (isRunning) {
                                    hapticController.trigger(hapticIntensity, "Stop")
                                    intervalEngine.pause()
                                } else {
                                    hapticController.trigger(hapticIntensity, "Start")
                                    intervalEngine.start(scope)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRunning) "PAUSE" else "START",
                            style = TextStyle(color = LuxuryColors.WarmBlack, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        )
                    }
                }
                AppMode.Stopwatch -> {
                    // Lap / Reset button
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .clickable {
                                resetAutoHideTimer()
                                if (state == StopwatchState.Running) {
                                    hapticController.trigger(hapticIntensity, "Lap")
                                    viewModel.lap()
                                } else if (state == StopwatchState.Paused) {
                                    hapticController.trigger(hapticIntensity, "Reset")
                                    if (elapsedTimeMs >= 3000L) {
                                        scope.launch {
                                            settingsRepository.recordCompletedSession("Stopwatch Session", "stopwatch", elapsedTimeMs)
                                        }
                                    }
                                    viewModel.reset()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, currentGrayColor)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (state == StopwatchState.Paused) "RESET" else "LAP",
                                    style = TextStyle(
                                        color = currentTextColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        }
                    }

                    // Big Start/Stop golden button
                    val buttonColor = if (state == StopwatchState.Running) Color(0xFF9E2A2B) else accentColor
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(buttonColor)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        resetAutoHideTimer()
                                        triggerPulse = true
                                        tryAwaitRelease()
                                        triggerPulse = false
                                    },
                                    onTap = {
                                        resetAutoHideTimer()
                                        if (state == StopwatchState.Running) {
                                            hapticController.trigger(hapticIntensity, "Stop")
                                            viewModel.pause()
                                        } else {
                                            hapticController.trigger(hapticIntensity, "Start")
                                            viewModel.start()
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state == StopwatchState.Running) "STOP" else "START",
                            style = TextStyle(
                                color = LuxuryColors.WarmBlack,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
                AppMode.Countdown -> {
                    // Countdown controls
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .clickable {
                                resetAutoHideTimer()
                                hapticController.trigger(hapticIntensity, "Reset")
                                viewModel.resetCountdown()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, currentGrayColor)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "RESET",
                                    style = TextStyle(color = currentTextColor, fontSize = 11.sp, letterSpacing = 1.sp)
                                )
                            }
                        }
                    }

                    val countdownBtnColor = if (isCountdownRunning) Color(0xFF9E2A2B) else accentColor
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(countdownBtnColor)
                            .clickable {
                                resetAutoHideTimer()
                                if (isCountdownRunning) {
                                    hapticController.trigger(hapticIntensity, "Stop")
                                    viewModel.pauseCountdown()
                                } else {
                                    hapticController.trigger(hapticIntensity, "Start")
                                    viewModel.startCountdown()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCountdownRunning) "PAUSE" else "START",
                            style = TextStyle(color = LuxuryColors.WarmBlack, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        )
                    }
                }
                AppMode.Counter -> {
                    // Reset Button (0.5-Second Continuous Press)
                    var isPressingReset by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        resetAutoHideTimer()
                                        isPressingReset = true
                                        var resetTriggered = false
                                        val job = scope.launch {
                                            kotlinx.coroutines.delay(500L)
                                            resetTriggered = true
                                            hapticController.trigger(hapticIntensity, "Reset")
                                            viewModel.resetCounter()
                                        }
                                        val released = tryAwaitRelease()
                                        isPressingReset = false
                                        if (!resetTriggered) {
                                            job.cancel()
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (isPressingReset) accentColor else currentGrayColor
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "RESET",
                                    style = TextStyle(
                                        color = if (isPressingReset) accentColor else currentTextColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        }
                    }

                    // Decrement (-1) Button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .clickable {
                                resetAutoHideTimer()
                                hapticController.trigger(hapticIntensity, "Lap")
                                viewModel.decrementCounter()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, currentGrayColor)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "− 1",
                                    style = TextStyle(color = currentTextColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    // Increment (+1) Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                            .clickable {
                                resetAutoHideTimer()
                                hapticController.trigger(hapticIntensity, "Lap")
                                viewModel.incrementCounter()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ 1",
                            style = TextStyle(color = LuxuryColors.WarmBlack, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
                AppMode.TimeLegacy -> {
                    // Handled internally by TimeLegacyScreen
                }
            }
        }

        // Small indicator link to check laps bottom sheet
        if (laps.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .graphicsLayer { alpha = controlsAlpha }
                    .clickable {
                        resetAutoHideTimer()
                        showBottomSheet = true
                    }
                    .padding(8.dp)
            ) {
                Text(
                    text = "VIEW LAPS (${laps.size})",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    )
                )
            }
        }
    }

    // Slide-up bottom sheet for luxury clean laps listing
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = currentBgColor,
            dragHandle = { BottomSheetDefaults.DragHandle(color = currentGrayColor) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "LAP TIMES & INSIGHTS",
                    style = TextStyle(
                        color = currentTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraLight,
                        letterSpacing = 4.sp
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val fastestLap = if (laps.size >= 2) laps.minByOrNull { it.lapTimeMs } else null
                val slowestLap = if (laps.size >= 2) laps.maxByOrNull { it.lapTimeMs } else null

                // Performance Session Insights (P1 - Item 6)
                if (laps.isNotEmpty()) {
                    val avgLapTime = laps.map { it.lapTimeMs }.average().toLong()
                    val avgMins = (avgLapTime / 1000) / 60
                    val avgSecs = (avgLapTime / 1000) % 60
                    val avgCents = (avgLapTime % 1000) / 10
                    val formattedAvg = String.format("%02d:%02d.%02d", avgMins, avgSecs, avgCents)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = currentGrayColor.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SESSION INSIGHTS",
                                    color = currentGrayColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                                // Share Card CTA Button (P1 - Item 9)
                                val context = androidx.compose.ui.platform.LocalContext.current
                                Text(
                                    text = "SHARE CARD",
                                    color = accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier
                                        .clickable {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "Stopwatch Premium Session")
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "🏆 STOPWATCH PREMIUM SESSION CARD 🏆\n" +
                                                    "------------------------------\n" +
                                                    "• Average Lap Time: $formattedAvg\n" +
                                                    (if (fastestLap != null) "• Fastest Cycle: Lap ${fastestLap.lapIndex} (${fastestLap.lapTimeMs / 1000f}s)\n" else "") +
                                                    "• Total Laps Count: ${laps.size}\n" +
                                                    "------------------------------\n" +
                                                    "Luxury Minimalist Stopwatch System"
                                                )
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Premium Session Info"))
                                        }
                                        .padding(4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "AVERAGE LAP", color = currentGrayColor, fontSize = 11.sp)
                                    Text(text = formattedAvg, color = currentTextColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                                if (fastestLap != null) {
                                    Column {
                                        Text(text = "FASTEST LAP", color = Color(0xFF4AC98F), fontSize = 11.sp)
                                        val fS = fastestLap.lapTimeMs / 1000
                                        Text(text = "LAP ${fastestLap.lapIndex} (${fS}s)", color = currentTextColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(laps.reversed()) { lap ->
                        val lapColor = when {
                            fastestLap != null && lap.lapIndex == fastestLap.lapIndex -> Color(0xFF4AC98F) // elegant green
                            slowestLap != null && lap.lapIndex == slowestLap.lapIndex -> Color(0xFFC94A4A) // subtle red
                            else -> currentTextColor
                        }
                        LapRowItem(
                            lap = lap,
                            textColor = lapColor,
                            grayColor = currentGrayColor
                        )
                        Divider(color = currentGrayColor.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

fun formatIntervalDuration(totalSecs: Int): String {
    val hrs = totalSecs / 3600
    val mins = (totalSecs % 3600) / 60
    val secs = totalSecs % 60
    return if (hrs > 0) {
        String.format("%dh %02dm %02ds", hrs, mins, secs)
    } else if (mins > 0) {
        String.format("%dm %02ds", mins, secs)
    } else {
        "${secs}s"
    }
}

fun formatDurationHoursMinutes(totalSecs: Int): String {
    return String.format("%02d:%02d", totalSecs / 3600, (totalSecs % 3600) / 60)
}

@Composable
fun IntervalQuickEditDialog(
    initialTemplate: IntervalTemplate,
    onDismiss: () -> Unit,
    onSave: (IntervalTemplate) -> Unit
) {
    var templateName by remember { mutableStateOf(initialTemplate.name) }
    var workSecs by remember { mutableIntStateOf((initialTemplate.workDurationMs / 1000).toInt()) }
    var restSecs by remember { mutableIntStateOf((initialTemplate.restDurationMs / 1000).toInt()) }
    var repetitions by remember { mutableIntStateOf(initialTemplate.repetitions) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "INTERVAL CONFIGURATION",
                    style = TextStyle(color = LuxuryColors.AccentGold, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
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
                    accentColor = LuxuryColors.AccentGold,
                    valueFormatter = { formatDurationHoursMinutes(it.toInt()) },
                    onValueChange = { workSecs = it.toInt().coerceIn(1, 18000) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                DragAdjustField(
                    label = "REST DURATION",
                    value = restSecs.toFloat(),
                    minValue = 1f,
                    maxValue = 3600f,
                    pixelsPerUnit = 4f,
                    accentColor = LuxuryColors.AccentGold,
                    valueFormatter = { formatDurationHoursMinutes(it.toInt()) },
                    onValueChange = { restSecs = it.toInt().coerceIn(1, 3600) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ROUNDS: $repetitions", color = LuxuryColors.CreamyWhite, fontSize = 11.sp)
                    Row {
                        Box(
                            modifier = Modifier
                                .clickable { if (repetitions > 1) repetitions -= 1 }
                                .padding(8.dp)
                        ) {
                            Text("-", color = LuxuryColors.AccentGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clickable { repetitions += 1 }
                                .padding(8.dp)
                        ) {
                            Text("+", color = LuxuryColors.AccentGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            val updated = IntervalTemplate(
                                id = initialTemplate.id,
                                name = templateName.ifBlank { "HIT" },
                                workDurationMs = workSecs * 1000L,
                                restDurationMs = restSecs * 1000L,
                                repetitions = repetitions
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryColors.AccentGold)
                    ) {
                        Text("SAVE", color = LuxuryColors.WarmBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCategoryPopup(
    category: String,
    settingsRepository: SettingsRepository,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 520.dp)
                .wrapContentHeight()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0D0D0D),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.uppercase(),
                        style = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    )
                    Text(
                        text = "✕",
                        style = TextStyle(color = LuxuryColors.WarmGray, fontSize = 16.sp),
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                CategoryContent(
                    categoryName = category,
                    settingsRepository = settingsRepository,
                    accentColor = accentColor,
                    scope = scope
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1C1C1E))
                        .clickable { onDismiss() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CLOSE",
                        style = TextStyle(
                            color = LuxuryColors.CreamyWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryContent(
    categoryName: String,
    settingsRepository: SettingsRepository,
    accentColor: Color,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val stylePreset by settingsRepository.stylePreset.collectAsState(initial = "Glass Premium")
    val colorPreset by settingsRepository.colorPreset.collectAsState(initial = "Gold")
    val hapticIntensity by settingsRepository.hapticIntensity.collectAsState(initial = "Medium")
    val themeMode by settingsRepository.themeMode.collectAsState(initial = "Midnight")
    val mainDisplayScale by settingsRepository.mainDisplayScale.collectAsState(initial = 1.0f)

    val shapes = listOf("rounded", "capsule", "circle", "sharp", "glass")
    val themeModes = listOf("Midnight Dark", "Warm Paper Light", "Obsidian Dark", "Pure White Light")
    val presets = listOf("Glass Premium", "Obsidian", "Titanium", "Ultra Minimal")
    val intensities = listOf("Off", "Light", "Medium", "Strong")
    val colorPresets = listOf("Gold", "Galaxy Blue", "Titanium", "Emerald", "Sapphire", "Violet", "Rose", "Ice", "Amber", "Pure White")

    when (categoryName.uppercase()) {
        "APPEARANCE" -> {
            Text("ILLUMINATION MODE", color = LuxuryColors.WarmGray, fontSize = 10.sp, letterSpacing = 1.8.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                themeModes.forEach { mode ->
                    val isSel = themeMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) accentColor.copy(alpha = 0.2f) else Color(0xFF161616))
                            .clickable { scope.launch { settingsRepository.setThemeMode(mode) } }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(mode.take(8), color = if (isSel) LuxuryColors.CreamyWhite else LuxuryColors.WarmGray, fontSize = 9.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("STYLE PRESET", color = LuxuryColors.WarmGray, fontSize = 10.sp, letterSpacing = 1.8.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                presets.forEach { preset ->
                    val isSel = stylePreset == preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) accentColor.copy(alpha = 0.2f) else Color(0xFF161616))
                            .clickable { scope.launch { settingsRepository.setStylePreset(preset) } }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(preset.take(8), color = if (isSel) LuxuryColors.CreamyWhite else LuxuryColors.WarmGray, fontSize = 9.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("COLOR ACCENT PRESET", color = LuxuryColors.WarmGray, fontSize = 10.sp, letterSpacing = 1.8.sp)
            LazyColumn(modifier = Modifier.height(110.dp)) {
                items(colorPresets.chunked(3)) { rowColors ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        rowColors.forEach { col ->
                            val isSel = colorPreset == col
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) accentColor.copy(alpha = 0.25f) else Color(0xFF161616))
                                    .clickable { scope.launch { settingsRepository.setColorPreset(col) } }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(col, color = if (isSel) LuxuryColors.CreamyWhite else LuxuryColors.WarmGray, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            DragAdjustField(
                label = "MAIN DISPLAY SCALE",
                value = mainDisplayScale,
                minValue = 0.7f,
                maxValue = 1.3f,
                pixelsPerUnit = 180f,
                accentColor = accentColor,
                valueFormatter = { String.format("%.2fx", it) },
                onValueChange = { scope.launch { settingsRepository.setMainDisplayScale(it) } }
            )
        }
        "STOPWATCH" -> WidgetSettingsBlock(index = 0, widgetTitle = "STOPWATCH", settingsRepository = settingsRepository, scope = scope, accentColor = accentColor)
        "COUNTDOWN" -> WidgetSettingsBlock(index = 1, widgetTitle = "COUNTDOWN", settingsRepository = settingsRepository, scope = scope, accentColor = accentColor)
        "COUNTER" -> WidgetSettingsBlock(index = 2, widgetTitle = "COUNTER", settingsRepository = settingsRepository, scope = scope, accentColor = accentColor)
        "INTERVAL" -> {
            val intervalName by settingsRepository.intervalName.collectAsState(initial = "HIT")
            val workMs by settingsRepository.intervalWorkMs.collectAsState(initial = 40000L)
            val restMs by settingsRepository.intervalRestMs.collectAsState(initial = 20000L)
            val rounds by settingsRepository.intervalRounds.collectAsState(initial = 8)

            var nameInput by remember(intervalName) { mutableStateOf(intervalName) }
            var workSecs by remember(workMs) { mutableIntStateOf((workMs / 1000).toInt()) }
            var restSecs by remember(restMs) { mutableIntStateOf((restMs / 1000).toInt()) }
            var roundsVal by remember(rounds) { mutableIntStateOf(rounds) }

            Text("INTERVAL CONFIGURATION", color = LuxuryColors.WarmGray, fontSize = 10.sp, letterSpacing = 1.8.sp)
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Interval Name", color = LuxuryColors.WarmGray, fontSize = 10.sp) },
                textStyle = TextStyle(color = LuxuryColors.CreamyWhite, fontSize = 12.sp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            DragAdjustField(
                label = "WORK DURATION",
                value = workSecs.toFloat(),
                minValue = 1f,
                maxValue = 18000f,
                pixelsPerUnit = 4f,
                accentColor = accentColor,
                valueFormatter = { formatDurationHoursMinutes(it.toInt()) },
                onValueChange = { workSecs = it.toInt().coerceIn(1, 18000) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            DragAdjustField(
                label = "REST DURATION",
                value = restSecs.toFloat(),
                minValue = 1f,
                maxValue = 3600f,
                pixelsPerUnit = 4f,
                accentColor = accentColor,
                valueFormatter = { formatDurationHoursMinutes(it.toInt()) },
                onValueChange = { restSecs = it.toInt().coerceIn(1, 3600) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ROUNDS: $roundsVal", color = LuxuryColors.CreamyWhite, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Row {
                    Box(modifier = Modifier.clickable { if (roundsVal > 1) roundsVal -= 1 }.padding(8.dp)) {
                        Text("-", color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.clickable { roundsVal += 1 }.padding(8.dp)) {
                        Text("+", color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SAVE CONFIGURATION", color = LuxuryColors.WarmBlack, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            WidgetSettingsBlock(index = 3, widgetTitle = "INTERVAL", settingsRepository = settingsRepository, scope = scope, accentColor = accentColor)
        }
        "SOUNDS & HAPTICS" -> {
            Text("HAPTIC INTENSITY", color = LuxuryColors.WarmGray, fontSize = 10.sp, letterSpacing = 1.8.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                intensities.forEach { intensity ->
                    val isSel = hapticIntensity == intensity
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) accentColor.copy(alpha = 0.2f) else Color(0xFF161616))
                            .clickable { scope.launch { settingsRepository.setHapticIntensity(intensity) } }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(intensity, color = if (isSel) LuxuryColors.CreamyWhite else LuxuryColors.WarmGray, fontSize = 10.sp)
                    }
                }
            }
        }
        "FLOATING WIDGETS" -> {
            val shapePreset by settingsRepository.shapePreset.collectAsState(initial = "rounded")
            val floatingPadding by settingsRepository.floatingPadding.collectAsState(initial = 6.0f)
            val floatingOpacity by settingsRepository.floatingOpacity.collectAsState(initial = 0.85f)

            Text("SHAPE PRESET", color = LuxuryColors.WarmGray, fontSize = 10.sp, letterSpacing = 1.8.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                shapes.forEach { shape ->
                    val isSel = shapePreset == shape
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) accentColor.copy(alpha = 0.2f) else Color(0xFF161616))
                            .clickable { scope.launch { settingsRepository.setShapePreset(shape) } }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(shape.uppercase(), color = if (isSel) LuxuryColors.CreamyWhite else LuxuryColors.WarmGray, fontSize = 9.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            DragAdjustField(
                label = "PADDING",
                value = floatingPadding,
                minValue = 0f,
                maxValue = 32f,
                pixelsPerUnit = 8f,
                accentColor = accentColor,
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
                accentColor = accentColor,
                valueFormatter = { "${(it * 100).toInt()}%" },
                onValueChange = { scope.launch { settingsRepository.setFloatingOpacity(it) } }
            )
        }
        "ADVANCED" -> {
            val volumeCounterScreenOffEnabled by settingsRepository.volumeCounterScreenOffEnabled.collectAsState(initial = false)
            val layoutOrientation by settingsRepository.layoutOrientation.collectAsState(initial = "horizontal")

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("VOLUME KEYS COUNTER", color = LuxuryColors.CreamyWhite, fontSize = 11.sp)
                Switch(
                    checked = volumeCounterScreenOffEnabled,
                    onCheckedChange = { scope.launch { settingsRepository.setVolumeCounterScreenOffEnabled(it) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("VERTICAL DISPLAY ORIENTATION", color = LuxuryColors.CreamyWhite, fontSize = 11.sp)
                Switch(
                    checked = layoutOrientation == "vertical",
                    onCheckedChange = { scope.launch { settingsRepository.setLayoutOrientation(if (it) "vertical" else "horizontal") } },
                    colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                )
            }
        }
    }
}

@Composable
fun WidgetSettingsBlock(
    index: Int,
    widgetTitle: String,
    settingsRepository: SettingsRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    accentColor: Color
) {
    val wWidth by settingsRepository.getWidgetWidth(index).collectAsState(initial = 170.0f)
    val wHeight by settingsRepository.getWidgetHeight(index).collectAsState(initial = 56.0f)
    val saveDimensions by settingsRepository.getWidgetSaveDimensions(index).collectAsState(initial = true)
    val fontSizeScale by settingsRepository.getWidgetFontSizeScale(index).collectAsState(initial = 1.0f)

    Column {
        DragAdjustField(
            label = "WIDTH",
            value = wWidth,
            minValue = 1f,
            maxValue = 320f,
            pixelsPerUnit = 1.5f,
            accentColor = accentColor,
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
            accentColor = accentColor,
            valueFormatter = { "${it.toInt()}dp" },
            onValueChange = { scope.launch { settingsRepository.setWidgetHeight(index, it) } }
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SAVE FLOATING DIMENSIONS", color = LuxuryColors.CreamyWhite, fontSize = 10.sp)
            Switch(
                checked = saveDimensions,
                onCheckedChange = { scope.launch { settingsRepository.setWidgetSaveDimensions(index, it) } },
                colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        DragAdjustField(
            label = "FONT SIZE SCALE",
            value = fontSizeScale,
            minValue = 0.5f,
            maxValue = 1.5f,
            pixelsPerUnit = 180f,
            accentColor = accentColor,
            valueFormatter = { String.format("%.2f", it) },
            onValueChange = { scope.launch { settingsRepository.setWidgetFontSizeScale(index, it) } }
        )
    }
}

@Composable
fun LapRowItem(lap: Lap, textColor: Color, grayColor: Color) {
    val totalSeconds = lap.cumulativeTimeMs / 1000
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val centiseconds = (lap.cumulativeTimeMs % 1000) / 10
    val formattedCum = String.format("%02d:%02d.%02d", minutes, seconds, centiseconds)

    val lapSecs = lap.lapTimeMs / 1000
    val lapMins = (lapSecs % 3600) / 60
    val lapS = lapSecs % 60
    val lapCent = (lap.lapTimeMs % 1000) / 10
    val formattedLap = String.format("%02d:%02d.%02d", lapMins, lapS, lapCent)

    val deltaSign = if (lap.diffFromPreviousMs >= 0) "+" else ""
    val deltaSecs = lap.diffFromPreviousMs / 1000
    val deltaCent = (Math.abs(lap.diffFromPreviousMs) % 1000) / 10
    val formattedDelta = "$deltaSign${deltaSecs}.${deltaCent}s"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "LAP ${lap.lapIndex}",
                style = TextStyle(
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = formattedCum,
                style = TextStyle(
                    color = grayColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light
                )
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formattedLap,
                style = TextStyle(
                    color = textColor,
                    fontSize = 14.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            )
            if (lap.lapIndex > 1) {
                Spacer(modifier = Modifier.width(12.dp))
                val colorDelta = if (lap.diffFromPreviousMs > 0) Color(0xFFC94A4A) else Color(0xFF4AC98F)
                Text(
                    text = formattedDelta,
                    style = TextStyle(
                        color = colorDelta,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Light
                    )
                )
            }
        }
    }
}
