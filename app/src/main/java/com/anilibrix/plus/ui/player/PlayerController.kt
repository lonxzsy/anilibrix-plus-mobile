package com.anilibrix.plus.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anilibrix.plus.core.util.Constants

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
    modifier: Modifier = Modifier
) {
    var showQualityMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { onToggleControls() }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {},
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = state.showControls,
            enter = fadeIn(),
            exit = fadeOut()
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
                        value = if (state.duration > 0) state.currentPosition.toFloat() / state.duration.toFloat() else 0f,
                        onValueChange = { fraction ->
                            onSeek((fraction * state.duration).toLong())
                        },
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
                                onClick = { onSeekRelative(-10000) }
                            )
                            BottomControlButton(
                                icon = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                onClick = onPlayPause
                            )
                            BottomControlButton(
                                icon = Icons.Default.Forward10,
                                onClick = { onSeekRelative(10000) }
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
                            label = state.quality,
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

        if (state.subtitlesEnabled && state.subtitleText.isNotBlank()) {
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
                progress = state.skipOpening.progress,
                onClick = onSkipOpening,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        if (state.skipEnding.active) {
            SkipButton(
                label = "Пропустить ED",
                remaining = state.skipEnding.remainingSeconds,
                progress = state.skipEnding.progress,
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
    progress: Float,
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
        modifier = modifier.padding(16.dp),
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
fun BottomControlButton(
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
fun DropdownSelector(
    label: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Box {
        androidx.compose.material3.TextButton(onClick = onToggle) {
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
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
