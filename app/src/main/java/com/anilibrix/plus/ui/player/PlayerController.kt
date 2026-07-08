package com.anilibrix.plus.ui.player

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
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showQualityMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
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
            enter = fadeIn() + scaleIn(initialScale = 0.98f),
            exit = fadeOut() + scaleOut(targetScale = 0.98f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
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
                            color = Color.White
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
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DropdownSelector(
                            label = if (state.quality == "Auto") "Auto" else "${state.quality}p",
                            expanded = showQualityMenu,
                            onToggle = { showQualityMenu = !showQualityMenu },
                            onDismiss = { showQualityMenu = false },
                            options = listOf("Auto", "480p", "720p", "1080p"),
                            onSelect = { q ->
                                onQualityChange(q.removeSuffix("p"))
                                showQualityMenu = false
                            }
                        )

                        DropdownSelector(
                            label = "${state.speed}x",
                            expanded = showSpeedMenu,
                            onToggle = { showSpeedMenu = !showSpeedMenu },
                            onDismiss = { showSpeedMenu = false },
                            options = listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"),
                            onSelect = { s ->
                                onSpeedChange(s.removeSuffix("x").toFloatOrNull() ?: 1.0f)
                                showSpeedMenu = false
                            }
                        )

                        IconButton(onClick = onToggleSubtitles) {
                            Icon(
                                Icons.Default.ClosedCaption,
                                contentDescription = "Subtitles",
                                tint = if (state.subtitlesEnabled) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        IconButton(onClick = onToggleMute) {
                            Icon(
                                if (state.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Volume",
                                tint = Color.White
                            )
                        }

                        IconButton(onClick = onToggleFullscreen) {
                            Icon(
                                if (state.isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        seekFeedback?.let { feedback ->
            SeekFeedbackOverlay(
                forward = feedback.forward,
                modifier = Modifier.align(if (feedback.forward) Alignment.CenterEnd else Alignment.CenterStart)
            )
            LaunchedEffect(feedback.nonce) {
                delay(650)
                seekFeedback = null
            }
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
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .background(Color(0x80000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

private data class SeekFeedback(
    val forward: Boolean,
    val nonce: Long
)

@Composable
private fun SeekFeedbackOverlay(
    forward: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 220),
        label = "seekFeedbackScale"
    )

    Box(
        modifier = modifier
            .padding(horizontal = 36.dp)
            .scale(scale)
            .size(104.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .size(104.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (forward) Icons.Default.Forward10 else Icons.Default.Replay10,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            )
            Text(
                text = if (forward) "+10 сек" else "-10 сек",
                color = Color.White,
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
                .background(Color(0x80000000))
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        if (state.skipOpening.active) {
            SkipButton(
                label = "Пропустить OP",
                remaining = state.skipOpening.remainingSeconds,
                onClick = onSkipOpening,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        if (state.skipEnding.active) {
            SkipButton(
                label = "Пропустить ED",
                remaining = state.skipEnding.remainingSeconds,
                onClick = onSkipEnding,
                modifier = Modifier.align(Alignment.CenterEnd)
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
    remaining: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .padding(16.dp)
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$remaining",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
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
            .background(Color(0xCC000000), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Следующая серия через $remaining",
            color = Color.White,
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
            .background(Color(0x99000000), RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
        Text(
            text = "Буферизация…",
            color = Color.White,
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
            .background(Color(0xDD000000), RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ошибка воспроизведения",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRetry) {
                Text("Повторить")
            }
            TextButton(onClick = onBack) {
                Text("Назад", color = Color.White)
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
            tint = Color.White,
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
                color = Color.White,
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
