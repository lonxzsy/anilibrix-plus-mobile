package com.anilibrix.plus.ui.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.StudioVideo
import com.anilibrix.plus.domain.repository.DecoderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudioPlayerViewModel @Inject constructor(
    private val decoderRepository: DecoderRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(StudioPlayerUiState())
    val state: StateFlow<StudioPlayerUiState> = _state.asStateFlow()

    private var controlsJob: Job? = null
    private var lastSource: String? = null
    private var lastEpisodeId: String? = null

    fun loadVideo(source: String, id: String) {
        lastSource = source
        lastEpisodeId = id
        viewModelScope.launch {
            val preferredQuality = settingsDataStore.preferredQuality.first()
            val preferredSpeed = settingsDataStore.playerSpeed.first().toFloatOrNull() ?: 1.0f
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    videoUrl = "",
                    videos = emptyList(),
                    selectedQuality = preferredQuality,
                    speed = preferredSpeed
                )
            }
            decoderRepository.getEpisodeVideos(source, id)
                .catch { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isPlaying = false,
                            error = e.message ?: "Не удалось загрузить видео",
                            showControls = true
                        )
                    }
                }
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            val videos = result.data.filter { it.url.isNotBlank() }
                            val selectedVideo = selectVideo(videos, preferredQuality)
                            if (selectedVideo == null) {
                                _state.update {
                                    it.copy(
                                        isLoading = false,
                                        isPlaying = false,
                                        error = "Для этой серии нет доступного видео",
                                        showControls = true
                                    )
                                }
                            } else {
                                _state.update {
                                    it.copy(
                                        videos = videos,
                                        videoUrl = selectedVideo.url,
                                        selectedQuality = selectedVideo.quality ?: preferredQuality,
                                        isLoading = false,
                                        isPlaying = true,
                                        error = null,
                                        showControls = true
                                    )
                                }
                            }
                        }
                        is NetworkResult.Error -> _state.update {
                            it.copy(
                                isLoading = false,
                                isPlaying = false,
                                error = result.message.ifBlank { "Не удалось загрузить видео" },
                                showControls = true
                            )
                        }
                        is NetworkResult.Loading -> {}
                    }
                }
        }
        startAutoHideControls()
    }

    fun handleIntent(intent: StudioPlayerIntent) {
        when (intent) {
            StudioPlayerIntent.PlayPause -> {
                _state.update { it.copy(isPlaying = !it.isPlaying) }
                if (_state.value.isPlaying) startAutoHideControls()
                else controlsJob?.cancel()
            }
            is StudioPlayerIntent.SeekTo -> _state.update { it.copy(currentPosition = intent.position) }
            is StudioPlayerIntent.SetSpeed -> {
                val speed = intent.speed.coerceIn(0.25f, 3.0f)
                _state.update { it.copy(speed = speed) }
                viewModelScope.launch { settingsDataStore.setPlayerSpeed(speed.toString()) }
            }
            is StudioPlayerIntent.SetQuality -> {
                val selectedVideo = selectVideo(_state.value.videos, intent.quality)
                if (selectedVideo != null) {
                    val currentPosition = _state.value.currentPosition
                    _state.update {
                        it.copy(
                            videoUrl = selectedVideo.url,
                            selectedQuality = selectedVideo.quality ?: intent.quality,
                            currentPosition = currentPosition,
                            error = null,
                            isPlaying = true,
                            showControls = true
                        )
                    }
                    viewModelScope.launch {
                        selectedVideo.quality?.let { quality ->
                            settingsDataStore.setPreferredQuality(quality)
                        }
                    }
                }
            }
            StudioPlayerIntent.ToggleControls -> {
                if (_state.value.showControls) {
                    _state.update { it.copy(showControls = false) }
                    controlsJob?.cancel()
                } else {
                    _state.update { it.copy(showControls = true) }
                    startAutoHideControls()
                }
            }
            StudioPlayerIntent.HideControls -> _state.update { it.copy(showControls = false) }
            StudioPlayerIntent.Retry -> {
                _state.update { it.copy(retryNonce = it.retryNonce + 1, error = null) }
                val source = lastSource
                val episodeId = lastEpisodeId
                if (source != null && episodeId != null) {
                    loadVideo(source, episodeId)
                }
            }
            is StudioPlayerIntent.ShowPlaybackError -> {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isPlaying = false,
                        error = intent.message.ifBlank { "Не удалось воспроизвести видео" },
                        showControls = true
                    )
                }
            }
            is StudioPlayerIntent.UpdatePosition -> _state.update { it.copy(currentPosition = intent.position) }
            is StudioPlayerIntent.UpdateDuration -> _state.update { it.copy(duration = intent.duration) }
        }
    }

    private fun selectVideo(videos: List<StudioVideo>, preferredQuality: String?): StudioVideo? {
        if (videos.isEmpty()) return null
        val normalized = preferredQuality?.removeSuffix("p")?.lowercase()
        return videos.firstOrNull { video ->
            val quality = video.quality?.removeSuffix("p")?.lowercase()
            quality == normalized
        } ?: videos.firstOrNull { video ->
            val quality = video.quality?.lowercase().orEmpty()
            normalized != null && quality.contains(normalized)
        } ?: videos.firstOrNull { it.quality?.contains("720") == true }
            ?: videos.first()
    }

    private fun startAutoHideControls() {
        controlsJob?.cancel()
        controlsJob = viewModelScope.launch {
            delay(3000)
            _state.update { it.copy(showControls = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        controlsJob?.cancel()
    }
}
