package com.anilibrix.plus.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.domain.model.CatalogQuery
import com.anilibrix.plus.domain.model.CatalogSort
import com.anilibrix.plus.domain.model.CatalogStatus
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.ReleaseType
import com.anilibrix.plus.domain.model.SeasonName
import com.anilibrix.plus.domain.repository.AnilibriaRepository
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
class CatalogViewModel @Inject constructor(
    private val anilibriaRepository: AnilibriaRepository,
    private val shikimoriRepository: com.anilibrix.plus.domain.repository.ShikimoriRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        restoreFiltersAndLoad()
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
                val query = intent.query.trim()
                _state.update { it.copy(filter = it.filter.copy(search = query)) }
                rememberSearch(query)
                persistCatalogFilter(_state.value.filter)
                loadCatalog(reset = true)
            }
            is CatalogIntent.UpdateFilter -> {
                _state.update { it.copy(filter = intent.filter) }
                persistCatalogFilter(intent.filter)
                loadCatalog(reset = true)
            }
            is CatalogIntent.LoadMore -> {
                val state = _state.value
                if (!state.hasMore || state.loadingMore || state.loading || state.refreshing) return
                loadCatalog(reset = false)
            }
            is CatalogIntent.Refresh -> {
                loadCatalog(reset = true)
            }
            is CatalogIntent.ToggleViewMode -> {
                _state.update { it.copy(filter = it.filter.copy(viewMode = intent.mode)) }
                viewModelScope.launch { settingsDataStore.setCatalogViewMode(intent.mode.name) }
            }
            is CatalogIntent.SelectSuggestion -> {
                _state.update { it.copy(filter = it.filter.copy(search = intent.suggestion)) }
                rememberSearch(intent.suggestion)
                persistCatalogFilter(_state.value.filter)
                loadCatalog(reset = true)
            }
            is CatalogIntent.ClearSearch -> {
                _state.update {
                    it.copy(
                        filter = it.filter.copy(search = ""),
                        suggestions = emptyList(),
                        characterSuggestions = emptyList(),
                    )
                }
                persistCatalogFilter(_state.value.filter)
                loadCatalog(reset = true)
            }
            CatalogIntent.ClearSearchHistory -> {
                _state.update { it.copy(searchHistory = emptyList()) }
                viewModelScope.launch { settingsDataStore.setCatalogSearchHistory(emptyList()) }
            }
        }
    }

    private fun restoreFiltersAndLoad() {
        viewModelScope.launch {
            val genres = settingsDataStore.catalogFilterGenre.first()
                ?.split("\n")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?: emptySet()
            val sortName = settingsDataStore.catalogSort.first()
            val viewModeName = settingsDataStore.catalogViewMode.first()
            val searchHistory = settingsDataStore.catalogSearchHistory.first()
            val filter = CatalogFilter(
                genres = genres,
                year = settingsDataStore.catalogFilterYear.first(),
                type = settingsDataStore.catalogFilterType.first()?.let { value ->
                    runCatching { ReleaseType.valueOf(value) }.getOrNull()
                },
                season = settingsDataStore.catalogFilterSeason.first()?.let { value ->
                    runCatching { SeasonName.valueOf(value) }.getOrNull()
                },
                status = settingsDataStore.catalogFilterStatus.first()?.let { value ->
                    runCatching { CatalogStatus.valueOf(value) }.getOrNull()
                },
                sort = runCatching {
                    CatalogSort.valueOf(sortName)
                }.getOrDefault(CatalogSort.UPDATED),
                viewMode = runCatching {
                    ViewMode.valueOf(viewModeName)
                }.getOrDefault(ViewMode.GRID)
            )
            _state.update {
                it.copy(
                    filter = filter,
                    searchHistory = searchHistory
                )
            }
            loadCatalog(reset = true)
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
            val query = CatalogQuery(
                page = page,
                limit = CATALOG_PAGE_SIZE,
                search = filter.search.ifBlank { null },
                genres = filter.genres,
                year = filter.year,
                type = filter.type,
                season = filter.season,
                status = filter.status,
                sort = filter.sort
            )

            anilibriaRepository.getCatalog(query)
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
                            val titles = result.data
                            _state.update {
                                it.copy(
                                    titles = if (reset) titles else it.titles + titles,
                                    currentPage = page,
                                    hasMore = titles.size >= CATALOG_PAGE_SIZE,
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
        // Персонажи ищутся параллельно тайтлам и намеренно не влияют на
        // suggestionsLoading: Shikimori отвечает медленнее и с троттлингом,
        // и ждать его ради основного списка незачем.
        viewModelScope.launch {
            shikimoriRepository.searchCharacters(query, limit = 4)
                .catch { _state.update { it.copy(characterSuggestions = emptyList()) } }
                .collect { result ->
                    if (result is NetworkResult.Success) {
                        _state.update { it.copy(characterSuggestions = result.data) }
                    }
                }
        }

        viewModelScope.launch {
            _state.update { it.copy(suggestionsLoading = true) }
            anilibriaRepository.getCatalog(CatalogQuery(page = 1, limit = 5, search = query))
                // Флаг снимается и при исключении: иначе панель поиска
                // застревает в состоянии загрузки навсегда.
                .catch { _state.update { it.copy(suggestionsLoading = false) } }
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            _state.update {
                                it.copy(
                                    suggestions = result.data.map { t -> t.name.main },
                                    suggestionsLoading = false,
                                )
                            }
                        }
                        is NetworkResult.Error -> {
                            _state.update { it.copy(suggestionsLoading = false) }
                        }
                        else -> {}
                    }
                }
        }
    }

    private fun rememberSearch(query: String) {
        if (query.isBlank()) return
        val history = (listOf(query) + _state.value.searchHistory.filterNot {
            it.equals(query, ignoreCase = true)
        }).take(10)
        _state.update { it.copy(searchHistory = history) }
        viewModelScope.launch { settingsDataStore.setCatalogSearchHistory(history) }
    }

    private fun persistCatalogFilter(filter: CatalogFilter) {
        viewModelScope.launch {
            settingsDataStore.setCatalogFilterGenre(filter.genres.takeIf { it.isNotEmpty() }?.joinToString("\n"))
            settingsDataStore.setCatalogFilterYear(filter.year)
            settingsDataStore.setCatalogFilterType(filter.type?.name)
            settingsDataStore.setCatalogFilterSeason(filter.season?.name)
            settingsDataStore.setCatalogFilterStatus(filter.status?.name)
            settingsDataStore.setCatalogSort(filter.sort.name)
            settingsDataStore.setCatalogViewMode(filter.viewMode.name)
        }
    }

    private companion object {
        const val CATALOG_PAGE_SIZE = 20
    }
}
