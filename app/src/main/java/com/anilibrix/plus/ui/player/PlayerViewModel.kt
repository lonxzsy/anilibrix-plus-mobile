package com.anilibrix.plus.ui.player

import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.core.architecture.BaseViewModel
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.core.util.SubtitleCue
import com.anilibrix.plus.domain.model.Episode
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.domain.repository.LocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val localRepository: LocalRepository,
    private val anilibriaRepository: AnilibriaRepository
) : BaseViewModel<PlayerUiState, Unit>() {

    override val initialUiState: PlayerUiState = PlayerUiState()

    private var controlsJob: Job? = null
    private var saveProgressJob: Job? = null
    private var openingTimerJob: Job? = null
    private var endingTimerJob: Job? = null
    private var autoAdvanceJob: Job? = null

    fun initialize(episode: Episode, titleName: String, titleId: Long, posterUrl: String?, restorePosition: Boolean = true) {
        updateState {
            copy(
                currentEpisode = episode,
                titleName = titleName,
                titleId = titleId,
                posterUrl = posterUrl,
                isLoading = false,
                isPlaying = true,
                quality = "1080",
                speed = 1.0f,
                playbackError = null,
                isBuffering = false,
                subtitleText = ""
            )
        }
        viewModelScope.launch {
            val quality = settingsDataStore.preferredQuality.first()
            updateState { copy(quality = quality) }
            if (restorePosition) {
                localRepository.getHistory().first().find {
                    it.titleId == titleId && it.episodeId == episode.id
                }?.let { saved ->
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
                                initialize(episode, release.name.main, release.id, release.poster?.medium ?: release.poster?.original)
                            } else {
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
            PlayerIntent.StopPiP -> updateState { copy(isPiP = false) }
            is PlayerIntent.UpdatePosition -> updatePosition(intent.position)
            is PlayerIntent.UpdateDuration -> updateState { copy(duration = intent.duration.coerceAtLeast(0L)) }
            is PlayerIntent.UpdateBuffered -> updateState { copy(bufferedPercentage = intent.percentage.coerceIn(0, 100)) }
            PlayerIntent.ToggleSubtitles -> updateState {
                val enabled = !subtitlesEnabled
                copy(
                    subtitlesEnabled = enabled,
                    subtitleText = if (enabled) subtitleAt(currentPosition) else ""
                )
            }
            is PlayerIntent.SetVolume -> updateState { copy(volume = intent.volume.coerceIn(0f, 1f)) }
            PlayerIntent.ToggleMute -> updateState { copy(isMuted = !isMuted) }
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
        }
    }

    private fun togglePlayPause() {
        updateState { copy(isPlaying = !isPlaying, playbackError = null) }
        if (uiState.value.isPlaying) startAutoHideControls()
        else controlsJob?.cancel()
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

    private fun subtitleAt(position: Long, cues: List<SubtitleCue> = uiState.value.subtitleCues): String {
        if (!uiState.value.subtitlesEnabled || position < 0L || cues.isEmpty()) return ""
        return cues
            .lastOrNull { cue -> position >= cue.startTime && position <= cue.endTime }
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
        updateState { copy(speed = speed.coerceIn(0.25f, 3.0f)) }
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

    private fun saveProgress(position: Long, duration: Long) {
        val state = uiState.value
        val episode = state.currentEpisode ?: return
        val safeDuration = duration.coerceAtLeast(0L)
        val safePosition = clampPosition(position, safeDuration)
        val nearCompletionThreshold = 10_000L
        if (state.titleId == 0L || safeDuration <= 0L || safePosition <= 0L) return
        if (safeDuration - safePosition <= nearCompletionThreshold) return
        val entry = HistoryEntry(
            titleId = state.titleId,
            titleName = state.titleName,
            posterUrl = state.posterUrl,
            episodeId = episode.id,
            episodeNumber = episode.ordinal,
            timestamp = safePosition,
            duration = safeDuration,
            watchedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            localRepository.addHistory(entry)
            if (episode.releaseEpisodeId.isNotBlank()) {
                anilibriaRepository.updateTimecode(
                    episode.releaseEpisodeId,
                    safePosition,
                    safeDuration
                ).collect {}
            }
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
        updateState { copy(skipOpening = SkipState(active = true, remainingSeconds = 3, progress = 0f)) }
        openingTimerJob = viewModelScope.launch {
            for (i in 3 downTo 1) {
                updateState { copy(skipOpening = SkipState(active = true, remainingSeconds = i, progress = (3 - i + 1) / 3f)) }
                delay(1000)
            }
            handleSkipOpening()
        }
    }

    private fun startEndingTimer() {
        endingTimerJob?.cancel()
        updateState { copy(skipEnding = SkipState(active = true, remainingSeconds = 3, progress = 0f)) }
        endingTimerJob = viewModelScope.launch {
            for (i in 3 downTo 1) {
                updateState { copy(skipEnding = SkipState(active = true, remainingSeconds = i, progress = (3 - i + 1) / 3f)) }
                delay(1000)
            }
            handleSkipEnding()
        }
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
        super.onCleared()
        controlsJob?.cancel()
        saveProgressJob?.cancel()
        openingTimerJob?.cancel()
        endingTimerJob?.cancel()
        autoAdvanceJob?.cancel()
    }
}
