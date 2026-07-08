package com.anilibrix.plus.ui.player

import android.view.KeyEvent
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface PlayerEntryPoint {
    fun cacheDataSourceFactory(): CacheDataSource.Factory
}

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
    val focusRequester = remember { FocusRequester() }
    val cacheDataSourceFactory = remember {
        val app = context.applicationContext as android.app.Application
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            app,
            PlayerEntryPoint::class.java
        )
        hiltEntryPoint.cacheDataSourceFactory()
    }
    val player = remember {
        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory as androidx.media3.datasource.DataSource.Factory)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    LaunchedEffect(titleId, episodeId) {
        viewModel.loadEpisode(titleId, episodeId)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(state.quality, state.currentEpisode, state.retryNonce) {
        val episode = state.currentEpisode ?: return@LaunchedEffect
        val url = when (state.quality) {
            "480" -> episode.hls480
            "720" -> episode.hls720
            "1080" -> episode.hls1080
            else -> episode.hls720 ?: episode.hls480 ?: episode.hls1080
        } ?: episode.hls720 ?: episode.hls480 ?: episode.hls1080

        if (url.isNullOrBlank()) {
            viewModel.handleIntent(PlayerIntent.ShowPlaybackError("Для этой серии нет доступного видео"))
            return@LaunchedEffect
        }

        viewModel.handleIntent(PlayerIntent.ClearPlaybackError)
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = state.isPlaying
    }

    LaunchedEffect(state.isPlaying) {
        player.playWhenReady = state.isPlaying && state.playbackError == null
    }

    LaunchedEffect(state.seekPosition) {
        val pos = state.seekPosition
        if (pos >= 0L && player.currentPosition != pos) {
            player.seekTo(pos)
            viewModel.handleIntent(PlayerIntent.SeekComplete)
        }
    }

    LaunchedEffect(state.speed) {
        player.setPlaybackSpeed(state.speed)
    }

    LaunchedEffect(state.volume, state.isMuted) {
        player.volume = if (state.isMuted) 0f else state.volume.coerceIn(0f, 1f)
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        viewModel.handleIntent(PlayerIntent.SetBuffering(true))
                    }

                    Player.STATE_READY -> {
                        viewModel.handleIntent(PlayerIntent.SetBuffering(false))
                        viewModel.handleIntent(PlayerIntent.ClearPlaybackError)
                        viewModel.handleIntent(PlayerIntent.UpdateDuration(sanitizeDuration(player.duration)))
                        viewModel.handleIntent(
                            PlayerIntent.UpdatePosition(
                                sanitizePosition(player.currentPosition, player.duration)
                            )
                        )
                    }

                    Player.STATE_ENDED -> {
                        viewModel.handleIntent(PlayerIntent.SetBuffering(false))
                        viewModel.handleIntent(PlayerIntent.OnVideoEnded)
                    }

                    else -> Unit
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val details = error.errorCodeName.ifBlank { error.localizedMessage ?: "Playback error" }
                viewModel.handleIntent(PlayerIntent.ShowPlaybackError("Не удалось воспроизвести видео: $details"))
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    var lastPosition by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val duration = sanitizeDuration(player.duration)
            val currentPosition = sanitizePosition(player.currentPosition, duration)
            if (currentPosition != lastPosition) {
                lastPosition = currentPosition
                viewModel.handleIntent(PlayerIntent.UpdatePosition(currentPosition))
                viewModel.handleIntent(PlayerIntent.UpdateBuffered(player.bufferedPercentage))
                viewModel.handleIntent(PlayerIntent.UpdateDuration(duration))
                viewModel.checkSkipRanges()
            }
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
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_SPACE -> {
                        viewModel.handleIntent(PlayerIntent.PlayPause)
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        viewModel.handleIntent(PlayerIntent.SeekRelative(-10_000))
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        viewModel.handleIntent(PlayerIntent.SeekRelative(10_000))
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_UP -> {
                        viewModel.handleIntent(PlayerIntent.SetVolume((state.volume + 0.1f).coerceAtMost(1f)))
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        viewModel.handleIntent(PlayerIntent.SetVolume((state.volume - 0.1f).coerceAtLeast(0f)))
                        true
                    }

                    KeyEvent.KEYCODE_F -> {
                        viewModel.handleIntent(PlayerIntent.ToggleFullscreen)
                        true
                    }

                    KeyEvent.KEYCODE_M -> {
                        viewModel.handleIntent(PlayerIntent.ToggleMute)
                        true
                    }

                    KeyEvent.KEYCODE_C -> {
                        viewModel.handleIntent(PlayerIntent.ToggleSubtitles)
                        true
                    }

                    KeyEvent.KEYCODE_PERIOD, KeyEvent.KEYCODE_NUMPAD_ADD -> {
                        viewModel.handleIntent(PlayerIntent.SetSpeed(nextSpeed(state.speed, 1)))
                        true
                    }

                    KeyEvent.KEYCODE_COMMA, KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> {
                        viewModel.handleIntent(PlayerIntent.SetSpeed(nextSpeed(state.speed, -1)))
                        true
                    }

                    KeyEvent.KEYCODE_ESCAPE -> {
                        if (state.isFullscreen) {
                            viewModel.handleIntent(PlayerIntent.ToggleFullscreen)
                        } else {
                            onBack()
                        }
                        true
                    }

                    in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                        if (state.duration > 0L) {
                            val digit = event.nativeKeyEvent.keyCode - KeyEvent.KEYCODE_0
                            viewModel.handleIntent(PlayerIntent.SeekTo(state.duration * digit / 10L))
                            true
                        } else {
                            false
                        }
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
            onRetry = { viewModel.handleIntent(PlayerIntent.RetryPlayback) },
            onBack = onBack,
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
        if (state.isFullscreen) {
            viewModel.handleIntent(PlayerIntent.ToggleFullscreen)
        } else {
            onBack()
        }
    }
}

private val supportedSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

private fun nextSpeed(current: Float, direction: Int): Float {
    val currentIndex = supportedSpeeds.indexOfFirst { kotlin.math.abs(it - current) < 0.01f }
        .takeIf { it >= 0 }
        ?: supportedSpeeds.indexOfFirst { it >= current }.takeIf { it >= 0 }
        ?: supportedSpeeds.lastIndex
    return supportedSpeeds[(currentIndex + direction).coerceIn(0, supportedSpeeds.lastIndex)]
}

private fun sanitizeDuration(durationMs: Long): Long {
    return if (durationMs == C.TIME_UNSET || durationMs < 0L) 0L else durationMs
}

private fun sanitizePosition(positionMs: Long, durationMs: Long): Long {
    val safeDuration = sanitizeDuration(durationMs)
    val safePosition = positionMs.coerceAtLeast(0L)
    return if (safeDuration > 0L) safePosition.coerceAtMost(safeDuration) else safePosition
}
