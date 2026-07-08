package com.anilibrix.plus.ui.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.StudioResult
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
class StudioSearchViewModel @Inject constructor(
    private val decoderRepository: DecoderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        StudioSearchUiState(selectedSources = DEFAULT_SOURCES.toSet())
    )
    val state: StateFlow<StudioSearchUiState> = _state.asStateFlow()

    fun handleIntent(intent: StudioSearchIntent) {
        when (intent) {
            is StudioSearchIntent.UpdateQuery -> _state.update { it.copy(query = intent.query) }
            is StudioSearchIntent.ToggleSource -> toggleSource(intent.source)
            StudioSearchIntent.Search -> search()
        }
    }

    private fun toggleSource(source: String) {
        val current = _state.value.selectedSources.toMutableSet()
        if (current.contains(source)) current.remove(source)
        else current.add(source)
        _state.update { it.copy(selectedSources = current) }
        if (_state.value.query.isNotBlank()) {
            search()
        }
    }

    private fun search() {
        val currentState = _state.value
        when {
            currentState.query.isBlank() -> {
                _state.update {
                    it.copy(
                        error = "Введите запрос для поиска",
                        hasSearched = true,
                        results = emptyMap()
                    )
                }
                return
            }
            currentState.selectedSources.isEmpty() -> {
                _state.update {
                    it.copy(
                        error = "Выберите хотя бы один источник",
                        hasSearched = true,
                        results = emptyMap()
                    )
                }
                return
            }
        }

        _state.update {
            it.copy(
                isLoading = true,
                error = null,
                results = emptyMap(),
                hasSearched = true
            )
        }

        val results = mutableMapOf<String, List<StudioResult>>()
        val errors = mutableListOf<String>()
        var completed = 0
        val total = currentState.selectedSources.size

        currentState.selectedSources.forEach { source ->
            viewModelScope.launch {
                decoderRepository.search(source, currentState.query)
                    .catch { e ->
                        synchronized(this@StudioSearchViewModel) {
                            completed++
                            results[source] = emptyList()
                            errors += "${source}: ${e.message ?: "ошибка поиска"}"
                            if (completed >= total) {
                                val hasAnyResult = results.values.any { it.isNotEmpty() }
                                _state.update {
                                    it.copy(
                                        results = results.toMap(),
                                        isLoading = false,
                                        error = if (!hasAnyResult && errors.isNotEmpty()) {
                                            "Не удалось выполнить поиск: ${errors.joinToString("; ")}"
                                        } else {
                                            null
                                        }
                                    )
                                }
                            }
                        }
                    }
                    .collect { networkResult ->
                        when (networkResult) {
                            is NetworkResult.Success -> {
                                synchronized(this@StudioSearchViewModel) {
                                    results[source] = networkResult.data
                                    completed++
                                    if (completed >= total) {
                                        val hasAnyResult = results.values.any { it.isNotEmpty() }
                                        _state.update {
                                            it.copy(
                                                results = results.toMap(),
                                                isLoading = false,
                                                error = if (!hasAnyResult && errors.isNotEmpty()) {
                                                    "Не удалось выполнить поиск: ${errors.joinToString("; ")}"
                                                } else {
                                                    null
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            is NetworkResult.Error -> {
                                synchronized(this@StudioSearchViewModel) {
                                    completed++
                                    results[source] = emptyList()
                                    errors += "${source}: ${networkResult.message}"
                                    if (completed >= total) {
                                        val hasAnyResult = results.values.any { it.isNotEmpty() }
                                        _state.update {
                                            it.copy(
                                                results = results.toMap(),
                                                isLoading = false,
                                                error = if (!hasAnyResult && errors.isNotEmpty()) {
                                                    "Не удалось выполнить поиск: ${errors.joinToString("; ")}"
                                                } else {
                                                    null
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            is NetworkResult.Loading -> {}
                        }
                    }
            }
        }
    }

    private companion object {
        val DEFAULT_SOURCES = listOf("anilibria", "animevost", "anilib", "yummyanime", "dreamcast")
    }
}
