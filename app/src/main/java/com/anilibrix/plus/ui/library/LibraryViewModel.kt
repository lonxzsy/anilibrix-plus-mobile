package com.anilibrix.plus.ui.library

import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.core.architecture.BaseViewModel
import com.anilibrix.plus.core.sync.SyncOperationKind
import com.anilibrix.plus.core.sync.SyncPayload
import com.anilibrix.plus.core.sync.SyncQueue
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.EpisodeProgress
import com.anilibrix.plus.domain.repository.LocalRepository
import com.anilibrix.plus.domain.usecase.ManageCollectionsUseCase
import com.anilibrix.plus.work.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val localRepository: LocalRepository,
    private val manageCollections: ManageCollectionsUseCase,
    private val syncQueue: SyncQueue,
    private val syncScheduler: SyncScheduler,
) : BaseViewModel<LibraryUiState, Unit>() {

    override val initialUiState: LibraryUiState = LibraryUiState()

    init {
        loadData()
    }

    /**
     * Пересчитывает производные списки.
     *
     * Фильтрация и сортировка раньше вызывались прямо в composable, поэтому
     * выполнялись заново на каждую рекомпозицию. Теперь они считаются один раз
     * при изменении входных данных.
     */
    private fun LibraryUiState.withDerived(): LibraryUiState = copy(
        filteredCollection = (collections[selectedStatus] ?: emptyList())
            .applyLibraryFilter(query, sort),
        filteredFavorites = favorites.applyLibraryFilter(query, sort),
        filteredHistory = history.applyHistoryFilter(query, sort),
        filteredPlaylists = playlists.applyPlaylistFilter(query, sort),
    )

    private fun updateDerived(block: LibraryUiState.() -> LibraryUiState) {
        updateState { block().withDerived() }
    }

    fun handleIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.SelectTab -> updateState { copy(selectedTab = intent.index) }
            is LibraryIntent.SelectStatus -> updateDerived { copy(selectedStatus = intent.status) }
            is LibraryIntent.UpdateQuery -> updateDerived { copy(query = intent.query) }
            is LibraryIntent.SetSort -> updateDerived { copy(sort = intent.sort) }

            is LibraryIntent.RemoveFavorite -> {
                viewModelScope.launch {
                    localRepository.removeFavorite(intent.item.titleId)
                    syncQueue.enqueue(
                        SyncOperationKind.FAVORITE,
                        intent.item.titleId,
                        SyncPayload(inFavorites = false),
                    )
                    syncScheduler.syncNow()
                    setPendingUndo("Удалено из избранного", DeletedLibraryItem.Favorite(intent.item))
                }
            }

            is LibraryIntent.RemoveFromCollection -> {
                val status = uiState.value.selectedStatus
                viewModelScope.launch {
                    manageCollections.clearStatus(intent.item.titleId)
                    syncScheduler.syncNow()
                    setPendingUndo(
                        message = "Убрано из «${status.displayName}»",
                        item = DeletedLibraryItem.Collection(intent.item, status),
                    )
                }
            }

            is LibraryIntent.RemoveHistory -> {
                viewModelScope.launch {
                    localRepository.deleteHistory(intent.entry.titleId, intent.entry.episodeId)
                    // Ключевая часть фикса: без этого удалённая запись
                    // возвращалась при следующей синхронизации, потому что на
                    // сервере таймкод оставался нетронутым — `deleteTimecode`
                    // не вызывался ниоткуда.
                    syncQueue.enqueue(
                        kind = SyncOperationKind.TIMECODE_DELETE,
                        titleId = intent.entry.titleId,
                        payload = SyncPayload(releaseEpisodeId = intent.entry.releaseEpisodeId),
                    )
                    syncScheduler.syncNow()
                    setPendingUndo("Запись удалена из истории", DeletedLibraryItem.History(intent.entry))
                }
            }

            is LibraryIntent.TogglePlaylist -> {
                val current = uiState.value.expandedPlaylistId
                updateState {
                    copy(expandedPlaylistId = if (current == intent.playlistId) null else intent.playlistId)
                }
            }

            is LibraryIntent.CreatePlaylist -> {
                viewModelScope.launch { localRepository.createPlaylist(intent.name) }
            }

            is LibraryIntent.StartRenamePlaylist ->
                updateState { copy(renamingPlaylist = intent.playlist) }

            LibraryIntent.DismissRenamePlaylist ->
                updateState { copy(renamingPlaylist = null) }

            is LibraryIntent.ConfirmRenamePlaylist -> {
                val playlist = uiState.value.renamingPlaylist ?: return
                viewModelScope.launch {
                    // `PlaylistDao.update` существовал с самого начала, но не
                    // вызывался ниоткуда: переименовать плейлист было нельзя,
                    // только удалить и создать заново, потеряв содержимое.
                    localRepository.renamePlaylist(playlist.id, intent.name.trim())
                    updateState { copy(renamingPlaylist = null) }
                }
            }

            is LibraryIntent.DeletePlaylist -> {
                viewModelScope.launch { localRepository.deletePlaylist(intent.playlistId) }
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
                    syncQueue.enqueue(
                        SyncOperationKind.FAVORITE,
                        deleted.item.titleId,
                        SyncPayload(inFavorites = true),
                    )
                }
                is DeletedLibraryItem.Collection -> {
                    manageCollections.setStatus(
                        titleId = deleted.item.titleId,
                        collectionType = deleted.type,
                        titleName = deleted.item.titleName,
                        posterUrl = deleted.item.posterUrl,
                    )
                }
                is DeletedLibraryItem.History -> {
                    localRepository.addHistory(deleted.entry)
                    syncQueue.enqueue(
                        kind = SyncOperationKind.TIMECODE_UPDATE,
                        titleId = deleted.entry.titleId,
                        payload = SyncPayload(
                            releaseEpisodeId = deleted.entry.releaseEpisodeId,
                            positionMs = deleted.entry.timestamp,
                            durationMs = deleted.entry.duration,
                        ),
                    )
                }
                null -> Unit
            }
            syncScheduler.syncNow()
            updateState { copy(pendingUndo = null) }
        }
    }

    private fun loadData() {
        updateState { copy(isLoading = true) }
        loadCollections()
        loadFavorites()
        loadHistory()
        loadContinueWatching()
        loadPlaylists()
    }

    /**
     * Собирает все пять списков разом.
     *
     * `combine`, а не пять независимых подписок: иначе после каждого из пяти
     * потоков шла бы своя перерисовка, и при первом открытии библиотека
     * дёргалась бы пять раз подряд.
     */
    private fun loadCollections() {
        viewModelScope.launch {
            val flows = CollectionType.entries.map { type ->
                localRepository.getCollections(type)
            }
            combine(flows) { lists ->
                CollectionType.entries.mapIndexed { index, type ->
                    type to lists[index].map { FavoriteItem(it.titleId, it.titleName, it.posterUrl) }
                }.toMap()
            }.collectLatest { grouped ->
                val enriched = grouped.mapValues { (type, items) ->
                    // Прогресс показываем только там, где он осмысленен.
                    // В «Просмотрено» и «Буду смотреть» подпись «Серия 0 из 12»
                    // была бы шумом.
                    if (type == CollectionType.WATCHING || type == CollectionType.ON_HOLD) {
                        items.map { it.withProgress() }
                    } else {
                        items
                    }
                }
                updateDerived {
                    copy(
                        collections = enriched,
                        collectionCounts = enriched.mapValues { it.value.size },
                        isLoading = false,
                    )
                }
            }
        }
    }

    private suspend fun FavoriteItem.withProgress(): FavoriteItem {
        val progress = localRepository.getTitleProgressOnce(titleId, totalEpisodes = 0)
        val last = progress.lastWatched ?: return this
        val watched = progress.watchedCount
        return copy(
            progressLabel = if (watched > 0) "Просмотрено серий: $watched" else "Серия ${last.episodeNumber}",
            progressFraction = if (last.isWatched) 1f else last.fraction,
        )
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            localRepository.getFavorites().collectLatest { titles ->
                updateDerived {
                    copy(
                        isLoading = false,
                        favorites = titles.map { FavoriteItem(it.titleId, it.titleName, it.posterUrl) },
                    )
                }
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            localRepository.getHistory().collectLatest { history ->
                updateDerived { copy(history = history, isLoading = false) }
            }
        }
    }

    /**
     * «Продолжить просмотр» — последняя запись по каждому тайтлу, а не по
     * каждой серии. Раньше сюда попадали несколько серий одного тайтла подряд,
     * и рейка забивалась одним и тем же названием.
     */
    private fun loadContinueWatching() {
        viewModelScope.launch {
            localRepository.getContinueWatching().collectLatest { history ->
                val filtered = history
                    .filter { entry ->
                        val fraction = if (entry.duration > 0) {
                            entry.timestamp.toFloat() / entry.duration
                        } else {
                            0f
                        }
                        fraction > EpisodeProgress.STARTED_THRESHOLD &&
                            fraction < EpisodeProgress.WATCHED_THRESHOLD
                    }
                    .take(10)
                updateState { copy(continueWatching = filtered) }
            }
        }
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            localRepository.getPlaylists().collectLatest { playlists ->
                val enriched = playlists.map { playlist ->
                    playlist.copy(items = localRepository.getPlaylistItems(playlist.id))
                }
                updateDerived { copy(playlists = enriched) }
            }
        }
    }
}
