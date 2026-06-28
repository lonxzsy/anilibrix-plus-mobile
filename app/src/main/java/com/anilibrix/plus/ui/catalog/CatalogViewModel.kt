package com.anilibrix.plus.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.ui.catalog.CatalogIntent
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
class CatalogViewModel @Inject constructor(
    private val anilibriaRepository: AnilibriaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadCatalog(reset = true)
    }

    fun onIntent(intent: CatalogIntent) {
        when (intent) {
            is CatalogIntent.Search -> {
                searchJob?.cancel()
                searchJob = viewModelScope.launch {
                    delay(600)
                    if (intent.query.length >= 2) {
                        loadSuggestions(intent.query)
                    }
                }
            }
            is CatalogIntent.SubmitSearch -> {
                _state.update { it.copy(filter = it.filter.copy(search = intent.query)) }
                loadCatalog(reset = true)
            }
            is CatalogIntent.UpdateFilter -> {
                _state.update { it.copy(filter = intent.filter) }
                loadCatalog(reset = true)
            }
            is CatalogIntent.LoadMore -> {
                if (!_state.value.hasMore || _state.value.loadingMore) return
                loadCatalog(reset = false)
            }
            is CatalogIntent.Refresh -> {
                loadCatalog(reset = true)
            }
            is CatalogIntent.ToggleViewMode -> {
                _state.update { it.copy(filter = it.filter.copy(viewMode = intent.mode)) }
            }
            is CatalogIntent.SelectSuggestion -> {
                _state.update { it.copy(filter = it.filter.copy(search = intent.suggestion)) }
                loadCatalog(reset = true)
            }
            is CatalogIntent.ClearSearch -> {
                _state.update { it.copy(filter = it.filter.copy(search = ""), suggestions = emptyList()) }
                loadCatalog(reset = true)
            }
        }
    }

    private fun loadCatalog(reset: Boolean) {
        viewModelScope.launch {
            val currentState = _state.value
            if (reset && currentState.titles.isEmpty()) {
                _state.update { it.copy(loading = true, error = null, hasMore = true) }
            } else if (reset) {
                _state.update { it.copy(refreshing = true, error = null, hasMore = true) }
            } else {
                _state.update { it.copy(loadingMore = true) }
            }

            val page = if (reset) 1 else currentState.currentPage + 1
            val filter = currentState.filter

            anilibriaRepository.getCatalog(
                page = page,
                limit = 20,
                search = filter.search.ifBlank { null }
            )
                .catch { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            loadingMore = false,
                            refreshing = false,
                            error = e.message ?: "Ошибка загрузки"
                        )
                    }
                }
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            val raw = result.data
                            val selectedGenres = currentState.filter.genres
                            val titles = if (selectedGenres.isEmpty()) raw else {
                                raw.filter { title ->
                                    title.genres.any { it.name in selectedGenres }
                                }
                            }
                            _state.update {
                                it.copy(
                                    titles = if (reset) titles else it.titles + titles,
                                    currentPage = page,
                                    hasMore = raw.size >= 20,
                                    loading = false,
                                    loadingMore = false,
                                    refreshing = false
                                )
                            }
                        }
                        is NetworkResult.Error -> {
                            _state.update {
                                it.copy(
                                    loading = false,
                                    loadingMore = false,
                                    refreshing = false,
                                    error = result.message
                                )
                            }
                        }
                        is NetworkResult.Loading -> {}
                    }
                }
        }
    }

    private fun loadSuggestions(query: String) {
        viewModelScope.launch {
            anilibriaRepository.getCatalog(page = 1, limit = 5, search = query)
                .catch {}
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            _state.update {
                                it.copy(suggestions = result.data.map { t -> t.name.main })
                            }
                        }
                        else -> {}
                    }
                }
        }
    }
}
