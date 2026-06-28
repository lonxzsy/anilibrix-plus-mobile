package com.anilibrix.plus.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.core.util.Transliteration
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.domain.repository.JikanRepository
import com.anilibrix.plus.domain.repository.LocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TitleDetailViewModel @Inject constructor(
    private val anilibriaRepository: AnilibriaRepository,
    private val jikanRepository: JikanRepository,
    private val localRepository: LocalRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    fun onIntent(intent: DetailIntent) {
        when (intent) {
            is DetailIntent.Load -> loadDetail(intent.id)
            is DetailIntent.SelectTab -> _state.update { it.copy(selectedTab = intent.tab) }
            is DetailIntent.SetRating -> setRating(intent.rating)
            DetailIntent.ToggleFavorite -> toggleFavorite()
            DetailIntent.ToggleWatchLater -> toggleWatchLater()
            is DetailIntent.PlayEpisode -> {}
            is DetailIntent.OpenMagnet -> {}
        }
    }

    private fun loadDetail(idOrAlias: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            val authToken = settingsDataStore.authToken.first()
            _state.update { it.copy(isLoggedIn = authToken != null) }

            anilibriaRepository.getRelease(idOrAlias)
                .catch { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "Ошибка загрузки") }
                }
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            val title = result.data
                            _state.update { it.copy(title = title, loading = false) }

                            val isFav = localRepository.isFavorite(title.id)
                            val isWl = localRepository.isInWatchLater(title.id)
                            val rating = localRepository.getRating(title.id) ?: 0f

                            _state.update {
                                it.copy(
                                    isFavorite = isFav,
                                    isInWatchLater = isWl,
                                    userRating = rating
                                )
                            }

                            launch {
                                val searchQuery = title.name.english?.takeIf { it.isNotBlank() }
                                    ?: Transliteration.toSearchQuery(title.name.main)
                                val malId = try {
                                    title.malId?.toLong() ?: run {
                                        val searchResult = jikanRepository.search(searchQuery).first()
                                        if (searchResult is NetworkResult.Success) {
                                            searchResult.data.firstOrNull()?.malId
                                        } else null
                                    }
                                } catch (_: Exception) { null }
                                if (malId != null) {
                                    jikanRepository.getCharacters(malId)
                                        .collect { charResult ->
                                            when (charResult) {
                                                is NetworkResult.Success -> {
                                                    _state.update { it.copy(characters = charResult.data) }
                                                }
                                                is NetworkResult.Error -> {
                                                    _state.update { it.copy(characters = emptyList()) }
                                                }
                                                is NetworkResult.Loading -> {}
                                            }
                                        }
                                }
                            }

                            launch {
                                anilibriaRepository.getFranchise(title.id)
                                    .catch { e ->
                                        _state.update { it.copy(franchise = emptyList()) }
                                    }
                                    .collect { franResult ->
                                        if (franResult is NetworkResult.Success) {
                                            _state.update { it.copy(franchise = franResult.data) }
                                        }
                                    }
                            }

                            launch {
                                anilibriaRepository.getTorrents(title.id)
                                    .catch { e ->
                                        _state.update { it.copy(torrents = emptyList()) }
                                    }
                                    .collect { torrResult ->
                                        if (torrResult is NetworkResult.Success) {
                                            _state.update { it.copy(torrents = torrResult.data) }
                                        }
                                    }
                            }
                        }
                        is NetworkResult.Error -> {
                            _state.update { it.copy(loading = false, error = result.message) }
                        }
                        is NetworkResult.Loading -> {}
                    }
                }
        }
    }

    private fun setRating(rating: Float) {
        viewModelScope.launch {
            val title = _state.value.title ?: return@launch
            localRepository.setRating(title.id, rating)
            _state.update { it.copy(userRating = rating) }
        }
    }

    private fun toggleFavorite() {
        viewModelScope.launch {
            val title = _state.value.title ?: return@launch
            val current = _state.value.isFavorite
            if (current) {
                localRepository.removeFavorite(title.id)
                _state.update { it.copy(isFavorite = false) }
            } else {
                localRepository.addFavorite(
                    title.id,
                    title.name.main,
                    title.poster?.medium ?: title.poster?.small
                )
                _state.update { it.copy(isFavorite = true) }
            }
        }
    }

    private fun toggleWatchLater() {
        viewModelScope.launch {
            val title = _state.value.title ?: return@launch
            val current = _state.value.isInWatchLater
            if (current) {
                localRepository.removeWatchLater(title.id)
                _state.update { it.copy(isInWatchLater = false) }
            } else {
                localRepository.addWatchLater(
                    title.id,
                    titleName = title.name.main,
                    posterUrl = title.poster?.medium ?: title.poster?.small
                )
                _state.update { it.copy(isInWatchLater = true) }
            }
        }
    }
}
