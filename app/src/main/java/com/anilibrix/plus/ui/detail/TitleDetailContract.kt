package com.anilibrix.plus.ui.detail

import com.anilibrix.plus.domain.model.Episode
import com.anilibrix.plus.domain.model.FranchiseItem
import com.anilibrix.plus.domain.model.MalCharacter
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.domain.model.Torrent

enum class DetailTab(val displayName: String) {
    DESCRIPTION("Описание"),
    EPISODES("Эпизоды"),
    CHARACTERS("Персонажи"),
    RELATED("Связанное"),
    TORRENTS("Торренты")
}

data class DetailUiState(
    val loading: Boolean = true,
    val title: Title? = null,
    val franchise: List<FranchiseItem> = emptyList(),
    val characters: List<MalCharacter> = emptyList(),
    val torrents: List<Torrent> = emptyList(),
    val isFavorite: Boolean = false,
    val isInWatchLater: Boolean = false,
    val userRating: Float = 0f,
    val selectedTab: DetailTab = DetailTab.DESCRIPTION,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

sealed interface DetailIntent {
    data class Load(val id: String) : DetailIntent
    data class SelectTab(val tab: DetailTab) : DetailIntent
    data class SetRating(val rating: Float) : DetailIntent
    data object ToggleFavorite : DetailIntent
    data object ToggleWatchLater : DetailIntent
    data class PlayEpisode(val episode: Episode) : DetailIntent
    data class OpenMagnet(val magnet: String) : DetailIntent
}
