package com.anilibrix.plus.ui.player

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anilibrix.plus.ui.components.AnilibrixLoadingIndicator
import com.anilibrix.plus.ui.theme.AnilibrixPlayerColors
import com.anilibrix.plus.ui.theme.AnilibrixShapeExtras
import com.anilibrix.plus.ui.theme.Elevation
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing
import kotlinx.coroutines.delay

private sealed class PlayerHud {
    data class Brightness(val value: Float) : PlayerHud()
    data class Volume(val value: Float) : PlayerHud()
    data class AspectRatio(val mode: AspectRatioMode) : PlayerHud()
}

@Composable
fun PlayerController(
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onQualityChange: (String) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onToggleSubtitles: () -> Unit,
    onToggleMute: () -> Unit,
    onSkipOpening: () -> Unit,
    onSkipEnding: () -> Unit,
    onDismissAutoAdvance: () -> Unit,
    onSkipAutoAdvance: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleControls: () -> Unit,
    onOpenTracks: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onBrightnessChange: (Float) -> Unit = {},
    onVolumeChange: (Float) -> Unit = {},
    onToggleTouchLock: () -> Unit = {},
    onCycleAspectRatio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var seekFeedback by remember { mutableStateOf<SeekFeedback?>(null) }
    var activeHud by remember { mutableStateOf<PlayerHud?>(null) }
    var showUnlockButton by remember { mutableStateOf(false) }
    val view = LocalView.current

    // Автоскрытие HUD-индикатора громкости / яркости
    LaunchedEffect(activeHud) {
        if (activeHud != null) {
            delay(1500)
            activeHud = null
        }
    }

    // Автоскрытие кнопки разблокировки в режиме Lock
    LaunchedEffect(showUnlockButton) {
        if (showUnlockButton) {
            delay(3500)
            showUnlockButton = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.playbackError, state.isTouchLocked) {
                detectTapGestures(
                    onTap = {
                        if (state.isTouchLocked) {
                            showUnlockButton = !showUnlockButton
                        } else if (state.playbackError == null) {
                            onToggleControls()
                        }
                    },
                    onDoubleTap = { offset ->
                        if (!state.isTouchLocked && state.playbackError == null && state.duration > 0L) {
                            val forward = offset.x >= size.width / 2f
                            val deltaMs = if (forward) 10_000L else -10_000L
                            onSeekRelative(deltaMs)
                            val currentSeek = if (seekFeedback != null && seekFeedback?.forward == forward) {
                                (seekFeedback?.seconds ?: 10) + 10
                            } else {
                                10
                            }
                            seekFeedback = SeekFeedback(
                                forward = forward,
                                seconds = currentSeek,
                                nonce = System.nanoTime()
                            )
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                )
            }
            .pointerInput(state.duration, state.playbackError, state.isTouchLocked) {
                if (state.isTouchLocked) return@pointerInput
                var accumulatedSeekMs = 0L
                detectHorizontalDragGestures(
                    onDragStart = { accumulatedSeekMs = 0L },
                    onDragEnd = {
                        if (state.playbackError == null && accumulatedSeekMs != 0L) {
                            onSeekRelative(accumulatedSeekMs)
                        }
                    },
                    onDragCancel = { accumulatedSeekMs = 0L },
                    onHorizontalDrag = { change, dragAmount ->
                        if (state.playbackError == null && state.duration > 0L) {
                            accumulatedSeekMs += (dragAmount * 80L).toLong()
                        }
                        change.consume()
                    }
                )
            }
            .pointerInput(state.brightness, state.volume, state.isTouchLocked) {
                if (state.isTouchLocked) return@pointerInput
                var startX = 0f
                detectVerticalDragGestures(
                    onDragStart = { offset -> startX = offset.x },
                    onVerticalDrag = { change, dragAmount ->
                        val screenHeight = size.height.toFloat()
                        val delta = -dragAmount / screenHeight
                        if (startX < size.width / 2f) {
                            // Левая половина: яркость
                            val newBrightness = (state.brightness + delta).coerceIn(0.01f, 1f)
                            onBrightnessChange(newBrightness)
                            activeHud = PlayerHud.Brightness(newBrightness)
                        } else {
                            // Правая половина: громкость
                            val newVolume = (state.volume + delta).coerceIn(0f, 1f)
                            onVolumeChange(newVolume)
                            activeHud = PlayerHud.Volume(newVolume)
                        }
                        change.consume()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Обычные элементы управления
        AnimatedVisibility(
            visible = state.showControls && !state.isTouchLocked && state.playbackError == null,
            enter = fadeIn(MotionTokens.effectsDefault()),
            exit = fadeOut(MotionTokens.effectsFast())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AnilibrixPlayerColors.scrim)
            ) {
                CenterControls(
                    state = state,
                    onPlayPause = onPlayPause,
                    onSkipOpening = onSkipOpening,
                    onSkipEnding = onSkipEnding,
                    onAutoAdvanceDismiss = onDismissAutoAdvance,
                    onAutoAdvanceSkip = onSkipAutoAdvance
                )

                // Верхний правый угол контролов (Кнопка блокировки экрана)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    IconButton(onClick = onToggleTouchLock) {
                        Icon(
                            imageVector = Icons.Rounded.LockOpen,
                            contentDescription = "Блокировка касаний",
                            tint = AnilibrixPlayerColors.onScrim
                        )
                    }
                }

                // Нижний блок: ползунок времени и кнопки
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Slider(
                        value = if (state.duration > 0L) {
                            (state.currentPosition.toFloat() / state.duration.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        },
                        onValueChange = { fraction ->
                            if (state.duration > 0L) {
                                onSeek((fraction.coerceIn(0f, 1f) * state.duration).toLong())
                            }
                        },
                        enabled = state.duration > 0L,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(state.currentPosition),
                            style = MaterialTheme.typography.labelSmall,
                            color = AnilibrixPlayerColors.onScrim
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BottomControlButton(
                                icon = Icons.Default.Replay10,
                                onClick = { onSeekRelative(-10_000) }
                            )
                            BottomControlButton(
                                icon = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                onClick = onPlayPause
                            )
                            BottomControlButton(
                                icon = Icons.Default.Forward10,
                                onClick = { onSeekRelative(10_000) }
                            )
                        }

                        Text(
                            text = formatDuration(state.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = AnilibrixPlayerColors.onScrim
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onOpenTracks) {
                            Icon(
                                Icons.Rounded.Tune,
                                contentDescription = "Качество, скорость и субтитры",
                                tint = AnilibrixPlayerColors.onScrim
                            )
                        }

                        IconButton(onClick = onToggleSubtitles) {
                            Icon(
                                Icons.Default.ClosedCaption,
                                contentDescription = "Субтитры",
                                tint = if (state.subtitlesEnabled) MaterialTheme.colorScheme.primary else AnilibrixPlayerColors.onScrim
                            )
                        }

                        IconButton(onClick = {
                            onCycleAspectRatio()
                            activeHud = PlayerHud.AspectRatio(state.aspectRatioMode)
                        }) {
                            Icon(
                                Icons.Rounded.AspectRatio,
                                contentDescription = "Формат: ${state.aspectRatioMode.label}",
                                tint = AnilibrixPlayerColors.onScrim
                            )
                        }

                        IconButton(onClick = onToggleMute) {
                            Icon(
                                if (state.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Volume",
                                tint = AnilibrixPlayerColors.onScrim
                            )
                        }

                        IconButton(onClick = onToggleFullscreen) {
                            Icon(
                                if (state.isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = AnilibrixPlayerColors.onScrim
                            )
                        }
                    }
                }
            }
        }

        // Кнопка разблокировки экрана при активном Touch Lock
        AnimatedVisibility(
            visible = state.isTouchLocked && showUnlockButton,
            enter = fadeIn(MotionTokens.effectsFast()) + scaleIn(MotionTokens.spatialFast(), initialScale = 0.8f),
            exit = fadeOut(MotionTokens.effectsFast()) + scaleOut(MotionTokens.spatialFast(), targetScale = 0.8f),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp)
        ) {
            Button(
                onClick = onToggleTouchLock,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = AnilibrixShapeExtras.pill,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = Elevation.level3)
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Экран заблокирован · Нажмите для снятия", style = MaterialTheme.typography.labelMedium)
            }
        }

        // HUD Индикаторы яркости, громкости и формата экрана
        AnimatedVisibility(
            visible = activeHud != null,
            enter = fadeIn(MotionTokens.effectsFast()) + scaleIn(MotionTokens.spatialFast(), initialScale = 0.9f),
            exit = fadeOut(MotionTokens.effectsDefault()),
            modifier = Modifier.align(
                when (activeHud) {
                    is PlayerHud.Brightness -> Alignment.CenterStart
                    is PlayerHud.Volume -> Alignment.CenterEnd
                    is PlayerHud.AspectRatio, null -> Alignment.TopCenter
                }
            )
        ) {
            when (val hud = activeHud) {
                is PlayerHud.Brightness -> VerticalHudPill(
                    icon = when {
                        hud.value > 0.66f -> Icons.Rounded.BrightnessHigh
                        hud.value > 0.33f -> Icons.Rounded.BrightnessMedium
                        else -> Icons.Rounded.BrightnessLow
                    },
                    percentage = hud.value,
                    label = "Яркость",
                    modifier = Modifier.padding(start = 24.dp)
                )
                is PlayerHud.Volume -> VerticalHudPill(
                    icon = if (hud.value == 0f) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                    percentage = hud.value,
                    label = "Громкость",
                    modifier = Modifier.padding(end = 24.dp)
                )
                is PlayerHud.AspectRatio -> {
                    Box(
                        modifier = Modifier
                            .padding(top = 50.dp)
                            .background(AnilibrixPlayerColors.scrimStrong, AnilibrixShapeExtras.pill)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Формат: ${hud.mode.label}",
                            color = AnilibrixPlayerColors.onScrim,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                null -> Unit
            }
        }

        // Анимированный оверлей двойного тапа перемотки
        seekFeedback?.let { feedback ->
            SeekRippleFeedbackOverlay(
                forward = feedback.forward,
                seconds = feedback.seconds,
                nonce = feedback.nonce,
                onFinished = { seekFeedback = null },
                modifier = Modifier.align(if (feedback.forward) Alignment.CenterEnd else Alignment.CenterStart)
            )
        }

        if (state.isBuffering && state.playbackError == null) {
            BufferingIndicator()
        }

        state.playbackError?.let { message ->
            PlaybackErrorOverlay(
                message = message,
                onRetry = onRetry,
                onBack = onBack
            )
        }

        if (state.subtitlesEnabled && state.subtitleText.isNotBlank() && state.playbackError == null) {
            Text(
                text = state.subtitleText,
                color = state.subtitleColor(),
                fontSize = state.subtitleSizeSp.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .background(AnilibrixPlayerColors.scrim, RoundedCornerShape(4.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun VerticalHudPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    percentage: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(44.dp)
            .height(160.dp)
            .background(AnilibrixPlayerColors.scrimStrong, AnilibrixShapeExtras.pill)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AnilibrixPlayerColors.onScrim,
            modifier = Modifier.size(20.dp)
        )
        // Тонкий вертикальный индикатор
        Box(
            modifier = Modifier
                .width(6.dp)
                .weight(1f)
                .padding(vertical = 8.dp)
                .clip(CircleShape)
                .background(AnilibrixPlayerColors.onScrim.copy(alpha = 0.25f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(percentage.coerceIn(0f, 1f))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Text(
            text = "${(percentage * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = AnilibrixPlayerColors.onScrim,
            fontSize = 10.sp
        )
    }
}

private fun PlayerUiState.subtitleColor(): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(subtitleColorHex))
    }.getOrDefault(AnilibrixPlayerColors.onScrim)
}

private data class SeekFeedback(
    val forward: Boolean,
    val seconds: Int = 10,
    val nonce: Long = 0L
)

@Composable
private fun SeekRippleFeedbackOverlay(
    forward: Boolean,
    seconds: Int,
    nonce: Long,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember(nonce) { Animatable(0f) }
    LaunchedEffect(nonce) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 650, easing = LinearEasing))
        onFinished()
    }

    Box(
        modifier = modifier
            .padding(horizontal = 36.dp)
            .size(110.dp)
            .graphicsLayer {
                val v = progress.value
                alpha = if (v < 0.15f) v / 0.15f else (1f - (v - 0.15f) / 0.85f)
                val scale = 0.85f + (v * 0.25f)
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(AnilibrixPlayerColors.scrimStrong)
            .drawBehind {
                val waveProgress = progress.value
                val strokeWidth = 3.dp.toPx()
                val radius = size.minDimension / 2f * waveProgress
                drawCircle(
                    color = Color.White.copy(alpha = (1f - waveProgress) * 0.35f),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (forward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${if (forward) "+" else "-"}$seconds с",
                style = MaterialTheme.typography.labelLarge,
                color = AnilibrixPlayerColors.onScrim,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CenterControls(
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onSkipOpening: () -> Unit,
    onSkipEnding: () -> Unit,
    onAutoAdvanceDismiss: () -> Unit,
    onAutoAdvanceSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(72.dp)
                .background(AnilibrixPlayerColors.scrimStrong, CircleShape)
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                tint = AnilibrixPlayerColors.onScrim,
                modifier = Modifier.size(40.dp)
            )
        }

        AnimatedVisibility(
            visible = state.skipOpening.active,
            enter = fadeIn(MotionTokens.effectsDefault()) +
                slideInHorizontally(MotionTokens.spatialDefault()) { it / 2 },
            exit = fadeOut(MotionTokens.effectsFast()) +
                slideOutHorizontally(MotionTokens.spatialFast()) { it / 2 },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            SkipButton(
                label = "Пропустить опенинг",
                state = state.skipOpening,
                onClick = onSkipOpening,
            )
        }

        AnimatedVisibility(
            visible = state.skipEnding.active,
            enter = fadeIn(MotionTokens.effectsDefault()) +
                slideInHorizontally(MotionTokens.spatialDefault()) { it / 2 },
            exit = fadeOut(MotionTokens.effectsFast()) +
                slideOutHorizontally(MotionTokens.spatialFast()) { it / 2 },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            SkipButton(
                label = "Пропустить эндинг",
                state = state.skipEnding,
                onClick = onSkipEnding,
            )
        }

        if (state.autoAdvance.active) {
            AutoAdvanceOverlay(
                remaining = state.autoAdvance.remainingSeconds,
                onDismiss = onAutoAdvanceDismiss,
                onSkip = onAutoAdvanceSkip,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun SkipButton(
    label: String,
    state: SkipState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(durationMillis = 120, easing = LinearEasing),
        label = "skipProgress",
    )
    val ringColor = MaterialTheme.colorScheme.onPrimary
    val trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)

    Button(
        onClick = onClick,
        modifier = modifier
            .padding(Spacing.lg)
            .height(Sizing.touchTarget),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = AnilibrixShapeExtras.pill,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = Elevation.level2,
            pressedElevation = Elevation.level4,
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            if (state.autoSkip) {
                Box(
                    modifier = Modifier
                        .size(Sizing.iconLg)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .drawBehind {
                            val stroke = 2.5.dp.toPx()
                            val inset = stroke / 2f
                            drawArc(
                                color = trackColor,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = -360f * (1f - animatedProgress).coerceIn(0f, 1f),
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${state.remainingSeconds}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(Sizing.iconSm)
            )
        }
    }
}

@Composable
private fun AutoAdvanceOverlay(
    remaining: Int,
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(AnilibrixPlayerColors.scrimStrong, RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Следующая серия через $remaining",
            color = AnilibrixPlayerColors.onScrim,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BottomControlButton(
                icon = Icons.Default.KeyboardArrowDown,
                onClick = onDismiss
            )
            BottomControlButton(
                icon = Icons.Default.Forward10,
                onClick = onSkip
            )
        }
    }
}

@Composable
private fun BufferingIndicator() {
    Column(
        modifier = Modifier
            .background(AnilibrixPlayerColors.scrimStrong, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnilibrixLoadingIndicator(size = 32.dp, color = AnilibrixPlayerColors.onScrim)
        Text(
            text = "Буферизация…",
            color = AnilibrixPlayerColors.onScrim,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun PlaybackErrorOverlay(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(AnilibrixPlayerColors.scrimStrong, RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ошибка воспроизведения",
            color = AnilibrixPlayerColors.onScrim,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = message,
            color = AnilibrixPlayerColors.onScrim.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRetry) {
                Text("Повторить")
            }
            TextButton(onClick = onBack) {
                Text("Назад", color = AnilibrixPlayerColors.onScrim)
            }
        }
    }
}

@Composable
private fun BottomControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AnilibrixPlayerColors.onScrim,
            modifier = Modifier.size(28.dp)
        )
    }
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
