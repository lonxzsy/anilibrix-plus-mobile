package com.anilibrix.plus.ui.player

import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.core.architecture.BaseViewModel
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.domain.model.Episode
import com.anilibrix.plus.domain.model.HistoryEntry
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
                speed = 1.0f
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
                        updateState {
                            copy(
                                currentPosition = saved.timestamp,
                                seekPosition = saved.timestamp,
                                duration = saved.duration
                            )
                        }
                    }
                }
            } else {
                // Если не восстанавливаем позицию, начинаем с начала
                updateState {
                    copy(
                        currentPosition = 0L,
                        seekPosition = 0L,
                        duration = 0L
                    )
                }
            }
        }
        startAutoHideControls()
        startSaveProgressTimer()
    }

    fun loadEpisode(titleAlias: String, episodeId: Long) {
        viewModelScope.launch {
            anilibriaRepository.getRelease(titleAlias)
                .collect { result ->
                    when (result) {
                        is com.anilibrix.plus.domain.model.NetworkResult.Success -> {
                            val release = result.data
                            val episodes = release.episodes ?: emptyList()
                            val episode = episodes.find { it.id == episodeId }
                            if (episode != null) {
                                updateState { copy(allEpisodes = episodes) }
                                initialize(episode, release.name.main, release.id, release.poster?.medium ?: release.poster?.original)
                            } else {
                                updateState { copy(isLoading = false) }
                            }
                        }
                        is com.anilibrix.plus.domain.model.NetworkResult.Error -> {
                            updateState { copy(isLoading = false) }
                        }
                        is com.anilibrix.plus.domain.model.NetworkResult.Loading -> {}
                    }
                }
        }
    }

    fun handleIntent(intent: PlayerIntent) {
        when (intent) {
            PlayerIntent.PlayPause -> togglePlayPause()
            is PlayerIntent.SeekTo -> updateState { copy(currentPosition = intent.position, seekPosition = intent.position) }
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
            is PlayerIntent.UpdatePosition -> updateState { copy(currentPosition = intent.position) }
            is PlayerIntent.UpdateDuration -> updateState { copy(duration = intent.duration) }
            is PlayerIntent.UpdateBuffered -> updateState { copy(bufferedPercentage = intent.percentage) }
            PlayerIntent.ToggleSubtitles -> updateState { copy(subtitlesEnabled = !subtitlesEnabled) }
            is PlayerIntent.SetVolume -> updateState { copy(volume = intent.volume) }
            PlayerIntent.ToggleMute -> updateState { copy(isMuted = !isMuted) }
            is PlayerIntent.SkipToNext -> skipToNext(intent.episode)
            is PlayerIntent.SetSubtitleCues -> updateState { copy(subtitleCues = intent.cues) }
            PlayerIntent.OnVideoEnded -> handleVideoEnded()
        }
    }

    private fun togglePlayPause() {
        updateState { copy(isPlaying = !isPlaying) }
        if (uiState.value.isPlaying) startAutoHideControls()
        else controlsJob?.cancel()
    }

    private fun seekRelative(deltaMs: Long) {
        val newPosition = (uiState.value.currentPosition + deltaMs).coerceIn(0L, uiState.value.duration)
        updateState { copy(currentPosition = newPosition, seekPosition = newPosition) }
        updateState { copy(isPlaying = true) }
    }

    private fun setQuality(quality: String) {
        val currentPos = uiState.value.currentPosition
        updateState { copy(quality = quality, seekPosition = currentPos) }
        viewModelScope.launch { settingsDataStore.setPreferredQuality(quality) }
    }

    private fun setSpeed(speed: Float) {
        updateState { copy(speed = speed) }
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
            updateState { copy(showControls = false) }
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
        if (state.titleId == 0L) return
        val entry = HistoryEntry(
            titleId = state.titleId,
            titleName = state.titleName,
            posterUrl = state.posterUrl,
            episodeId = episode.id,
            episodeNumber = episode.ordinal,
            timestamp = position,
            duration = duration,
            watchedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            localRepository.addHistory(entry)
            anilibriaRepository.updateTimecode(state.titleId, episode.id, position, duration, episode.releaseEpisodeId).collect {}
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
            startOpeningTimer(openingStop.toLong())
        } else if (state.skipOpening.active && episode.opening != null && position >= openingStop.toLong()) {
            openingTimerJob?.cancel()
            updateState { copy(skipOpening = SkipState()) }
        }

        if (!state.skipEnding.active && episode.ending != null && position in endingStart.toLong()..endingStop.toLong()) {
            startEndingTimer(endingStop.toLong())
        } else if (state.skipEnding.active && episode.ending != null && position >= endingStop.toLong()) {
            endingTimerJob?.cancel()
            updateState { copy(skipEnding = SkipState()) }
        }

        if (!state.autoAdvance.active && duration > 0 && position >= duration - 5000) {
            startAutoAdvance()
        }
    }

    private fun startOpeningTimer(targetPosition: Long) {
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

    private fun startEndingTimer(targetPosition: Long) {
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
                skipEnding = SkipState()
            )
        }
    }

    private fun startAutoAdvance() {
        val currentEpisode = uiState.value.currentEpisode ?: return
        val allEpisodes = uiState.value.allEpisodes
        
        // Найти следующую серию
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
            // Автоматически переключиться на следующую серию
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
                subtitleCues = emptyList()
            )
        }
        
        // Инициализировать новую серию без восстановления позиции
        initialize(episode, titleName, titleId, posterUrl, restorePosition = false)
    }

    private fun handleVideoEnded() {
        // Когда видео закончилось, запустить автопереход на следующую серию
        val currentEpisode = uiState.value.currentEpisode ?: return
        val allEpisodes = uiState.value.allEpisodes
        
        val currentIndex = allEpisodes.indexOfFirst { it.id == currentEpisode.id }
        val nextEpisode = if (currentIndex >= 0 && currentIndex < allEpisodes.size - 1) {
            allEpisodes[currentIndex + 1]
        } else {
            null
        }
        
        if (nextEpisode != null) {
            // Если есть следующая серия, автоматически переключиться на нее
            skipToNext(nextEpisode)
        } else {
            // Если следующей серии нет, просто остановить воспроизведение
            updateState { copy(isPlaying = false) }
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
