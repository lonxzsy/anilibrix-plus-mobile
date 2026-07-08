package com.anilibrix.plus.ui.trending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.domain.model.MalAnime
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.JikanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrendingViewModel @Inject constructor(
    private val jikanRepository: JikanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TrendingUiState())
    val state: StateFlow<TrendingUiState> = _state.asStateFlow()

    init {
        loadPage(1)
    }

    fun handleIntent(intent: TrendingIntent) {
        when (intent) {
            is TrendingIntent.SetSortBy -> {
                _state.update { it.copy(sortBy = intent.sortBy, items = emptyList(), currentPage = 1) }
                loadPage(1)
            }
            TrendingIntent.ToggleViewMode -> {
                val newMode = if (_state.value.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
                _state.update { it.copy(viewMode = newMode) }
            }
            TrendingIntent.LoadNextPage -> {
                val s = _state.value
                if (!s.isLoadingMore && s.currentPage < s.totalPages) {
                    loadPage(s.currentPage + 1, isMore = true)
                }
            }
            TrendingIntent.Refresh -> {
                _state.update { it.copy(items = emptyList(), currentPage = 1) }
                loadPage(1)
            }
        }
    }

    private fun loadPage(page: Int, isMore: Boolean = false) {
        viewModelScope.launch {
            try {
                _state.update { if (!isMore) it.copy(isLoading = true, error = null) else it.copy(isLoadingMore = true) }

                var hasError = false
                val result = jikanRepository.getTop(page)
                val data = mutableListOf<MalAnime>()
                result.collect { networkResult ->
                    when (networkResult) {
                        is NetworkResult.Success -> { data.addAll(networkResult.data) }
                        is NetworkResult.Error -> {
                            _state.update { it.copy(error = networkResult.message, isLoading = false, isLoadingMore = false) }
                            hasError = true
                        }
                        is NetworkResult.Loading -> {}
                    }
                }

                if (hasError) return@launch

                val sorted = sortItems(data, _state.value.sortBy)
                val allItems = if (isMore) _state.value.items + sorted else sorted
                _state.update {
                    it.copy(
                        items = allItems,
                        currentPage = page,
                        totalPages = 10,
                        isLoading = false,
                        isLoadingMore = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: "Неизвестная ошибка",
                        isLoading = false,
                        isLoadingMore = false
                    )
                }
            }
        }
    }

    private fun sortItems(items: List<MalAnime>, sortBy: SortBy): List<MalAnime> {
        return when (sortBy) {
            SortBy.SCORE -> items.sortedByDescending { it.score ?: 0.0 }
            SortBy.POPULARITY -> items.sortedBy { it.popularity ?: Int.MAX_VALUE }
            SortBy.RANK -> items.sortedBy { it.rank ?: Int.MAX_VALUE }
            SortBy.TITLE -> items.sortedBy { it.title }
        }
    }
}
