package com.anilibrix.plus.ui.player

import android.view.KeyEvent
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.anilibrix.plus.domain.model.Episode
import kotlinx.coroutines.delay

@androidx.compose.ui.ExperimentalComposeUiApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    titleId: String,
    episodeId: Long,
    onBack: () -> Unit,
    onNextEpisode: (Long) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    LaunchedEffect(titleId, episodeId) {
        viewModel.loadEpisode(titleId, episodeId)
    }

    LaunchedEffect(state.quality, state.currentEpisode) {
        val episode = state.currentEpisode ?: return@LaunchedEffect
        val url = when (state.quality) {
            "480" -> episode.hls480
            "720" -> episode.hls720
            "1080" -> episode.hls1080
            else -> episode.hls720 ?: episode.hls480 ?: episode.hls1080
        }
        if (!url.isNullOrBlank()) {
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
            var attempts = 0
            while (player.playbackState != Player.STATE_READY && attempts < 100) {
                delay(100)
                attempts++
            }
            // Использовать seekPosition из state вместо currentPosition плеера
            // Это позволяет корректно обрабатывать автопереход на новую серию (seekPosition = 0)
            if (state.seekPosition > 0L) {
                player.seekTo(state.seekPosition)
            } else if (state.seekPosition == 0L) {
                player.seekTo(0L)
            }
        }
    }

    LaunchedEffect(state.isPlaying) {
        player.playWhenReady = state.isPlaying
    }

    LaunchedEffect(state.seekPosition) {
        val pos = state.seekPosition
        if (pos >= 0L && player.currentPosition != pos) {
            player.seekTo(pos)
        }
    }

    LaunchedEffect(state.speed) {
        player.setPlaybackSpeed(state.speed)
    }

    LaunchedEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        viewModel.handleIntent(PlayerIntent.UpdateDuration(player.duration))
                        viewModel.handleIntent(PlayerIntent.UpdatePosition(player.currentPosition))
                    }
                    Player.STATE_ENDED -> {
                        viewModel.handleIntent(PlayerIntent.OnVideoEnded)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
            }
        }
        player.addListener(listener)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            viewModel.handleIntent(PlayerIntent.UpdatePosition(player.currentPosition))
            viewModel.handleIntent(PlayerIntent.UpdateBuffered(player.bufferedPercentage))
            viewModel.handleIntent(PlayerIntent.UpdateDuration(player.duration))
            viewModel.checkSkipRanges()
        }
    }

    LaunchedEffect(state.isFullscreen) {
        val window = (context as? android.app.Activity)?.window ?: return@LaunchedEffect
        val controller = window.insetsController ?: return@LaunchedEffect
        if (state.isFullscreen) {
            controller.hide(WindowInsets.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsets.Type.systemBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
            (context as? android.app.Activity)?.window?.insetsController?.show(WindowInsets.Type.systemBars())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_SPACE -> {
                        if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                            viewModel.handleIntent(PlayerIntent.PlayPause)
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                            viewModel.handleIntent(PlayerIntent.SeekRelative(-10000))
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                            viewModel.handleIntent(PlayerIntent.SeekRelative(10000))
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_F -> {
                        if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_M -> {
                        if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                            viewModel.handleIntent(PlayerIntent.ToggleMute)
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_ESCAPE -> {
                        if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                            onBack()
                            true
                        } else false
                    }
                    else -> false
                }
            }
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

        PlayerController(
            state = state,
            onPlayPause = { viewModel.handleIntent(PlayerIntent.PlayPause) },
            onSeek = { viewModel.handleIntent(PlayerIntent.SeekTo(it)) },
            onSeekRelative = { viewModel.handleIntent(PlayerIntent.SeekRelative(it)) },
            onQualityChange = { viewModel.handleIntent(PlayerIntent.SetQuality(it)) },
            onSpeedChange = { viewModel.handleIntent(PlayerIntent.SetSpeed(it)) },
            onToggleSubtitles = { viewModel.handleIntent(PlayerIntent.ToggleSubtitles) },
            onToggleMute = { viewModel.handleIntent(PlayerIntent.ToggleMute) },
            onSkipOpening = { viewModel.handleIntent(PlayerIntent.SkipOpening) },
            onSkipEnding = { viewModel.handleIntent(PlayerIntent.SkipEnding) },
            onDismissAutoAdvance = { viewModel.handleIntent(PlayerIntent.DismissAutoAdvance) },
            onSkipAutoAdvance = { viewModel.handleIntent(PlayerIntent.SkipAutoAdvance) },
            onToggleFullscreen = { viewModel.handleIntent(PlayerIntent.ToggleFullscreen) },
            onToggleControls = { viewModel.handleIntent(PlayerIntent.ToggleControls) },
            modifier = Modifier.fillMaxSize()
        )

        if (!state.isFullscreen) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.titleName,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1
                        )
                        state.currentEpisode?.let { ep ->
                            Text(
                                text = "Эпизод ${ep.ordinal}: ${ep.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.handleIntent(PlayerIntent.StartPiP) }) {
                        Icon(
                            Icons.Default.PictureInPictureAlt,
                            contentDescription = "PiP",
                            tint = Color.White
                        )
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        }
    }

    BackHandler {
        onBack()
    }
}
