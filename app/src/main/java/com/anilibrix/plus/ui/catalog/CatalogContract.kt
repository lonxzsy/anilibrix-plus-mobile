package com.anilibrix.plus.ui.catalog

import com.anilibrix.plus.domain.model.ReleaseType
import com.anilibrix.plus.domain.model.SeasonName
import com.anilibrix.plus.domain.model.Title

enum class ViewMode { GRID, LIST }

data class CatalogFilter(
    val search: String = "",
    val genres: Set<String> = emptySet(),
    val year: Int? = null,
    val type: ReleaseType? = null,
    val season: SeasonName? = null,
    val status: String? = null,
    val viewMode: ViewMode = ViewMode.GRID
) {
    val hasActiveFilters: Boolean
        get() = search.isNotEmpty() || genres.isNotEmpty() || year != null || type != null || season != null || status != null
}

data class CatalogUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val titles: List<Title> = emptyList(),
    val filter: CatalogFilter = CatalogFilter(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val suggestions: List<String> = emptyList(),
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
}
