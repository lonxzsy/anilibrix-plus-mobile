package com.anilibrix.plus.ui.library

import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.Playlist

data class LibraryUiState(
    val selectedTab: Int = 0,
    val favorites: List<FavoriteItem> = emptyList(),
    val watchLater: List<FavoriteItem> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val continueWatching: List<HistoryEntry> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val expandedPlaylistId: Long? = null,
    val isLoading: Boolean = false
)

data class FavoriteItem(
    val titleId: Long,
    val titleName: String,
    val posterUrl: String?
)

sealed class LibraryIntent {
    data class SelectTab(val index: Int) : LibraryIntent()
    data class RemoveFavorite(val titleId: Long) : LibraryIntent()
    data class RemoveWatchLater(val titleId: Long) : LibraryIntent()
    data class RemoveHistory(val titleId: Long, val episodeId: Long) : LibraryIntent()
    data class TogglePlaylist(val playlistId: Long) : LibraryIntent()
    data class CreatePlaylist(val name: String) : LibraryIntent()
    data class DeletePlaylist(val playlistId: Long) : LibraryIntent()
    data object Refresh : LibraryIntent()
}
