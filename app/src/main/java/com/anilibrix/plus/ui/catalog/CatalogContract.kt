package com.anilibrix.plus.ui.catalog

import androidx.compose.runtime.Immutable
import com.anilibrix.plus.domain.model.CatalogSort
import com.anilibrix.plus.domain.model.CatalogStatus
import com.anilibrix.plus.domain.model.ReleaseType
import com.anilibrix.plus.domain.model.SeasonName
import com.anilibrix.plus.domain.model.ShikimoriCharacterSearchResult
import com.anilibrix.plus.domain.model.Title

enum class ViewMode { GRID, LIST }

@Immutable
data class CatalogFilter(
    val search: String = "",
    val genres: Set<String> = emptySet(),
    val year: Int? = null,
    val type: ReleaseType? = null,
    val season: SeasonName? = null,
    val status: CatalogStatus? = null,
    val sort: CatalogSort = CatalogSort.UPDATED,
    val viewMode: ViewMode = ViewMode.GRID
) {
    val hasActiveFilters: Boolean
        get() = search.isNotEmpty() ||
            genres.isNotEmpty() ||
            year != null ||
            type != null ||
            season != null ||
            status != null ||
            sort != CatalogSort.UPDATED
}

@Immutable
data class CatalogUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val titles: List<Title> = emptyList(),
    val filter: CatalogFilter = CatalogFilter(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val suggestions: List<String> = emptyList(),
    /**
     * Отдельный флаг для подсказок поиска.
     *
     * Без него панель поиска не могла отличить «ответ ещё не пришёл» от
     * «совпадений нет» и показывала скелетон бесконечно.
     */
    val suggestionsLoading: Boolean = false,
    /**
     * Персонажи, найденные по тому же запросу.
     *
     * Данные Shikimori уже были в приложении, но искать по персонажам было
     * негде: экран персонажа открывался только из карточки тайтла.
     */
    val characterSuggestions: List<ShikimoriCharacterSearchResult> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val error: String? = null,
    val loadingMore: Boolean = false
)

sealed interface CatalogIntent {
    data class Search(val query: String) : CatalogIntent
    data class SubmitSearch(val query: String) : CatalogIntent
    data class UpdateFilter(val filter: CatalogFilter) : CatalogIntent
    data object LoadMore : CatalogIntent
    data object Refresh : CatalogIntent
    data class ToggleViewMode(val mode: ViewMode) : CatalogIntent
    data class SelectSuggestion(val suggestion: String) : CatalogIntent
    data object ClearSearch : CatalogIntent
    data object ClearSearchHistory : CatalogIntent
}
