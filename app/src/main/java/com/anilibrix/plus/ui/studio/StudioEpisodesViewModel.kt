package com.anilibrix.plus.ui.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.StudioEpisode
import com.anilibrix.plus.domain.repository.DecoderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudioEpisodesViewModel @Inject constructor(
    private val decoderRepository: DecoderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StudioEpisodesUiState())
    val state: StateFlow<StudioEpisodesUiState> = _state.asStateFlow()

    fun handleIntent(intent: StudioEpisodesIntent) {
        when (intent) {
            is StudioEpisodesIntent.LoadEpisodes -> loadEpisodes(intent.source, intent.id, intent.title)
        }
    }

    private fun loadEpisodes(source: String, id: String, title: String) {
        _state.update { it.copy(source = source, animeId = id, title = title, isLoading = true, error = null, episodes = emptyList()) }
        viewModelScope.launch {
            decoderRepository.getAnime(source, id)
                .catch { e ->
                    _state.update { it.copy(error = e.message ?: "Unknown error", isLoading = false) }
                }
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> _state.update { it.copy(episodes = result.data, isLoading = false) }
                        is NetworkResult.Error -> _state.update { it.copy(error = result.message, isLoading = false) }
                        is NetworkResult.Loading -> {}
                    }
                }
        }
    }
}
