package com.anilibrix.plus.ui.trending

import com.anilibrix.plus.domain.model.MalAnime

/** Куда экран должен перейти после разрешения соответствия. */
sealed interface TrendingNavigation {
    data class ToTitle(val titleId: Long) : TrendingNavigation
    data class NotFound(val anime: MalAnime) : TrendingNavigation
}

data class TrendingUiState(
    val items: List<MalAnime> = emptyList(),
    /**
     * Сопоставление MAL → Anilibria для уже проверенных тайтлов.
     *
     * `null` в значении означает «искали, в Anilibria нет» — это не то же
     * самое, что «ещё не искали», и карточка должна показывать разное.
     */
    val anilibriaMatches: Map<Long, Long?> = emptyMap(),
    /** MAL-идентификаторы, по которым сейчас идёт поиск. */
    val resolving: Set<Long> = emptySet(),
    val sortBy: SortBy = SortBy.SCORE,
    val viewMode: ViewMode = ViewMode.GRID,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val navigation: TrendingNavigation? = null,
)

enum class SortBy(val displayName: String) {
    SCORE("Оценка"),
    POPULARITY("Популярность"),
    RANK("Ранг"),
    TITLE("Название")
}

enum class ViewMode {
    GRID, LIST
}

sealed class TrendingIntent {
    data class SetSortBy(val sortBy: SortBy) : TrendingIntent()
    data object ToggleViewMode : TrendingIntent()
    data object LoadNextPage : TrendingIntent()
    data object Refresh : TrendingIntent()
    /** Тап по карточке: найти тайтл в Anilibria и открыть его. */
    data class OpenAnime(val anime: MalAnime) : TrendingIntent()
    data object ClearNavigation : TrendingIntent()
}
