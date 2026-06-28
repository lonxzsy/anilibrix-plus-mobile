package com.anilibrix.plus.ui.library

import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.core.architecture.BaseViewModel
import com.anilibrix.plus.domain.model.FavoriteTitle
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.Playlist
import com.anilibrix.plus.domain.model.PlaylistItem
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
            is LibraryIntent.RemoveFavorite -> {
                viewModelScope.launch {
                    localRepository.removeFavorite(intent.titleId)
                    loadFavorites()
                }
            }
            is LibraryIntent.RemoveWatchLater -> {
                viewModelScope.launch {
                    localRepository.removeWatchLater(intent.titleId)
                    loadWatchLater()
                }
            }
            is LibraryIntent.RemoveHistory -> {
                viewModelScope.launch {
                    localRepository.deleteHistory(intent.titleId, intent.episodeId)
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
            LibraryIntent.Refresh -> loadData()
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
