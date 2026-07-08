package com.anilibrix.plus.ui.home

import androidx.compose.runtime.Immutable
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.Title

@Immutable
data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val heroItems: List<Title> = emptyList(),
    val recommended: List<Title> = emptyList(),
    val continueWatching: List<HistoryEntry> = emptyList(),
    val recentUpdates: List<Title> = emptyList(),
    val currentHeroIndex: Int = 0,
    val error: String? = null
)

sealed interface HomeIntent {
    data object Load : HomeIntent
    data object Refresh : HomeIntent
    data class HeroSlideChanged(val index: Int) : HomeIntent
}
