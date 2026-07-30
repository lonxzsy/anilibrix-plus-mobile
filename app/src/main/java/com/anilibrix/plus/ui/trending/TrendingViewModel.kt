package com.anilibrix.plus.ui.trending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.domain.model.CatalogQuery
import com.anilibrix.plus.domain.model.MalAnime
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.JikanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrendingViewModel @Inject constructor(
    private val jikanRepository: JikanRepository,
    private val anilibriaRepository: AnilibriaRepository,
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
            is TrendingIntent.OpenAnime -> openAnime(intent.anime)
            TrendingIntent.ClearNavigation -> _state.update { it.copy(navigation = null) }
        }
    }

    /**
     * Открывает тайтл из тренда.
     *
     * Карточки этого экрана вообще не имели обработчика нажатия: список был
     * тупиком — посмотреть, что в топе, можно, а перейти к тайтлу нельзя.
     *
     * Соответствие ищется по названию в каталоге Anilibria и запоминается:
     * повторные тапы по той же карточке не ходят в сеть заново. Если тайтла
     * в Anilibria нет, честно сообщаем об этом, а не открываем что попало.
     */
    private fun openAnime(anime: MalAnime) {
        val cached = _state.value.anilibriaMatches
        if (cached.containsKey(anime.malId)) {
            val titleId = cached[anime.malId]
            _state.update {
                it.copy(
                    navigation = if (titleId != null) {
                        TrendingNavigation.ToTitle(titleId)
                    } else {
                        TrendingNavigation.NotFound(anime)
                    }
                )
            }
            return
        }

        if (anime.malId in _state.value.resolving) return

        viewModelScope.launch {
            _state.update { it.copy(resolving = it.resolving + anime.malId) }

            val query = anime.title.trim()
            val result = anilibriaRepository
                .getCatalog(CatalogQuery(page = 1, limit = 10, search = query))
                .first { it !is NetworkResult.Loading }

            val match = (result as? NetworkResult.Success)?.data?.bestMatchFor(anime.title)

            _state.update {
                it.copy(
                    resolving = it.resolving - anime.malId,
                    anilibriaMatches = it.anilibriaMatches + (anime.malId to match?.id),
                    navigation = if (match != null) {
                        TrendingNavigation.ToTitle(match.id)
                    } else {
                        TrendingNavigation.NotFound(anime)
                    },
                )
            }
        }
    }

    /**
     * Точное совпадение по нормализованному названию, иначе — первое из выдачи.
     *
     * Первое из выдачи допустимо здесь, но не в синхронизации с трекером:
     * промах открывает не тот тайтл, и человек это сразу видит и вернётся, а
     * промах в чужом списке испортил бы данные молча.
     */
    private fun List<Title>.bestMatchFor(query: String): Title? {
        if (isEmpty()) return null
        val normalized = query.normalizeForMatch()
        return firstOrNull { title ->
            listOfNotNull(title.name.main, title.name.english, title.name.alternative)
                .any { it.normalizeForMatch() == normalized }
        } ?: first()
    }

    private fun String.normalizeForMatch(): String = lowercase().filter { it.isLetterOrDigit() }

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
