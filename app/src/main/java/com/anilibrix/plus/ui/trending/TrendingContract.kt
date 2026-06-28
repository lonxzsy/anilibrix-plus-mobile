package com.anilibrix.plus.ui.trending

import com.anilibrix.plus.domain.model.MalAnime

data class TrendingUiState(
    val items: List<MalAnime> = emptyList(),
    val sortBy: SortBy = SortBy.SCORE,
    val viewMode: ViewMode = ViewMode.GRID,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)

enum class SortBy(val displayName: String) {
    SCORE("Score"),
    POPULARITY("Popularity"),
    RANK("Rank"),
    TITLE("Title")
}

enum class ViewMode {
    GRID, LIST
}

sealed class TrendingIntent {
    data class SetSortBy(val sortBy: SortBy) : TrendingIntent()
    data object ToggleViewMode : TrendingIntent()
    data object LoadNextPage : TrendingIntent()
    data object Refresh : TrendingIntent()
}
