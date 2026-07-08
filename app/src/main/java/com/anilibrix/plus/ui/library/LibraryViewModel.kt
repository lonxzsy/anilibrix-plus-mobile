package com.anilibrix.plus.ui.library

import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.core.architecture.BaseViewModel
import com.anilibrix.plus.domain.repository.LocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val localRepository: LocalRepository
) : BaseViewModel<LibraryUiState, Unit>() {

    override val initialUiState: LibraryUiState = LibraryUiState()

    init {
        loadData()
    }

    fun handleIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.SelectTab -> {
                updateState { copy(selectedTab = intent.index) }
            }
            is LibraryIntent.UpdateQuery -> {
                updateState { copy(query = intent.query) }
            }
            is LibraryIntent.SetSort -> {
                updateState { copy(sort = intent.sort) }
            }
            is LibraryIntent.RemoveFavorite -> {
                viewModelScope.launch {
                    localRepository.removeFavorite(intent.item.titleId)
                    setPendingUndo(
                        message = "Удалено из избранного",
                        item = DeletedLibraryItem.Favorite(intent.item)
                    )
                    loadFavorites()
                }
            }
            is LibraryIntent.RemoveWatchLater -> {
                viewModelScope.launch {
                    localRepository.removeWatchLater(intent.item.titleId)
                    setPendingUndo(
                        message = "Удалено из «Буду смотреть»",
                        item = DeletedLibraryItem.WatchLater(intent.item)
                    )
                    loadWatchLater()
                }
            }
            is LibraryIntent.RemoveHistory -> {
                viewModelScope.launch {
                    localRepository.deleteHistory(intent.entry.titleId, intent.entry.episodeId)
                    setPendingUndo(
                        message = "Запись удалена из истории",
                        item = DeletedLibraryItem.History(intent.entry)
                    )
                    loadHistory()
                }
            }
            is LibraryIntent.TogglePlaylist -> {
                val current = uiState.value.expandedPlaylistId
                if (current == intent.playlistId) {
                    updateState { copy(expandedPlaylistId = null) }
                } else {
                    updateState { copy(expandedPlaylistId = intent.playlistId) }
                }
            }
            is LibraryIntent.CreatePlaylist -> {
                viewModelScope.launch {
                    localRepository.createPlaylist(intent.name)
                    loadPlaylists()
                }
            }
            is LibraryIntent.DeletePlaylist -> {
                viewModelScope.launch {
                    localRepository.deletePlaylist(intent.playlistId)
                    loadPlaylists()
                }
            }
            LibraryIntent.UndoLastRemoval -> undoLastRemoval()
            LibraryIntent.DismissUndo -> updateState { copy(pendingUndo = null) }
            LibraryIntent.Refresh -> loadData()
        }
    }

    private fun setPendingUndo(message: String, item: DeletedLibraryItem) {
        updateState {
            copy(
                pendingUndo = PendingLibraryUndo(
                    token = System.currentTimeMillis(),
                    message = message,
                    item = item
                )
            )
        }
    }

    private fun undoLastRemoval() {
        viewModelScope.launch {
            when (val deleted = uiState.value.pendingUndo?.item) {
                is DeletedLibraryItem.Favorite -> {
                    localRepository.addFavorite(
                        deleted.item.titleId,
                        deleted.item.titleName,
                        deleted.item.posterUrl
                    )
                    loadFavorites()
                }
                is DeletedLibraryItem.WatchLater -> {
                    localRepository.addWatchLater(
                        deleted.item.titleId,
                        deleted.item.titleName,
                        deleted.item.posterUrl
                    )
                    loadWatchLater()
                }
                is DeletedLibraryItem.History -> {
                    localRepository.addHistory(deleted.entry)
                    loadHistory()
                }
                null -> Unit
            }
            updateState { copy(pendingUndo = null) }
        }
    }

    private fun loadData() {
        loadContinueWatching()
        loadFavorites()
        loadWatchLater()
        loadHistory()
        loadPlaylists()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            localRepository.getFavorites().collectLatest { titles ->
                updateState {
                    copy(favorites = titles.map { f ->
                        FavoriteItem(
                            titleId = f.titleId,
                            titleName = f.titleName,
                            posterUrl = f.posterUrl
                        )
                    })
                }
            }
        }
    }

    private fun loadWatchLater() {
        viewModelScope.launch {
            localRepository.getWatchLater().collectLatest { items ->
                updateState {
                    copy(watchLater = items.map { f ->
                        FavoriteItem(
                            titleId = f.titleId,
                            titleName = f.titleName,
                            posterUrl = f.posterUrl
                        )
                    })
                }
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            localRepository.getHistory().collectLatest { history ->
                updateState { copy(history = history) }
            }
        }
    }

    private fun loadContinueWatching() {
        viewModelScope.launch {
            localRepository.getHistory().collectLatest { history ->
                val filtered = history
                    .filter { entry ->
                        val progress = if (entry.duration > 0) entry.timestamp.toFloat() / entry.duration.toFloat() else 0f
                        progress > 0f && progress < 0.9f
                    }
                    .sortedByDescending { it.watchedAt }
                    .take(6)
                updateState { copy(continueWatching = filtered) }
            }
        }
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            localRepository.getPlaylists().collectLatest { playlists ->
                val enriched = playlists.map { playlist ->
                    val items = localRepository.getPlaylistItems(playlist.id)
                    playlist.copy(items = items)
                }
                updateState { copy(playlists = enriched) }
            }
        }
    }
}
