package com.anilibrix.plus.ui.player

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.anilibrix.plus.ui.theme.AnilibrixShapeExtras
import com.anilibrix.plus.ui.theme.Elevation
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.graphicsLayer
import com.anilibrix.plus.ui.theme.AnilibrixPlayerColors
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.components.AnilibrixLoadingIndicator

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
    modifier: Modifier = Modifier
) {
    var seekFeedback by remember { mutableStateOf<SeekFeedback?>(null) }
    val view = LocalView.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.playbackError) {
                detectTapGestures(
                    onTap = {
                        if (state.playbackError == null) onToggleControls()
                    },
                    onDoubleTap = { offset ->
                        if (state.playbackError == null && state.duration > 0L) {
                            val forward = offset.x >= size.width / 2f
                            val deltaMs = if (forward) 10_000L else -10_000L
                            onSeekRelative(deltaMs)
                            seekFeedback = SeekFeedback(forward = forward, nonce = System.nanoTime())
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                )
            }
            .pointerInput(state.duration, state.playbackError) {
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
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = state.showControls && state.playbackError == null,
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
                        // Качество, скорость и субтитры — одна шторка снизу.
                        // Раньше это были два выпадающих меню в верхнем углу:
                        // дотянуться до них одной рукой на телефоне нельзя,
                        // а список открывался поверх самого видео.
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

        seekFeedback?.let { feedback ->
            SeekFeedbackOverlay(
                forward = feedback.forward,
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

private fun PlayerUiState.subtitleColor(): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(subtitleColorHex))
    }.getOrDefault(AnilibrixPlayerColors.onScrim)
}

private data class SeekFeedback(
    val forward: Boolean,
    val nonce: Long
)

@Composable
private fun SeekFeedbackOverlay(
    forward: Boolean,
    nonce: Long,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    /*
     * Раньше здесь стоял `animateFloatAsState(targetValue = 1f)` — цель
     * КОНСТАНТНАЯ, поэтому анимация не запускалась ни разу: кружок появлялся
     * и исчезал рывком, а убирал его внешний `delay(650)`.
     *
     * Теперь прогресс гонит Animatable, ключуемый по nonce (каждый тап —
     * новый запуск), и он же сообщает владельцу, когда пора убрать оверлей.
     */
    val progress = remember(nonce) { Animatable(0f) }
    LaunchedEffect(nonce) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 650, easing = LinearEasing))
        onFinished()
    }

    Box(
        modifier = modifier
            .padding(horizontal = 36.dp)
            .size(104.dp)
            .graphicsLayer {
                val v = progress.value
                // Быстро проявляется, затем плавно гаснет, слегка вырастая.
                alpha = if (v < 0.12f) v / 0.12f else (1f - (v - 0.12f) / 0.88f)
                val s = 0.82f + 0.30f * v
                scaleX = s
                scaleY = s
            }
            .clip(CircleShape)
            .background(AnilibrixPlayerColors.onScrim.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (forward) Icons.Default.Forward10 else Icons.Default.Replay10,
                contentDescription = null,
                tint = AnilibrixPlayerColors.onScrim,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            )
            Text(
                text = if (forward) "+10 сек" else "-10 сек",
                color = AnilibrixPlayerColors.onScrim,
                style = MaterialTheme.typography.labelMedium,
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
    onAutoAdvanceSkip: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AnilibrixPlayerColors.scrim)
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                tint = AnilibrixPlayerColors.onScrim,
                modifier = Modifier.size(40.dp)
            )
        }

        // Появление и уход — по общему motion-языку; раньше кнопка
        // возникала и исчезала мгновенно, посреди кадра.
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

/**
 * Кнопка пропуска заставки.
 *
 * В режиме автопропуска вокруг счётчика едет кольцо обратного отсчёта — видно,
 * сколько осталось до того, как приложение сделает это само, и успеваешь
 * передумать. Поле `SkipState.progress` для этого существовало с самого начала,
 * но не читалось нигде.
 *
 * В режиме «Спрашивать» кольца и счётчика нет: ничего само не произойдёт,
 * показывать отсчёт было бы враньём.
 */
@Composable
private fun SkipButton(
    label: String,
    state: SkipState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Кольцо анимируется отдельно от состояния: ViewModel тикает раз в 100 мс,
    // а между тиками дугу доводит анимация — иначе видна ступенчатость.
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
                                // Дуга УБЫВАЕТ: полное кольцо в начале,
                                // пустое — в момент срабатывания.
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

@Composable
private fun DropdownSelector(
    label: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Box {
        TextButton(onClick = onToggle) {
            Text(
                text = label,
                color = AnilibrixPlayerColors.onScrim,
                style = MaterialTheme.typography.labelMedium
            )
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = { onSelect(option) }
                )
            }
        }
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
