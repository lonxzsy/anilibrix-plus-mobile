package com.anilibrix.plus.ui.player

import android.view.KeyEvent
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Videocam
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.anilibrix.plus.core.util.SubtitleParser
import androidx.media3.ui.AspectRatioFrameLayout
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
    val focusRequester = remember { FocusRequester() }
    val activity = rememberActivity()

    // Здесь наконец задействован SubtitleParser: он лежал в core/util с самого
    // начала и не вызывался ниоткуда, а PlayerIntent.SetSubtitleCues никто не
    // отправлял — субтитры не работали вообще никак.
    val subtitlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val resolver = context.contentResolver
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "субтитры"
        val content = runCatching {
            resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()

        if (content.isNullOrBlank()) {
            viewModel.handleIntent(PlayerIntent.ShowPlaybackError("Не удалось прочитать файл субтитров"))
            return@rememberLauncherForActivityResult
        }

        val cues = if (name.endsWith(".vtt", ignoreCase = true) || content.startsWith("WEBVTT")) {
            SubtitleParser.parseVtt(content)
        } else {
            SubtitleParser.parseSrt(content)
        }

        if (cues.isEmpty()) {
            viewModel.handleIntent(PlayerIntent.ShowPlaybackError("В файле не нашлось субтитров"))
        } else {
            viewModel.handleIntent(PlayerIntent.LoadExternalSubtitles(name, cues))
        }
    }
    val isInPip = LocalIsInPictureInPicture.current

    // Держим состояние ViewModel в согласии с системой: из PiP выходят и
    // жестом, о котором знает только Activity.
    LaunchedEffect(isInPip) {
        viewModel.handleIntent(PlayerIntent.SetPiP(isInPip))
    }
    // Плеер живёт в PlaybackService, а экран держит только пульт к нему.
    // Пока подключение не завершилось, controller равен null — на первых
    // кадрах это нормально.
    val controller by rememberMediaController()
    val player: Player? = controller

    LaunchedEffect(titleId, episodeId) {
        viewModel.loadEpisode(titleId, episodeId)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(state.quality, state.currentEpisode, state.retryNonce, player) {
        val activePlayer = player ?: return@LaunchedEffect
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
        // Метаданные нужны системному уведомлению и экрану блокировки:
        // без них там будет безымянный прямоугольник.
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaId("${state.titleId}:${episode.id}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(state.titleName)
                    .setSubtitle("Серия ${episode.ordinal}")
                    .setArtworkUri(state.posterUrl?.let(android.net.Uri::parse))
                    .build()
            )
            .build()
        activePlayer.setMediaItem(mediaItem, state.currentPosition)
        activePlayer.prepare()
        activePlayer.playWhenReady = state.isPlaying
    }

    LaunchedEffect(state.isPlaying, player) {
        player?.playWhenReady = state.isPlaying && state.playbackError == null
    }

    LaunchedEffect(state.seekPosition, player) {
        val activePlayer = player ?: return@LaunchedEffect
        val pos = state.seekPosition
        if (pos >= 0L && activePlayer.currentPosition != pos) {
            activePlayer.seekTo(pos)
            viewModel.handleIntent(PlayerIntent.SeekComplete)
        }
    }

    LaunchedEffect(state.speed, player) {
        player?.setPlaybackSpeed(state.speed)
    }

    LaunchedEffect(state.volume, state.isMuted, player) {
        player?.volume = if (state.isMuted) 0f else state.volume.coerceIn(0f, 1f)
    }

    // Выбор встроенной субтитровой дорожки. Через TrackSelectionParameters,
    // а не переключением рендерера: параметры переживают смену качества и
    // пересоздание источника, а прямое включение рендерера — нет.
    LaunchedEffect(state.selectedSubtitleTrackId, player) {
        val activePlayer = player ?: return@LaunchedEffect
        activePlayer.trackSelectionParameters = activePlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(
                androidx.media3.common.C.TRACK_TYPE_TEXT,
                state.selectedSubtitleTrackId == null,
            )
            .apply {
                val selected = state.selectedSubtitleTrackId
                if (selected != null) {
                    // id дорожки — «индекс группы:индекс внутри группы»,
                    // как он был собран в onTracksChanged.
                    val groupIndex = selected.substringBefore(':').toIntOrNull()
                    val trackIndex = selected.substringAfter(':').toIntOrNull()
                    val groups = activePlayer.currentTracks.groups
                        .filter { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT }
                    if (groupIndex != null && trackIndex != null && groupIndex in groups.indices) {
                        setOverrideForType(
                            androidx.media3.common.TrackSelectionOverride(
                                groups[groupIndex].mediaTrackGroup,
                                trackIndex,
                            )
                        )
                    }
                }
            }
            .build()
    }

    DisposableEffect(player) {
        val activePlayer = player ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        viewModel.handleIntent(PlayerIntent.SetBuffering(true))
                    }

                    Player.STATE_READY -> {
                        viewModel.handleIntent(PlayerIntent.SetBuffering(false))
                        viewModel.handleIntent(PlayerIntent.ClearPlaybackError)
                        viewModel.handleIntent(PlayerIntent.UpdateDuration(sanitizeDuration(activePlayer.duration)))
                        viewModel.handleIntent(
                            PlayerIntent.UpdatePosition(
                                sanitizePosition(activePlayer.currentPosition, activePlayer.duration)
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

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                // Что реально есть в потоке, знает только плеер после разбора
                // манифеста — до этого момента список дорожек предсказать нечем.
                val textTracks = tracks.groups
                    .filter { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT }
                    .flatMapIndexed { groupIndex: Int, group: androidx.media3.common.Tracks.Group ->
                        (0 until group.length).map { i ->
                            val format = group.getTrackFormat(i)
                            PlayerSubtitleTrack(
                                id = "${groupIndex}:${i}",
                                label = format.label
                                    ?: format.language?.uppercase()
                                    ?: "Дорожка ${groupIndex + 1}",
                                language = format.language,
                            )
                        }
                    }
                viewModel.handleIntent(PlayerIntent.SetSubtitleTracks(textTracks))
            }

            override fun onPlayerError(error: PlaybackException) {
                val details = error.errorCodeName.ifBlank { error.localizedMessage ?: "Playback error" }
                viewModel.handleIntent(PlayerIntent.ShowPlaybackError("Не удалось воспроизвести видео: $details"))
            }
        }
        activePlayer.addListener(listener)
        onDispose { activePlayer.removeListener(listener) }
    }

    var lastPosition by remember { mutableLongStateOf(0L) }
    LaunchedEffect(player) {
        val activePlayer = player ?: return@LaunchedEffect
        while (true) {
            delay(1000)
            val duration = sanitizeDuration(activePlayer.duration)
            val currentPosition = sanitizePosition(activePlayer.currentPosition, duration)
            if (currentPosition != lastPosition) {
                lastPosition = currentPosition
                viewModel.handleIntent(PlayerIntent.UpdatePosition(currentPosition))
                viewModel.handleIntent(PlayerIntent.UpdateBuffered(activePlayer.bufferedPercentage))
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
            // Плеер НЕ освобождаем: он общий и живёт в сервисе. Раньше здесь
            // стоял player.release(), и именно поэтому воспроизведение
            // обрывалось при любом уходе с экрана.
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
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            // В режиме «только звук» отвязываем поверхность от плеера:
            // видеодекодер останавливается, звук идёт дальше из сервиса.
            update = { view -> view.player = if (state.audioOnly) null else player },
            modifier = Modifier.fillMaxSize()
        )

        if (!isInPip) {
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
            onOpenTracks = { viewModel.handleIntent(PlayerIntent.ShowTracksSheet) },
            onRetry = { viewModel.handleIntent(PlayerIntent.RetryPlayback) },
            onBack = onBack,
            modifier = Modifier.fillMaxSize()
        )
        }

        if (!state.isFullscreen && !isInPip) {
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
                    IconButton(onClick = { viewModel.handleIntent(PlayerIntent.ToggleAudioOnly) }) {
                        Icon(
                            imageVector = if (state.audioOnly) {
                                Icons.Rounded.Headphones
                            } else {
                                Icons.Rounded.Videocam
                            },
                            contentDescription = if (state.audioOnly) {
                                "Включить видео"
                            } else {
                                "Только звук"
                            },
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = {
                        // Сначала реально сворачиваем, потом отмечаем в
                        // состоянии: раньше здесь менялся только флаг, а
                        // системный вызов не делался вовсе.
                        activity?.enterPictureInPicture(player)
                        viewModel.handleIntent(PlayerIntent.StartPiP)
                    }) {
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

    if (state.showTracksSheet) {
        TracksSheet(
            qualities = state.availableQualities.map { quality ->
                TrackOption(id = quality, label = "${quality}p")
            },
            selectedQuality = state.quality,
            onQualitySelected = { viewModel.handleIntent(PlayerIntent.SetQuality(it)) },
            speeds = supportedSpeeds.map { speed ->
                TrackOption(id = speed.toString(), label = formatSpeed(speed))
            },
            selectedSpeed = state.speed.toString(),
            onSpeedSelected = { value ->
                value.toFloatOrNull()?.let { viewModel.handleIntent(PlayerIntent.SetSpeed(it)) }
            },
            subtitleTracks = state.subtitleTracks.map { track ->
                TrackOption(id = track.id, label = track.label, supporting = track.language)
            },
            selectedSubtitle = state.selectedSubtitleTrackId,
            onSubtitleSelected = { viewModel.handleIntent(PlayerIntent.SelectSubtitleTrack(it)) },
            onLoadSubtitleFile = {
                // MIME у .srt на разных устройствах разный, поэтому
                // разрешаем text/* и любой тип: иначе на части прошивок файл
                // просто не виден в системном выборе.
                subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "*/*"))
            },
            onDismiss = { viewModel.handleIntent(PlayerIntent.DismissTracksSheet) },
        )
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

private fun formatSpeed(speed: Float): String =
    if (speed == 1f) "Обычная" else "${speed}×".replace(".0×", "×")

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
