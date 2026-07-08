package com.anilibrix.plus.ui.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudioPlayerViewModel @Inject constructor(
    private val decoderRepository: DecoderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StudioPlayerUiState())
    val state: StateFlow<StudioPlayerUiState> = _state.asStateFlow()

    private var controlsJob: Job? = null

    fun loadVideo(source: String, id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, videoUrl = "") }
            decoderRepository.getEpisodeVideos(source, id)
                .catch { e ->
                    _state.update { it.copy(isLoading = false) }
                }
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            val url = result.data.firstOrNull()?.url ?: ""
                            _state.update { it.copy(videoUrl = url, isLoading = false, isPlaying = true) }
                        }
                        is NetworkResult.Error -> _state.update { it.copy(isLoading = false) }
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
            is StudioPlayerIntent.SetSpeed -> _state.update { it.copy(speed = intent.speed) }
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
            is StudioPlayerIntent.UpdatePosition -> _state.update { it.copy(currentPosition = intent.position) }
            is StudioPlayerIntent.UpdateDuration -> _state.update { it.copy(duration = intent.duration) }
        }
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
