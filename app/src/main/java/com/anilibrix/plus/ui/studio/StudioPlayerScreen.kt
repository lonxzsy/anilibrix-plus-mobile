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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.anilibrix.plus.ui.player.formatDuration

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
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadVideo(source, episodeId)
    }

    LaunchedEffect(state.videoUrl) {
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

        if (state.showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { viewModel.handleIntent(StudioPlayerIntent.PlayPause) },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0x80000000))
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
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
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Row {
                            IconButton(onClick = { viewModel.handleIntent(StudioPlayerIntent.SeekTo(state.currentPosition - 10000)) }) {
                                Icon(Icons.Default.Replay10, null, tint = Color.White)
                            }
                            IconButton(onClick = { viewModel.handleIntent(StudioPlayerIntent.PlayPause) }) {
                                Icon(
                                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    null,
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { viewModel.handleIntent(StudioPlayerIntent.SeekTo(state.currentPosition + 10000)) }) {
                                Icon(Icons.Default.Forward10, null, tint = Color.White)
                            }
                        }
                        Text(
                            text = formatDuration(state.duration),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            modifier = Modifier.align(Alignment.TopCenter),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }
}
