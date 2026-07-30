package com.anilibrix.plus.ui.studio

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.anilibrix.plus.ui.player.formatDuration
import com.anilibrix.plus.ui.theme.AnilibrixPlayerColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioPlayerScreen(
    source: String,
    episodeId: String,
    onBack: () -> Unit,
    viewModel: StudioPlayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showQualityMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadVideo(source, episodeId)
    }

    LaunchedEffect(state.videoUrl, state.retryNonce) {
        if (state.videoUrl.isNotBlank()) {
            val mediaItem = MediaItem.Builder()
                .setUri(state.videoUrl)
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
        }
    }

    LaunchedEffect(state.isPlaying) {
        player.playWhenReady = state.isPlaying
    }

    LaunchedEffect(state.currentPosition) {
        if (state.currentPosition > 0 && player.currentPosition != state.currentPosition) {
            player.seekTo(state.currentPosition)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            viewModel.handleIntent(StudioPlayerIntent.UpdatePosition(player.currentPosition))
            viewModel.handleIntent(StudioPlayerIntent.UpdateDuration(player.duration))
        }
    }

    LaunchedEffect(state.speed) {
        player.setPlaybackSpeed(state.speed)
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val details = error.errorCodeName.ifBlank { error.localizedMessage ?: "Playback error" }
                viewModel.handleIntent(StudioPlayerIntent.ShowPlaybackError("Не удалось воспроизвести видео: $details"))
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = AnilibrixPlayerColors.onScrim
            )
        }

        state.error?.let { message ->
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(AnilibrixPlayerColors.scrimStrong, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ошибка воспроизведения",
                    color = AnilibrixPlayerColors.onScrim,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = message,
                    color = AnilibrixPlayerColors.onScrim.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { viewModel.handleIntent(StudioPlayerIntent.Retry) }) {
                        Text("Повторить")
                    }
                    TextButton(onClick = onBack) {
                        Text("Назад", color = AnilibrixPlayerColors.onScrim)
                    }
                }
            }
        }

        if (state.showControls && state.error == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AnilibrixPlayerColors.scrim),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { viewModel.handleIntent(StudioPlayerIntent.PlayPause) },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AnilibrixPlayerColors.scrim)
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = AnilibrixPlayerColors.onScrim,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Slider(
                        value = if (state.duration > 0) state.currentPosition.toFloat() / state.duration.toFloat() else 0f,
                        onValueChange = { fraction ->
                            viewModel.handleIntent(StudioPlayerIntent.SeekTo((fraction * state.duration).toLong()))
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(state.currentPosition),
                            color = AnilibrixPlayerColors.onScrim,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Row {
                            IconButton(onClick = { viewModel.handleIntent(StudioPlayerIntent.SeekTo(state.currentPosition - 10000)) }) {
                                Icon(Icons.Default.Replay10, null, tint = AnilibrixPlayerColors.onScrim)
                            }
                            IconButton(onClick = { viewModel.handleIntent(StudioPlayerIntent.PlayPause) }) {
                                Icon(
                                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    null,
                                    tint = AnilibrixPlayerColors.onScrim
                                )
                            }
                            IconButton(onClick = { viewModel.handleIntent(StudioPlayerIntent.SeekTo(state.currentPosition + 10000)) }) {
                                Icon(Icons.Default.Forward10, null, tint = AnilibrixPlayerColors.onScrim)
                            }
                        }
                        Text(
                            text = formatDuration(state.duration),
                            color = AnilibrixPlayerColors.onScrim,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            TextButton(
                                onClick = { showQualityMenu = true },
                                enabled = state.videos.isNotEmpty()
                            ) {
                                Text(
                                    text = state.selectedQuality?.let { qualityLabel(it) } ?: "Авто",
                                    color = AnilibrixPlayerColors.onScrim
                                )
                            }
                            DropdownMenu(
                                expanded = showQualityMenu,
                                onDismissRequest = { showQualityMenu = false }
                            ) {
                                state.videos.forEach { video ->
                                    val quality = video.quality
                                    DropdownMenuItem(
                                        text = { Text(quality?.let { qualityLabel(it) } ?: "Авто") },
                                        onClick = {
                                            viewModel.handleIntent(StudioPlayerIntent.SetQuality(quality))
                                            showQualityMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        Box {
                            TextButton(onClick = { showSpeedMenu = true }) {
                                Text("${state.speed}x", color = AnilibrixPlayerColors.onScrim)
                            }
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                                    DropdownMenuItem(
                                        text = { Text("${speed}x") },
                                        onClick = {
                                            viewModel.handleIntent(StudioPlayerIntent.SetSpeed(speed))
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = AnilibrixPlayerColors.onScrim)
                }
            },
            modifier = Modifier.align(Alignment.TopCenter),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }
}

private fun qualityLabel(quality: String): String {
    return if (quality.endsWith("p", ignoreCase = true)) quality else "${quality}p"
}
