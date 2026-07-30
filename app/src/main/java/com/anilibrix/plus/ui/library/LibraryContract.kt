package com.anilibrix.plus.ui.library

import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.Playlist

data class LibraryUiState(
    val selectedTab: Int = 0,
    /**
     * Активный статус на вкладке «Мои списки».
     *
     * Пять статусов сделаны фильтром внутри одной вкладки, а не пятью
     * отдельными вкладками: восемь вкладок в строке не помещаются, а перебирать
     * их свайпом, чтобы найти один тайтл, — хуже, чем один тап по чипу.
     */
    val selectedStatus: CollectionType = CollectionType.WATCHING,
    val query: String = "",
    val sort: LibrarySort = LibrarySort.RECENT,
    val collections: Map<CollectionType, List<FavoriteItem>> = emptyMap(),
    val collectionCounts: Map<CollectionType, Int> = emptyMap(),
    val favorites: List<FavoriteItem> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val continueWatching: List<HistoryEntry> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val expandedPlaylistId: Long? = null,
    val renamingPlaylist: Playlist? = null,
    val pendingUndo: PendingLibraryUndo? = null,
    /**
     * Раньше это поле было объявлено, но НИКОГДА не выставлялось и не читалось —
     * библиотека вообще не показывала загрузку и мигала пустым состоянием,
     * пока Room отдавал первую порцию.
     */
    val isLoading: Boolean = true,
    // Отфильтрованные и отсортированные списки считает ViewModel.
    // Раньше это делалось прямо в composable, то есть заново на КАЖДУЮ
    // рекомпозицию — на больших списках заметно.
    val filteredCollection: List<FavoriteItem> = emptyList(),
    val filteredFavorites: List<FavoriteItem> = emptyList(),
    val filteredHistory: List<HistoryEntry> = emptyList(),
    val filteredPlaylists: List<Playlist> = emptyList(),
) {
    fun countOf(type: CollectionType): Int = collectionCounts[type] ?: 0
}

internal fun List<FavoriteItem>.applyLibraryFilter(
    query: String,
    sort: LibrarySort,
): List<FavoriteItem> {
    val filtered = filter { query.isBlank() || it.titleName.contains(query, ignoreCase = true) }
    return when (sort) {
        LibrarySort.RECENT -> filtered
        LibrarySort.TITLE -> filtered.sortedBy { it.titleName.lowercase() }
    }
}

internal fun List<HistoryEntry>.applyHistoryFilter(
    query: String,
    sort: LibrarySort,
): List<HistoryEntry> {
    val filtered = filter { query.isBlank() || it.titleName.contains(query, ignoreCase = true) }
    return when (sort) {
        LibrarySort.RECENT -> filtered.sortedByDescending { it.watchedAt }
        LibrarySort.TITLE -> filtered.sortedBy { it.titleName.lowercase() }
    }
}

internal fun List<Playlist>.applyPlaylistFilter(
    query: String,
    sort: LibrarySort,
): List<Playlist> {
    val filtered = filter { playlist ->
        query.isBlank() ||
            playlist.name.contains(query, ignoreCase = true) ||
            playlist.items.any { it.titleName.contains(query, ignoreCase = true) }
    }
    return when (sort) {
        LibrarySort.RECENT -> filtered.sortedByDescending { it.createdAt }
        LibrarySort.TITLE -> filtered.sortedBy { it.name.lowercase() }
    }
}

enum class LibrarySort(val displayName: String) {
    RECENT("Сначала новые"),
    TITLE("По названию")
}

data class FavoriteItem(
    val titleId: Long,
    val titleName: String,
    val posterUrl: String?,
    /** «Серия 7 из 12» под названием — виден прогресс, не открывая тайтл. */
    val progressLabel: String? = null,
    val progressFraction: Float = 0f,
)

data class PendingLibraryUndo(
    val token: Long,
    val message: String,
    val item: DeletedLibraryItem
)

sealed interface DeletedLibraryItem {
    data class Favorite(val item: FavoriteItem) : DeletedLibraryItem
    data class Collection(val item: FavoriteItem, val type: CollectionType) : DeletedLibraryItem
    data class History(val entry: HistoryEntry) : DeletedLibraryItem
}

sealed class LibraryIntent {
    data class SelectTab(val index: Int) : LibraryIntent()
    data class SelectStatus(val status: CollectionType) : LibraryIntent()
    data class UpdateQuery(val query: String) : LibraryIntent()
    data class SetSort(val sort: LibrarySort) : LibraryIntent()
    data class RemoveFavorite(val item: FavoriteItem) : LibraryIntent()
    data class RemoveFromCollection(val item: FavoriteItem) : LibraryIntent()
    data class RemoveHistory(val entry: HistoryEntry) : LibraryIntent()
    data class TogglePlaylist(val playlistId: Long) : LibraryIntent()
    data class CreatePlaylist(val name: String) : LibraryIntent()
    data class StartRenamePlaylist(val playlist: Playlist) : LibraryIntent()
    data object DismissRenamePlaylist : LibraryIntent()
    data class ConfirmRenamePlaylist(val name: String) : LibraryIntent()
    data class DeletePlaylist(val playlistId: Long) : LibraryIntent()
    data object UndoLastRemoval : LibraryIntent()
    data object DismissUndo : LibraryIntent()
    data object Refresh : LibraryIntent()
}
