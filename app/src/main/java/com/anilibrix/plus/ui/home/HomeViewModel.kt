package com.anilibrix.plus.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.AnilibriaRepository
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
class HomeViewModel @Inject constructor(
    private val anilibriaRepository: AnilibriaRepository,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Load -> loadData()
            HomeIntent.Refresh -> refresh()
            is HomeIntent.HeroSlideChanged -> _state.update { it.copy(currentHeroIndex = intent.index) }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            launch {
                anilibriaRepository.getRecommended(limit = 10, releaseId = null)
                    .catch { e ->
                        _state.update { it.copy(error = e.message ?: "Ошибка загрузки") }
                    }
                    .collect { result ->
                        when (result) {
                            is NetworkResult.Success -> {
                                val items = result.data
                                _state.update {
                                    it.copy(
                                        heroItems = items.take(5),
                                        recommended = items.drop(5),
                                        loading = false
                                    )
                                }
                            }
                            is NetworkResult.Error -> {
                                _state.update { it.copy(error = result.message, loading = false) }
                            }
                            is NetworkResult.Loading -> {}
                        }
                    }
            }

            launch {
                anilibriaRepository.getCatalog(page = 1, limit = 20, search = null)
                    .catch { e ->
                        _state.update { it.copy(error = e.message ?: "Ошибка загрузки") }
                    }
                    .collect { result ->
                        when (result) {
                            is NetworkResult.Success -> {
                                _state.update { it.copy(recentUpdates = result.data, loading = false) }
                            }
                            is NetworkResult.Error -> {
                                _state.update { it.copy(error = result.message, loading = false) }
                            }
                            is NetworkResult.Loading -> {}
                        }
                    }
            }

            launch {
                localRepository.getHistory().collect { history ->
                    val continueWatching = history
                        .groupBy { it.titleId }
                        .map { (_, entries) -> entries.maxByOrNull { it.watchedAt } }
                        .filterNotNull()
                        .filter { entry ->
                            val progress = if (entry.duration > 0) entry.timestamp.toFloat() / entry.duration else 0f
                            progress < 0.9f
                        }
                        .sortedByDescending { it.watchedAt }
                        .take(5)
                    _state.update { it.copy(continueWatching = continueWatching) }
                }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }

            anilibriaRepository.getRecommended(limit = 10, releaseId = null)
                .catch { e ->
                    _state.update { it.copy(refreshing = false, error = e.message) }
                }
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            val items = result.data
                            _state.update {
                                it.copy(
                                    heroItems = items.take(5),
                                    recommended = items.drop(5),
                                    refreshing = false
                                )
                            }
                        }
                        is NetworkResult.Error -> {
                            _state.update { it.copy(refreshing = false, error = result.message) }
                        }
                        is NetworkResult.Loading -> {}
                    }
                }

            anilibriaRepository.getCatalog(page = 1, limit = 20, search = null)
                .catch { e ->
                    _state.update { it.copy(refreshing = false, error = e.message) }
                }
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            _state.update { it.copy(recentUpdates = result.data, refreshing = false) }
                        }
                        is NetworkResult.Error -> {
                            _state.update { it.copy(refreshing = false, error = result.message) }
                        }
                        is NetworkResult.Loading -> {}
                    }
                }
        }
    }
}
