package com.anilibrix.plus.ui.library

import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.Playlist

data class LibraryUiState(
    val selectedTab: Int = 0,
    val query: String = "",
    val sort: LibrarySort = LibrarySort.RECENT,
    val favorites: List<FavoriteItem> = emptyList(),
    val watchLater: List<FavoriteItem> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val continueWatching: List<HistoryEntry> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val expandedPlaylistId: Long? = null,
    val pendingUndo: PendingLibraryUndo? = null,
    val isLoading: Boolean = false
)

enum class LibrarySort(val displayName: String) {
    RECENT("Сначала новые"),
    TITLE("По названию")
}

data class FavoriteItem(
    val titleId: Long,
    val titleName: String,
    val posterUrl: String?
)

data class PendingLibraryUndo(
    val token: Long,
    val message: String,
    val item: DeletedLibraryItem
)

sealed interface DeletedLibraryItem {
    data class Favorite(val item: FavoriteItem) : DeletedLibraryItem
    data class WatchLater(val item: FavoriteItem) : DeletedLibraryItem
    data class History(val entry: HistoryEntry) : DeletedLibraryItem
}

sealed class LibraryIntent {
    data class SelectTab(val index: Int) : LibraryIntent()
    data class UpdateQuery(val query: String) : LibraryIntent()
    data class SetSort(val sort: LibrarySort) : LibraryIntent()
    data class RemoveFavorite(val item: FavoriteItem) : LibraryIntent()
    data class RemoveWatchLater(val item: FavoriteItem) : LibraryIntent()
    data class RemoveHistory(val entry: HistoryEntry) : LibraryIntent()
    data class TogglePlaylist(val playlistId: Long) : LibraryIntent()
    data class CreatePlaylist(val name: String) : LibraryIntent()
    data class DeletePlaylist(val playlistId: Long) : LibraryIntent()
    data object UndoLastRemoval : LibraryIntent()
    data object DismissUndo : LibraryIntent()
    data object Refresh : LibraryIntent()
}
