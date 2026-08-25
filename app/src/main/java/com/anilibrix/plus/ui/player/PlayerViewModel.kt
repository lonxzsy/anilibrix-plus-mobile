package com.anilibrix.plus.ui.player

import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.app.di.ApplicationScope
import com.anilibrix.plus.core.architecture.BaseViewModel
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.core.sync.SyncOperationKind
import com.anilibrix.plus.core.sync.SyncPayload
import com.anilibrix.plus.core.sync.SyncQueue
import com.anilibrix.plus.core.util.SubtitleCue
import com.anilibrix.plus.domain.model.Episode
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.domain.repository.LocalRepository
import com.anilibrix.plus.work.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val localRepository: LocalRepository,
    private val anilibriaRepository: AnilibriaRepository,
    private val syncQueue: SyncQueue,
    private val syncScheduler: SyncScheduler,
    private val aniSkipRepository: com.anilibrix.plus.domain.repository.AniSkipRepository,
    private val kodikRepository: com.anilibrix.plus.domain.repository.KodikRepository,
    /**
     * Сохранение прогресса живёт дольше экрана: запись в базу не должна
     * отменяться от того, что пользователь вышел из плеера — именно в этот
     * момент её и надо гарантированно довести до конца.
     */
    @ApplicationScope private val applicationScope: CoroutineScope,
) : BaseViewModel<PlayerUiState, Unit>() {

    override val initialUiState: PlayerUiState = PlayerUiState()

    private var controlsJob: Job? = null
    private var saveProgressJob: Job? = null
    private var openingTimerJob: Job? = null
    private var endingTimerJob: Job? = null
    private var autoAdvanceJob: Job? = null

    /**
     * Режим пропуска заставок. Читается один раз при старте и обновляется
     * подпиской: настройку меняют в профиле, а не в плеере.
     */
    private var skipMode: SkipMode = SkipMode.ASK

    init {
        viewModelScope.launch {
            settingsDataStore.playerSkipMode.collect { skipMode = SkipMode.fromStorage(it) }
        }
    }

    fun initialize(episode: Episode, titleName: String, titleId: Long, posterUrl: String?, restorePosition: Boolean = true) {
        updateState {
            copy(
                currentEpisode = episode,
                titleName = titleName,
                titleId = titleId,
                posterUrl = posterUrl,
                isLoading = false,
                isPlaying = true,
                availableQualities = episode.availableQualities(),
                playbackError = null,
                isBuffering = false,
                subtitleText = ""
            )
        }
        viewModelScope.launch {
            val quality = settingsDataStore.preferredQuality.first()
            val speed = settingsDataStore.playerSpeed.first().toFloatOrNull() ?: 1.0f
            val subtitlesEnabled = settingsDataStore.playerSubtitlesEnabled.first()
            val subtitleSize = settingsDataStore.playerSubtitlesSize.first()
            val subtitleColor = settingsDataStore.playerSubtitlesColor.first()
            updateState {
                copy(
                    quality = quality,
                    speed = speed.coerceIn(0.25f, 3.0f),
                    subtitlesEnabled = subtitlesEnabled,
                    subtitleSizeSp = subtitleSize.coerceIn(14, 48),
                    subtitleColorHex = subtitleColor,
                    subtitleText = if (subtitlesEnabled) subtitleAt(currentPosition) else ""
                )
            }
            if (restorePosition) {
                // Точечный запрос вместо выборки всей истории (до 500 строк)
                // с фильтрацией в памяти на каждом открытии плеера.
                localRepository.getHistoryEntry(titleId, episode.id)?.let { saved ->
                    if (saved.timestamp > 0 && saved.duration > 0) {
                        val duration = saved.duration.coerceAtLeast(0L)
                        val position = saved.timestamp.coerceIn(0L, duration)
                        updateState {
                            copy(
                                currentPosition = position,
                                seekPosition = position,
                                duration = duration,
                                subtitleText = subtitleAt(position)
                            )
                        }
                    }
                }
            } else {
                updateState {
                    copy(
                        currentPosition = 0L,
                        seekPosition = 0L,
                        duration = 0L,
                        subtitleText = ""
                    )
                }
            }

            // Автоматическое получение таймкодов опенингов из AniSkip, если у серии их нет
            if (episode.opening == null && episode.ending == null) {
                if (settingsDataStore.aniskipEnabled.first()) {
                    val (op, ed) = aniSkipRepository.getSkipIntervals(
                        malId = titleId,
                        episodeNumber = episode.ordinal,
                        episodeLengthSeconds = episode.duration.toDouble()
                    )
                    if (op != null || ed != null) {
                        updateState {
                            copy(currentEpisode = currentEpisode?.copy(opening = op, ending = ed))
                        }
                    }
                }
            }
        }
        startAutoHideControls()
        startSaveProgressTimer()
    }

    fun loadEpisode(titleAlias: String, episodeId: Long) {
        viewModelScope.launch {
            updateState {
                copy(
                    isLoading = true,
                    isBuffering = false,
                    playbackError = null
                )
            }
            anilibriaRepository.getRelease(titleAlias)
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            val release = result.data
                            val episodes = release.episodes ?: emptyList()
                            val episode = episodes.find { it.id == episodeId }
                            if (episode != null) {
                                updateState { copy(allEpisodes = episodes) }
                                initialize(episode, release.name.main, release.id, release.poster?.fullUrl)
                            } else {
                                // Попытка найти стороннюю серию (Kodik)
                                kodikRepository.getEpisodes(
                                    shikimoriId = null,
                                    malId = release.malId?.toLong(),
                                    translationId = null,
                                    kodikId = null
                                ).collect { kodikResult ->
                                    if (kodikResult is NetworkResult.Success && kodikResult.data.isNotEmpty()) {
                                        val kEp = kodikResult.data.find { it.id == episodeId }
                                            ?: kodikResult.data.firstOrNull()
                                        if (kEp != null) {
                                            updateState { copy(allEpisodes = kodikResult.data) }
                                            initialize(kEp, release.name.main, release.id, release.poster?.fullUrl)
                                            return@collect
                                        }
                                    }
                                    updateState {
                                        copy(
                                            isLoading = false,
                                            isPlaying = false,
                                            isBuffering = false,
                                            playbackError = "Серия недоступна или была удалена"
                                        )
                                    }
                                }
                            }
                        }
                        is NetworkResult.Error -> {
                            updateState {
                                copy(
                                    isLoading = false,
                                    isPlaying = false,
                                    isBuffering = false,
                                    playbackError = result.message.ifBlank { "Не удалось загрузить серию" }
                                )
                            }
                        }
                        NetworkResult.Loading -> updateState {
                            copy(
                                isLoading = true,
                                playbackError = null
                            )
                        }
                    }
                }
        }
    }

    fun handleIntent(intent: PlayerIntent) {
        when (intent) {
            PlayerIntent.PlayPause -> togglePlayPause()
            is PlayerIntent.SeekTo -> seekTo(intent.position)
            is PlayerIntent.SeekRelative -> seekRelative(intent.deltaMs)
            is PlayerIntent.SetQuality -> setQuality(intent.quality)
            is PlayerIntent.SetSpeed -> setSpeed(intent.speed)
            PlayerIntent.ToggleControls -> toggleControls()
            PlayerIntent.HideControls -> hideControls()
            PlayerIntent.ShowControls -> showControls()
            is PlayerIntent.SaveProgress -> saveProgress(intent.position, intent.duration)
            PlayerIntent.SkipOpening -> handleSkipOpening()
            PlayerIntent.SkipEnding -> handleSkipEnding()
            PlayerIntent.DismissAutoAdvance -> dismissAutoAdvance()
            PlayerIntent.SkipAutoAdvance -> skipAutoAdvance()
            PlayerIntent.ToggleFullscreen -> updateState { copy(isFullscreen = !isFullscreen) }
            PlayerIntent.StartPiP -> updateState { copy(isPiP = true) }
            is PlayerIntent.SetPiP -> updateState { copy(isPiP = intent.active) }
            PlayerIntent.ShowTracksSheet -> updateState { copy(showTracksSheet = true, showControls = true) }
            PlayerIntent.DismissTracksSheet -> updateState { copy(showTracksSheet = false) }
            is PlayerIntent.SetSubtitleTracks -> updateState { copy(subtitleTracks = intent.tracks) }
            is PlayerIntent.SelectSubtitleTrack -> updateState {
                copy(
                    selectedSubtitleTrackId = intent.trackId,
                    subtitlesEnabled = intent.trackId != null || externalSubtitleName != null,
                )
            }
            is PlayerIntent.LoadExternalSubtitles -> updateState {
                copy(
                    subtitleCues = intent.cues,
                    externalSubtitleName = intent.name,
                    subtitlesEnabled = true,
                    selectedSubtitleTrackId = null,
                    showTracksSheet = false,
                    subtitleText = subtitleAt(currentPosition, intent.cues),
                )
            }
            PlayerIntent.StopPiP -> updateState { copy(isPiP = false) }
            is PlayerIntent.UpdatePosition -> updatePosition(intent.position)
            is PlayerIntent.UpdateDuration -> updateState { copy(duration = intent.duration.coerceAtLeast(0L)) }
            is PlayerIntent.UpdateBuffered -> updateState { copy(bufferedPercentage = intent.percentage.coerceIn(0, 100)) }
            PlayerIntent.ToggleSubtitles -> updateState {
                val enabled = !subtitlesEnabled
                viewModelScope.launch { settingsDataStore.setPlayerSubtitlesEnabled(enabled) }
                copy(
                    subtitlesEnabled = enabled,
                    subtitleText = if (enabled) subtitleAt(currentPosition) else ""
                )
            }
            is PlayerIntent.SetSubtitleSize -> {
                val size = intent.sizeSp.coerceIn(14, 48)
                updateState { copy(subtitleSizeSp = size) }
                viewModelScope.launch { settingsDataStore.setPlayerSubtitlesSize(size) }
            }
            is PlayerIntent.SetSubtitleColor -> {
                updateState { copy(subtitleColorHex = intent.colorHex) }
                viewModelScope.launch { settingsDataStore.setPlayerSubtitlesColor(intent.colorHex) }
            }
            is PlayerIntent.SetVolume -> updateState { copy(volume = intent.volume.coerceIn(0f, 1f)) }
            PlayerIntent.ToggleMute -> updateState { copy(isMuted = !isMuted) }
            PlayerIntent.ToggleAudioOnly -> updateState { copy(audioOnly = !audioOnly) }
            is PlayerIntent.SkipToNext -> skipToNext(intent.episode)
            is PlayerIntent.SetSubtitleCues -> updateState {
                copy(
                    subtitleCues = intent.cues,
                    subtitleText = if (subtitlesEnabled) subtitleAt(currentPosition, intent.cues) else ""
                )
            }
            is PlayerIntent.SetBuffering -> updateState { copy(isBuffering = intent.isBuffering) }
            is PlayerIntent.ShowPlaybackError -> updateState {
                copy(
                    isLoading = false,
                    isPlaying = false,
                    isBuffering = false,
                    playbackError = intent.message.ifBlank { "Не удалось воспроизвести видео" },
                    showControls = true
                )
            }
            PlayerIntent.ClearPlaybackError -> updateState { copy(playbackError = null) }
            PlayerIntent.RetryPlayback -> updateState {
                copy(
                    playbackError = null,
                    isLoading = true,
                    isBuffering = true,
                    isPlaying = true,
                    retryNonce = retryNonce + 1
                )
            }
            PlayerIntent.OnVideoEnded -> handleVideoEnded()
            PlayerIntent.SeekComplete -> updateState { copy(seekPosition = -1L) }
            is PlayerIntent.SetBrightness -> updateState { copy(brightness = intent.brightness.coerceIn(0.01f, 1f)) }
            PlayerIntent.ToggleTouchLock -> updateState {
                val newLocked = !isTouchLocked
                copy(isTouchLocked = newLocked, showControls = !newLocked)
            }
            is PlayerIntent.SetAspectRatio -> updateState { copy(aspectRatioMode = intent.mode) }
            is PlayerIntent.SetAudioDelay -> updateState { copy(audioDelayMs = intent.offsetMs) }
            is PlayerIntent.SetSubtitleDelay -> updateState {
                copy(
                    subtitleDelayMs = intent.offsetMs,
                    subtitleText = subtitleAt(currentPosition, subtitleCues, intent.offsetMs)
                )
            }
        }
    }

    private fun togglePlayPause() {
        updateState { copy(isPlaying = !isPlaying, playbackError = null) }
        if (uiState.value.isPlaying) {
            startAutoHideControls()
        } else {
            controlsJob?.cancel()
            // Пауза — самый вероятный момент, когда человек уходит из
            // приложения. Фиксируем позицию сразу, не дожидаясь таймера.
            flushProgress()
        }
    }

    private fun seekTo(position: Long) {
        val state = uiState.value
        val newPosition = clampPosition(position, state.duration)
        updateState {
            copy(
                currentPosition = newPosition,
                seekPosition = newPosition,
                subtitleText = subtitleAt(newPosition),
                playbackError = null
            )
        }
    }

    private fun seekRelative(deltaMs: Long) {
        val state = uiState.value
        val newPosition = clampPosition(state.currentPosition + deltaMs, state.duration)
        updateState {
            copy(
                currentPosition = newPosition,
                seekPosition = newPosition,
                subtitleText = subtitleAt(newPosition),
                playbackError = null,
                isPlaying = true
            )
        }
    }

    private fun updatePosition(position: Long) {
        val safePosition = clampPosition(position, uiState.value.duration)
        updateState {
            copy(
                currentPosition = safePosition,
                subtitleText = subtitleAt(safePosition)
            )
        }
    }

    private fun subtitleAt(
        position: Long,
        cues: List<SubtitleCue> = uiState.value.subtitleCues,
        delayMs: Long = uiState.value.subtitleDelayMs
    ): String {
        if (!uiState.value.subtitlesEnabled || position < 0L || cues.isEmpty()) return ""
        val adjustedPosition = (position - delayMs).coerceAtLeast(0L)
        return cues
            .lastOrNull { cue -> adjustedPosition >= cue.startTime && adjustedPosition <= cue.endTime }
            ?.text
            .orEmpty()
    }

    private fun clampPosition(position: Long, duration: Long): Long {
        val safeDuration = duration.coerceAtLeast(0L)
        val safePosition = position.coerceAtLeast(0L)
        return if (safeDuration > 0L) safePosition.coerceAtMost(safeDuration) else safePosition
    }

    private fun setQuality(quality: String) {
        val normalizedQuality = quality.takeIf { it.isNotBlank() } ?: "Auto"
        val currentPos = uiState.value.currentPosition
        updateState { copy(quality = normalizedQuality, seekPosition = currentPos, playbackError = null) }
        viewModelScope.launch { settingsDataStore.setPreferredQuality(normalizedQuality) }
    }

    private fun setSpeed(speed: Float) {
        val normalized = speed.coerceIn(0.25f, 3.0f)
        updateState { copy(speed = normalized) }
        viewModelScope.launch { settingsDataStore.setPlayerSpeed(normalized.toString()) }
    }

    private fun toggleControls() {
        if (uiState.value.showControls) {
            updateState { copy(showControls = false) }
            controlsJob?.cancel()
        } else {
            updateState { copy(showControls = true) }
            startAutoHideControls()
        }
    }

    private fun hideControls() {
        updateState { copy(showControls = false) }
    }

    private fun showControls() {
        updateState { copy(showControls = true) }
        startAutoHideControls()
    }

    private fun startAutoHideControls() {
        controlsJob?.cancel()
        controlsJob = viewModelScope.launch {
            delay(3000)
            if (uiState.value.playbackError == null) {
                updateState { copy(showControls = false) }
            }
        }
    }

    private fun startSaveProgressTimer() {
        saveProgressJob?.cancel()
        saveProgressJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                val state = uiState.value
                if (state.currentEpisode != null && state.currentPosition > 0) {
                    saveProgress(state.currentPosition, state.duration)
                }
            }
        }
    }

    /**
     * Записывает позицию просмотра.
     *
     * Раньше досмотренная почти до конца серия **не сохранялась вовсе**:
     * стояла проверка «если до конца меньше 10 секунд — выйти». В результате
     * серия, просмотренная целиком, не оставляла в истории ни следа, отметка
     * «просмотрено» никогда не появлялась, а суммарное время просмотра в
     * профиле систематически занижалось.
     *
     * Теперь такая серия записывается с позицией, равной длительности, — то
     * есть честно помечается досмотренной.
     */
    private fun saveProgress(position: Long, duration: Long) {
        val state = uiState.value
        val episode = state.currentEpisode ?: return
        val safeDuration = duration.coerceAtLeast(0L)
        if (state.titleId == 0L || safeDuration <= 0L) return

        val rawPosition = clampPosition(position, safeDuration)
        if (rawPosition <= 0L) return

        val nearCompletionThreshold = 10_000L
        val completed = safeDuration - rawPosition <= nearCompletionThreshold
        val safePosition = if (completed) safeDuration else rawPosition

        val entry = HistoryEntry(
            titleId = state.titleId,
            titleName = state.titleName,
            posterUrl = state.posterUrl,
            episodeId = episode.id,
            episodeNumber = episode.ordinal,
            timestamp = safePosition,
            duration = safeDuration,
            watchedAt = System.currentTimeMillis(),
            releaseEpisodeId = episode.releaseEpisodeId,
        )
        applicationScope.launch {
            localRepository.addHistory(entry)
            // Через очередь, а не прямым запросом: без сети прогресс раньше
            // просто терялся, и после возвращения онлайн сервер отдавал
            // старую позицию, откатывая просмотр назад.
            syncQueue.enqueue(
                kind = SyncOperationKind.TIMECODE_UPDATE,
                titleId = state.titleId,
                payload = SyncPayload(
                    releaseEpisodeId = episode.releaseEpisodeId,
                    positionMs = safePosition,
                    durationMs = safeDuration,
                ),
            )
            if (completed) {
                // Счётчик серий для внешнего трекера пересчитывается только
                // по факту досмотра — промежуточные позиции ему не нужны.
                localRepository.getCollectionType(state.titleId)?.let { status ->
                    syncQueue.enqueue(
                        kind = SyncOperationKind.SHIKIMORI_RATE,
                        titleId = state.titleId,
                        payload = SyncPayload(status = status.value),
                    )
                }
            }
            syncScheduler.syncNow()
        }
    }

    /**
     * Сохранить прямо сейчас — на паузе и при уходе с экрана.
     *
     * Без этого прогресс писался только по пятисекундному таймеру, и вылет
     * приложения стоил до пяти секунд просмотра.
     */
    fun flushProgress() {
        val state = uiState.value
        if (state.currentPosition > 0L) {
            saveProgress(state.currentPosition, state.duration)
        }
    }

    fun checkSkipRanges() {
        val state = uiState.value
        val episode = state.currentEpisode ?: return
        val position = state.currentPosition
        val duration = state.duration

        val openingStart = (episode.opening?.start ?: 0.0) * 1000.0
        val openingStop = (episode.opening?.stop ?: 0.0) * 1000.0
        val endingStart = (episode.ending?.start ?: 0.0) * 1000.0
        val endingStop = (episode.ending?.stop ?: 0.0) * 1000.0

        // В режиме «Никогда» кнопка не показывается вовсе: если человек
        // опенинги смотрит, предложение их пропустить — просто помеха.
        if (skipMode == SkipMode.NEVER) return

        if (!state.skipOpening.active && episode.opening != null && position in openingStart.toLong()..openingStop.toLong()) {
            startOpeningTimer()
        } else if (state.skipOpening.active && episode.opening != null && position >= openingStop.toLong()) {
            openingTimerJob?.cancel()
            updateState { copy(skipOpening = SkipState()) }
        }

        if (!state.skipEnding.active && episode.ending != null && position in endingStart.toLong()..endingStop.toLong()) {
            startEndingTimer()
        } else if (state.skipEnding.active && episode.ending != null && position >= endingStop.toLong()) {
            endingTimerJob?.cancel()
            updateState { copy(skipEnding = SkipState()) }
        }

        if (!state.autoAdvance.active && duration > 0 && position >= duration - 5000) {
            startAutoAdvance()
        }
    }

    private fun startOpeningTimer() {
        openingTimerJob?.cancel()
        val auto = skipMode == SkipMode.AUTO
        updateState {
            copy(skipOpening = SkipState(active = true, remainingSeconds = COUNTDOWN_SECONDS, progress = 0f, autoSkip = auto))
        }
        // В режиме «Спрашивать» кнопка висит без отсчёта и ждёт решения.
        if (!auto) return
        openingTimerJob = viewModelScope.launch {
            runCountdown { seconds, progress ->
                updateState {
                    copy(skipOpening = skipOpening.copy(remainingSeconds = seconds, progress = progress))
                }
            }
            handleSkipOpening()
        }
    }

    private fun startEndingTimer() {
        endingTimerJob?.cancel()
        val auto = skipMode == SkipMode.AUTO
        updateState {
            copy(skipEnding = SkipState(active = true, remainingSeconds = COUNTDOWN_SECONDS, progress = 0f, autoSkip = auto))
        }
        if (!auto) return
        endingTimerJob = viewModelScope.launch {
            runCountdown { seconds, progress ->
                updateState {
                    copy(skipEnding = skipEnding.copy(remainingSeconds = seconds, progress = progress))
                }
            }
            handleSkipEnding()
        }
    }

    /**
     * Отсчёт до автопропуска.
     *
     * Шагает по 100 мс, а не по секунде: кольцо должно ехать плавно, а не
     * прыгать тремя рывками. Цифра при этом обновляется только на смене
     * секунды — дёргающийся счётчик читать невозможно.
     */
    private suspend fun runCountdown(onTick: (seconds: Int, progress: Float) -> Unit) {
        val totalMs = COUNTDOWN_SECONDS * 1000L
        var elapsed = 0L
        while (elapsed < totalMs) {
            val remaining = ((totalMs - elapsed + 999L) / 1000L).toInt()
            onTick(remaining, elapsed.toFloat() / totalMs)
            delay(COUNTDOWN_STEP_MS)
            elapsed += COUNTDOWN_STEP_MS
        }
        onTick(0, 1f)
    }

    private fun handleSkipOpening() {
        openingTimerJob?.cancel()
        val episode = uiState.value.currentEpisode ?: return
        val target = ((episode.opening?.stop ?: 0.0) * 1000.0).toLong()
        if (target <= 0L) return
        updateState {
            copy(
                currentPosition = target,
                seekPosition = target,
                subtitleText = subtitleAt(target),
                skipOpening = SkipState()
            )
        }
    }

    private fun handleSkipEnding() {
        endingTimerJob?.cancel()
        val episode = uiState.value.currentEpisode ?: return
        val target = ((episode.ending?.stop ?: 0.0) * 1000.0).toLong()
        if (target <= 0L) return
        updateState {
            copy(
                currentPosition = target,
                seekPosition = target,
                subtitleText = subtitleAt(target),
                skipEnding = SkipState()
            )
        }
    }

    private fun startAutoAdvance() {
        val currentEpisode = uiState.value.currentEpisode ?: return
        val allEpisodes = uiState.value.allEpisodes
        val currentIndex = allEpisodes.indexOfFirst { it.id == currentEpisode.id }
        val nextEpisode = if (currentIndex >= 0 && currentIndex < allEpisodes.size - 1) {
            allEpisodes[currentIndex + 1]
        } else {
            null
        }

        updateState {
            copy(
                autoAdvance = AutoAdvanceState(
                    active = true,
                    remainingSeconds = 5,
                    nextEpisode = nextEpisode
                )
            )
        }
        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            for (i in 5 downTo 1) {
                updateState { copy(autoAdvance = autoAdvance.copy(remainingSeconds = i)) }
                delay(1000)
            }
            if (nextEpisode != null) {
                skipToNext(nextEpisode)
            } else {
                updateState {
                    copy(
                        isPlaying = false,
                        autoAdvance = AutoAdvanceState()
                    )
                }
            }
        }
    }

    private fun dismissAutoAdvance() {
        autoAdvanceJob?.cancel()
        updateState { copy(autoAdvance = AutoAdvanceState()) }
    }

    private fun skipAutoAdvance() {
        autoAdvanceJob?.cancel()
        val next = uiState.value.autoAdvance.nextEpisode
        if (next != null) {
            skipToNext(next)
        } else {
            updateState { copy(autoAdvance = AutoAdvanceState()) }
        }
    }

    private fun skipToNext(episode: Episode) {
        val titleName = uiState.value.titleName
        val titleId = uiState.value.titleId
        val posterUrl = uiState.value.posterUrl

        updateState {
            copy(
                skipOpening = SkipState(),
                skipEnding = SkipState(),
                autoAdvance = AutoAdvanceState(),
                subtitleCues = emptyList(),
                subtitleText = "",
                playbackError = null,
                isBuffering = false
            )
        }

        initialize(episode, titleName, titleId, posterUrl, restorePosition = false)
    }

    private fun handleVideoEnded() {
        val currentEpisode = uiState.value.currentEpisode ?: return
        val allEpisodes = uiState.value.allEpisodes
        val currentIndex = allEpisodes.indexOfFirst { it.id == currentEpisode.id }
        val nextEpisode = if (currentIndex >= 0 && currentIndex < allEpisodes.size - 1) {
            allEpisodes[currentIndex + 1]
        } else {
            null
        }

        if (nextEpisode != null) {
            skipToNext(nextEpisode)
        } else {
            updateState { copy(isPlaying = false, isBuffering = false) }
        }
    }

    override fun onCleared() {
        // Сохраняем ДО отмены задач: иначе выход с экрана терял до пяти
        // секунд просмотра — ровно интервал таймера.
        flushProgress()
        super.onCleared()
        controlsJob?.cancel()
        saveProgressJob?.cancel()
        openingTimerJob?.cancel()
        endingTimerJob?.cancel()
        autoAdvanceJob?.cancel()
    }

    private companion object {
        const val COUNTDOWN_SECONDS = 3
        const val COUNTDOWN_STEP_MS = 100L
    }

    private fun Episode.availableQualities(): List<String> {
        return buildList {
            if (!hls480.isNullOrBlank()) add("480")
            if (!hls720.isNullOrBlank()) add("720")
            if (!hls1080.isNullOrBlank()) add("1080")
        }
    }
}
