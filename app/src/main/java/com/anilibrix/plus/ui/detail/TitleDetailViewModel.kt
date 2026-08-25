package com.anilibrix.plus.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.core.download.DownloadRepository
import com.anilibrix.plus.core.sync.SyncOperationKind
import com.anilibrix.plus.core.sync.SyncPayload
import com.anilibrix.plus.core.sync.SyncQueue
import com.anilibrix.plus.core.util.Transliteration
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.Episode
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.MalAnime
import com.anilibrix.plus.domain.model.MalCharacter
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.ShikimoriAnime
import com.anilibrix.plus.domain.model.ShikimoriCharacter
import com.anilibrix.plus.domain.model.ShikimoriRelated
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.domain.model.Torrent
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.domain.repository.JikanRepository
import com.anilibrix.plus.domain.repository.LocalRepository
import com.anilibrix.plus.domain.repository.ShikimoriRepository
import com.anilibrix.plus.domain.usecase.ManageCollectionsUseCase
import com.anilibrix.plus.work.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.Locale
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class TitleDetailViewModel @Inject constructor(
    private val anilibriaRepository: AnilibriaRepository,
    private val jikanRepository: JikanRepository,
    private val shikimoriRepository: ShikimoriRepository,
    private val localRepository: LocalRepository,
    private val downloadRepository: DownloadRepository,
    private val manageCollections: ManageCollectionsUseCase,
    private val syncQueue: SyncQueue,
    private val syncScheduler: SyncScheduler,
    private val settingsDataStore: SettingsDataStore,
    private val kodikRepository: com.anilibrix.plus.domain.repository.KodikRepository,
    private val consumetRepository: com.anilibrix.plus.domain.repository.ConsumetRepository,
    private val nyaaRepository: com.anilibrix.plus.domain.repository.NyaaRepository,
    private val torrentDownloadManager: com.anilibrix.plus.core.torrent.TorrentDownloadManager
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            torrentDownloadManager.activeTasks.collect { allTasks ->
                val currentTitleId = _state.value.title?.id
                if (currentTitleId != null) {
                    val matching = allTasks.filter { it.titleId == currentTitleId }
                    _state.update { it.copy(activeTorrentTasks = matching) }
                }
            }
        }
    }

    fun onIntent(intent: DetailIntent) {
        when (intent) {
            is DetailIntent.Load -> loadDetail(intent.id)
            is DetailIntent.SelectTab -> _state.update { it.copy(selectedTab = intent.tab) }
            is DetailIntent.SetRating -> setRating(intent.rating)
            DetailIntent.ToggleFavorite -> toggleFavorite()
            DetailIntent.ToggleWatchLater -> toggleWatchLater()
            DetailIntent.ShowStatusSheet -> _state.update { it.copy(showStatusSheet = true) }
            DetailIntent.DismissStatusSheet -> _state.update { it.copy(showStatusSheet = false) }
            is DetailIntent.SetCollectionStatus -> setCollectionStatus(intent.status)
            DetailIntent.ClearCollectionStatus -> clearCollectionStatus()
            is DetailIntent.ToggleEpisodeWatched -> toggleEpisodeWatched(intent.episode)
            is DetailIntent.MarkWatchedUpTo -> markWatchedUpTo(intent.episode)
            is DetailIntent.DownloadEpisode -> downloadEpisode(intent.episode)
            is DetailIntent.CancelDownload -> cancelDownload(intent.episode)
            is DetailIntent.DownloadNext -> downloadNext(intent.count)
            DetailIntent.ShowPlaylistDialog -> _state.update { it.copy(showPlaylistDialog = true) }
            DetailIntent.DismissPlaylistDialog -> _state.update { it.copy(showPlaylistDialog = false) }
            is DetailIntent.TogglePlaylistMembership -> togglePlaylistMembership(intent.playlistId)
            is DetailIntent.PlayEpisode -> {}
            is DetailIntent.OpenMagnet -> {}
            is DetailIntent.OpenScreenshot -> _state.update { it.copy(fullscreenScreenshot = intent.index) }
            DetailIntent.CloseScreenshot -> _state.update { it.copy(fullscreenScreenshot = null) }
            DetailIntent.ShowVoiceoverSheet -> _state.update { it.copy(showVoiceoverSheet = true) }
            DetailIntent.DismissVoiceoverSheet -> _state.update { it.copy(showVoiceoverSheet = false) }
            is DetailIntent.SelectVoiceover -> selectVoiceover(intent.option, intent.rememberForTitle)
            is DetailIntent.SelectTorrentSource -> selectTorrentSource(intent.source)
            is DetailIntent.SetTorrentSearchQuery -> _state.update { it.copy(torrentSearchQuery = intent.query) }
            is DetailIntent.SetTorrentEpisodeFilter -> _state.update { it.copy(selectedTorrentEpisodeFilter = intent.episodeNumber) }
            is DetailIntent.SetTorrentQualityFilter -> _state.update { it.copy(selectedTorrentQualityFilter = intent.quality) }
            is DetailIntent.ClickTorrent -> clickTorrent(intent.torrent)
            DetailIntent.DismissTorrentDialog -> _state.update { it.copy(selectedTorrentForDownload = null, torrentResolvedMetadata = null, torrentMetadataLoading = false) }
            is DetailIntent.StartTorrentDownload -> startTorrentDownload(intent.torrent, intent.selectedIndices)
        }
    }

    private fun loadDetail(idOrAlias: String) {
        viewModelScope.launch {
            _state.value = DetailUiState(loading = true)
            addDebug("Load title: $idOrAlias")

            val authToken = settingsDataStore.authToken.first()
            _state.update { it.copy(isLoggedIn = authToken != null) }

            anilibriaRepository.getRelease(idOrAlias)
                .catch { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "Ошибка загрузки") }
                }
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            val title = result.data
                            _state.update { it.copy(title = title, loading = false) }
                            addDebug("Anilibria title loaded: id=${title.id}, malId=${title.malId ?: "null"}, name=${title.name.main}")

                            loadLocalState(title)
                            loadAnilibriaRelatedData(title)
                            loadExternalData(title)
                        }
                        is NetworkResult.Error -> {
                            _state.update { it.copy(loading = false, error = result.message) }
                        }
                        is NetworkResult.Loading -> {}
                    }
                }
        }
    }

    private fun loadLocalState(title: Title) {
        viewModelScope.launch {
            val isFav = localRepository.isFavorite(title.id)
            val rating = localRepository.getRating(title.id) ?: 0f
            val playlists = localRepository.getPlaylists().first()
            val playlistIds = playlists
                .filter { playlist -> playlist.items.any { it.titleId == title.id } }
                .map { it.id }
                .toSet()

            _state.update {
                it.copy(
                    isFavorite = isFav,
                    userRating = rating,
                    playlists = playlists,
                    playlistIdsForTitle = playlistIds
                )
            }
        }

        // Статус и прогресс — подпиской, а не разовым чтением: они меняются
        // и снаружи экрана (после просмотра серии, после синхронизации), и
        // разъезжались бы с тем, что видит пользователь.
        viewModelScope.launch {
            manageCollections.observeStatus(title.id).collect { status ->
                _state.update {
                    it.copy(
                        collectionStatus = status,
                        isInWatchLater = status == CollectionType.WATCH_LATER,
                    )
                }
            }
        }

        viewModelScope.launch {
            downloadRepository.observeForTitle(title.id).collect { downloads ->
                _state.update { it.copy(downloads = downloads) }
            }
        }

        viewModelScope.launch {
            localRepository.getTitleProgress(title.id, title.episodes.orEmpty().size).collect { progress ->
                _state.update {
                    it.copy(
                        progress = progress,
                        resumeTarget = progress.resumeTarget(title.episodes.orEmpty()),
                    )
                }
            }
        }
    }

    private fun setCollectionStatus(status: CollectionType) {
        viewModelScope.launch {
            val title = _state.value.title ?: return@launch
            manageCollections.setStatus(
                titleId = title.id,
                collectionType = status,
                titleName = title.name.main,
                posterUrl = title.poster?.cardUrl,
            )
            syncScheduler.syncNow()
            _state.update { it.copy(showStatusSheet = false) }
        }
    }

    private fun clearCollectionStatus() {
        viewModelScope.launch {
            val title = _state.value.title ?: return@launch
            manageCollections.clearStatus(title.id)
            syncScheduler.syncNow()
            _state.update { it.copy(showStatusSheet = false) }
        }
    }

    /**
     * Отметка серии просмотренной без её открытия.
     *
     * Повторное нажатие снимает отметку — иначе случайно поставленную было бы
     * не убрать.
     */
    private fun toggleEpisodeWatched(episode: Episode) {
        viewModelScope.launch {
            val title = _state.value.title ?: return@launch
            val current = _state.value.progress.progressOf(episode.id)

            if (current?.isWatched == true) {
                localRepository.deleteHistory(title.id, episode.id)
                syncQueue.enqueue(
                    kind = SyncOperationKind.TIMECODE_DELETE,
                    titleId = title.id,
                    payload = SyncPayload(releaseEpisodeId = episode.releaseEpisodeId),
                )
            } else {
                val entry = title.historyEntryFor(episode)
                localRepository.markEpisodeWatched(entry)
                syncQueue.enqueue(
                    kind = SyncOperationKind.TIMECODE_UPDATE,
                    titleId = title.id,
                    payload = SyncPayload(
                        releaseEpisodeId = episode.releaseEpisodeId,
                        positionMs = entry.duration,
                        durationMs = entry.duration,
                    ),
                )
                enqueueTrackerProgress(title.id)
            }
            syncScheduler.syncNow()
        }
    }

    /**
     * «Отметить все до этой» — то, чего обычно и не хватает: человек посмотрел
     * половину сезона в другом месте и не хочет тыкать в каждую серию.
     */
    private fun markWatchedUpTo(episode: Episode) {
        viewModelScope.launch {
            val title = _state.value.title ?: return@launch
            val entries = title.episodes.orEmpty()
                .filter { it.ordinal <= episode.ordinal }
                .map { title.historyEntryFor(it) }
            if (entries.isEmpty()) return@launch

            localRepository.markEpisodesWatchedUpTo(entries)
            entries.forEach { entry ->
                syncQueue.enqueue(
                    kind = SyncOperationKind.TIMECODE_UPDATE,
                    titleId = title.id,
                    payload = SyncPayload(
                        releaseEpisodeId = entry.releaseEpisodeId,
                        positionMs = entry.duration,
                        durationMs = entry.duration,
                    ),
                )
            }
            enqueueTrackerProgress(title.id)
            syncScheduler.syncNow()
        }
    }

    /** Доносит новый счётчик серий до внешнего трекера. */
    private suspend fun enqueueTrackerProgress(titleId: Long) {
        val status = manageCollections.getStatus(titleId) ?: return
        syncQueue.enqueue(
            kind = SyncOperationKind.SHIKIMORI_RATE,
            titleId = titleId,
            payload = SyncPayload(status = status.value),
        )
    }

    private fun downloadEpisode(episode: Episode) {
        viewModelScope.launch {
            val title = _state.value.title ?: return@launch
            downloadRepository.enqueue(title, episode)
        }
    }

    private fun cancelDownload(episode: Episode) {
        val title = _state.value.title ?: return
        downloadRepository.remove(DownloadRepository.requestId(title.id, episode.id))
    }

    /**
     * Скачать следующие непросмотренные серии.
     *
     * Считаем именно от прогресса, а не «первые N с начала»: человек,
     * досмотревший до седьмой серии, хочет на дорогу восьмую и дальше.
     */
    private fun downloadNext(count: Int) {
        viewModelScope.launch {
            val title = _state.value.title ?: return@launch
            val progress = _state.value.progress
            val already = _state.value.downloads.keys

            val next = title.episodes.orEmpty()
                .filter { progress.progressOf(it.id)?.isWatched != true }
                .filter { it.id !in already }
                .take(count)

            downloadRepository.enqueueAll(title, next)
        }
    }

    private fun Title.historyEntryFor(episode: Episode) = HistoryEntry(
        titleId = id,
        titleName = name.main,
        posterUrl = poster?.cardUrl,
        episodeId = episode.id,
        episodeNumber = episode.ordinal,
        // Длительность приходит в секундах; ноль означает «сервер её не знает».
        // Подставляем условные 24 минуты, иначе доля просмотра всегда была бы
        // нулевой и отметка не считалась бы.
        timestamp = episode.durationMs(),
        duration = episode.durationMs(),
        watchedAt = System.currentTimeMillis(),
        releaseEpisodeId = episode.releaseEpisodeId,
    )

    private fun Episode.durationMs(): Long =
        if (duration > 0) duration * 1000L else DEFAULT_EPISODE_DURATION_MS

    private companion object {
        /**
         * Запасная длительность серии, когда сервер её не отдал.
         *
         * Без неё доля просмотра всегда получалась нулевой, и отметка
         * «просмотрено» не срабатывала бы вовсе.
         */
        const val DEFAULT_EPISODE_DURATION_MS = 24 * 60 * 1000L
    }

    private fun loadAnilibriaRelatedData(title: Title) {
        viewModelScope.launch {
            anilibriaRepository.getFranchise(title.id)
                .catch { _ -> _state.update { it.copy(franchise = emptyList()) } }
                .collect { franResult ->
                    if (franResult is NetworkResult.Success) {
                        _state.update { it.copy(franchise = franResult.data) }
                    }
                }
        }

        viewModelScope.launch {
            anilibriaRepository.getTorrents(title.id)
                .catch { _ -> _state.update { it.copy(torrents = emptyList()) } }
                .collect { torrResult ->
                    if (torrResult is NetworkResult.Success) {
                        _state.update { it.copy(torrents = torrResult.data) }
                    }
                }
        }

        viewModelScope.launch {
            anilibriaRepository.getRecommended(limit = 12, releaseId = title.id)
                .catch { _ -> _state.update { it.copy(recommendedTitles = emptyList()) } }
                .collect { recResult ->
                    if (recResult is NetworkResult.Success) {
                        _state.update { it.copy(recommendedTitles = recResult.data) }
                    }
                }
        }
    }

    private fun loadExternalData(title: Title) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    externalLoading = true,
                    charactersLoading = true,
                    statisticsLoading = true,
                    relatedLoading = true,
                    externalErrors = emptyMap()
                )
            }

            supervisorScope {
                addDebug("External loading started")
                val malIdDeferred = async { resolveMalId(title) }
                val shikimoriIdDeferred = async { resolveShikimoriId(title) }

                val malId = malIdDeferred.await()
                _state.update { it.copy(malId = malId) }
                addDebug("Resolved MAL ID: ${malId ?: "null"}")

                launch { loadCharacters(malId) { shikimoriIdDeferred.await() } }
                launch { loadJikanData(malId) }
                launch {
                    val shikimoriId = shikimoriIdDeferred.await()
                    _state.update { it.copy(shikimoriId = shikimoriId) }
                    addDebug("Resolved Shikimori ID: ${shikimoriId ?: "null"}")
                    loadShikimoriData(shikimoriId)
                    loadShikimoriRelated(title, shikimoriId)
                    loadScreenshots(shikimoriId)
                    loadVoiceovers(title, shikimoriId, malId)
                }
                launch { loadNyaaTorrents(title) }
            }
        }
    }

    /**
     * Кадры из аниме.
     *
     * Эндпоинт `animes/{id}/screenshots` был объявлен, но у репозитория не
     * было метода — вызвать его было неоткуда, и кадры не показывались нигде.
     */
    private suspend fun loadScreenshots(shikimoriId: Int?) {
        if (shikimoriId == null) return
        when (val result = shikimoriRepository.getScreenshots(shikimoriId).awaitResult()) {
            is NetworkResult.Success -> _state.update { it.copy(screenshots = result.data.take(20)) }
            // Кадры — украшение, а не содержание: ошибку показывать незачем,
            // просто не рисуем ленту.
            else -> Unit
        }
    }

    private suspend fun resolveMalId(title: Title): Long? {
        title.malId?.toLong()?.let {
            addDebug("MAL ID from Anilibria: $it")
            return it
        }

        val searchQuery = title.name.english?.takeIf { it.isNotBlank() }
            ?: Transliteration.toSearchQuery(title.name.main)
        addDebug("Search MAL by query: $searchQuery")

        return when (val result = jikanRepository.search(searchQuery).awaitResult()) {
            is NetworkResult.Success -> {
                addDebug("MAL search results: ${result.data.size}")
                result.data.firstOrNull()?.malId
            }
            is NetworkResult.Error -> {
                addDebug("MAL search error: ${result.message}")
                recordExternalError(DetailDataSource.JIKAN, result.message)
                null
            }
            is NetworkResult.Loading -> null
        }
    }

    private suspend fun resolveShikimoriId(title: Title): Int? {
        val queries = listOfNotNull(
            title.name.main.takeIf { it.isNotBlank() },
            title.name.english?.takeIf { it.isNotBlank() },
            Transliteration.toSearchQuery(title.name.main).takeIf { it.isNotBlank() }
        ).distinct()

        addDebug("Search Shikimori queries: ${queries.joinToString(" | ")}")
        for (query in queries) {
            when (val result = shikimoriRepository.search(query).awaitResult()) {
                is NetworkResult.Success -> {
                    addDebug("Shikimori search '$query': ${result.data.size} results")
                    val match = findBestShikimoriMatch(title, result.data)
                    if (match != null) return match.id
                }
                is NetworkResult.Error -> {
                    addDebug("Shikimori search '$query' error: ${result.message}")
                    recordExternalError(DetailDataSource.SHIKIMORI, result.message)
                }
                is NetworkResult.Loading -> {}
            }
        }

        addDebug("Shikimori ID not found")
        return null
    }

    private suspend fun loadCharacters(
        malId: Long?,
        resolveFallbackShikimoriId: suspend () -> Int?
    ) {
        try {
            if (malId != null) {
                addDebug("Load Jikan characters for MAL ID: $malId")
                when (val result = jikanRepository.getCharacters(malId).awaitResult()) {
                    is NetworkResult.Success -> {
                        addDebug("Jikan characters loaded: ${result.data.size}")
                        if (result.data.isNotEmpty()) {
                            _state.update {
                                it.copy(
                                    characters = result.data,
                                    characterItems = result.data.map { character -> character.toDetailItem() },
                                    charactersLoading = false
                                )
                            }
                            return
                        }
                    }
                    is NetworkResult.Error -> {
                        addDebug("Jikan characters error: ${result.message}")
                        recordExternalError(DetailDataSource.JIKAN, result.message)
                    }
                    is NetworkResult.Loading -> {}
                }
            } else {
                addDebug("Skip Jikan characters: MAL ID is null")
            }

            val shikimoriId = resolveFallbackShikimoriId()
            if (shikimoriId != null) {
                addDebug("Load Shikimori characters for ID: $shikimoriId")
                when (val result = shikimoriRepository.getCharacters(shikimoriId).awaitResult()) {
                    is NetworkResult.Success -> {
                        addDebug("Shikimori characters loaded: ${result.data.size}")
                        _state.update {
                            it.copy(
                                characterItems = result.data.map { character -> character.toDetailItem() },
                                charactersLoading = false
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        addDebug("Shikimori characters error: ${result.message}")
                        recordExternalError(DetailDataSource.SHIKIMORI, result.message)
                        _state.update { it.copy(charactersLoading = false) }
                    }
                    is NetworkResult.Loading -> {}
                }
            } else {
                addDebug("Skip Shikimori characters: Shikimori ID is null")
                _state.update { it.copy(charactersLoading = false) }
            }
        } finally {
            _state.update { it.copy(charactersLoading = false) }
        }
    }

    private suspend fun loadJikanData(malId: Long?) {
        if (malId == null) {
            _state.update { it.copy(statisticsLoading = false, externalLoading = false) }
            return
        }

        when (val detailResult = jikanRepository.getDetail(malId).awaitResult()) {
            is NetworkResult.Success -> _state.update { it.copy(malDetails = detailResult.data) }
            is NetworkResult.Error -> recordExternalError(DetailDataSource.JIKAN, detailResult.message)
            is NetworkResult.Loading -> {}
        }

        when (val statsResult = jikanRepository.getStatistics(malId).awaitResult()) {
            is NetworkResult.Success -> _state.update { it.copy(statistics = statsResult.data) }
            is NetworkResult.Error -> recordExternalError(DetailDataSource.JIKAN, statsResult.message)
            is NetworkResult.Loading -> {}
        }

        when (val recResult = jikanRepository.getRecommendations(malId).awaitResult()) {
            is NetworkResult.Success -> _state.update { it.copy(malRecommendations = recResult.data) }
            is NetworkResult.Error -> recordExternalError(DetailDataSource.JIKAN, recResult.message)
            is NetworkResult.Loading -> {}
        }

        _state.update { it.copy(statisticsLoading = false, externalLoading = false) }
    }

    private suspend fun loadShikimoriData(shikimoriId: Int?) {
        if (shikimoriId == null) {
            _state.update { it.copy(statisticsLoading = false) }
            return
        }

        when (val result = shikimoriRepository.getAnime(shikimoriId).awaitResult()) {
            is NetworkResult.Success -> {
                _state.update {
                    it.copy(
                        shikimoriDetails = result.data,
                        statisticsLoading = false
                    )
                }
            }
            is NetworkResult.Error -> {
                recordExternalError(DetailDataSource.SHIKIMORI, result.message)
                _state.update { it.copy(statisticsLoading = false) }
            }
            is NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadShikimoriRelated(title: Title, shikimoriId: Int?) {
        if (shikimoriId == null) {
            _state.update { it.copy(relatedLoading = false, externalLoading = false) }
            return
        }

        when (val result = shikimoriRepository.getRelated(shikimoriId).awaitResult()) {
            is NetworkResult.Success -> {
                val franchiseIds = _state.value.franchise.map { it.id }.toSet()
                val related = result.data
                    .take(8)
                    .map { related -> related.toRelatedItem(matchAnilibriaRelated(related, franchiseIds)) }
                    .distinctBy { it.anilibriaId?.toString() ?: it.id }

                _state.update {
                    it.copy(
                        relatedItems = related,
                        relatedLoading = false,
                        externalLoading = false
                    )
                }
            }
            is NetworkResult.Error -> {
                recordExternalError(DetailDataSource.SHIKIMORI, result.message)
                _state.update { it.copy(relatedLoading = false, externalLoading = false) }
            }
            is NetworkResult.Loading -> {}
        }
    }

    private suspend fun matchAnilibriaRelated(
        related: ShikimoriRelated,
        franchiseIds: Set<Long>
    ): Title? {
        val query = related.russian?.takeIf { it.isNotBlank() } ?: related.name
        val result = anilibriaRepository.getCatalog(page = 1, limit = 5, search = query).awaitResult()
        if (result !is NetworkResult.Success) return null

        val relatedNames = listOfNotNull(related.russian, related.name).map { it.normalizedForMatch() }
        return result.data.firstOrNull { candidate ->
            candidate.id !in franchiseIds && candidate.matchNames().any { it in relatedNames }
        } ?: result.data.firstOrNull { it.id !in franchiseIds }
    }

    private fun findBestShikimoriMatch(title: Title, candidates: List<ShikimoriAnime>): ShikimoriAnime? {
        val titleNames = title.matchNames().toSet()
        return candidates.firstOrNull { candidate ->
            listOfNotNull(candidate.name, candidate.russian)
                .map { it.normalizedForMatch() }
                .any { it in titleNames }
        } ?: candidates.firstOrNull()
    }

    private fun MalCharacter.toDetailItem(): DetailCharacterItem = DetailCharacterItem(
        id = malId.toString(),
        name = name,
        role = role,
        imageUrl = imageUrl,
        seiyuuName = seiyuu?.name,
        seiyuuImageUrl = seiyuu?.imageUrl,
        source = DetailDataSource.JIKAN,
        malId = malId
    )

    private fun ShikimoriCharacter.toDetailItem(): DetailCharacterItem {
        val firstSeiyu = seiyus.firstOrNull()
        return DetailCharacterItem(
            id = id.toString(),
            name = russian?.takeIf { it.isNotBlank() } ?: name,
            role = role,
            imageUrl = imageUrl.toShikimoriUrl(),
            seiyuuName = firstSeiyu?.russian?.takeIf { it.isNotBlank() } ?: firstSeiyu?.name,
            seiyuuImageUrl = firstSeiyu?.imageUrl.toShikimoriUrl(),
            source = DetailDataSource.SHIKIMORI,
            malId = null
        )
    }

    private fun ShikimoriRelated.toRelatedItem(matchedTitle: Title?): RelatedTitleItem = RelatedTitleItem(
        id = id.toString(),
        title = matchedTitle?.name?.main ?: russian?.takeIf { it.isNotBlank() } ?: name,
        posterUrl = matchedTitle?.poster?.cardUrl ?: imageUrl.toShikimoriUrl(),
        relation = relation,
        source = DetailDataSource.SHIKIMORI,
        anilibriaId = matchedTitle?.id
    )

    private fun Title.matchNames(): List<String> = listOfNotNull(
        name.main,
        name.english,
        name.alternative
    ).filter { it.isNotBlank() }.map { it.normalizedForMatch() }

    private fun String.normalizedForMatch(): String = lowercase(Locale.ROOT)
        .replace(Regex("[^a-zа-я0-9]+"), "")
        .trim()

    private fun String?.toShikimoriUrl(): String? = when {
        isNullOrBlank() -> null
        startsWith("http") -> this
        startsWith("/") -> "https://shikimori.one$this"
        else -> this
    }

    private suspend fun <T> Flow<NetworkResult<T>>.awaitResult(): NetworkResult<T> {
        var result: NetworkResult<T> = NetworkResult.Loading
        collect { value ->
            if (value !is NetworkResult.Loading) {
                result = value
            }
        }
        return result
    }

    private fun recordExternalError(source: DetailDataSource, message: String) {
        _state.update { state ->
            state.copy(externalErrors = state.externalErrors + (source to message))
        }
    }

    private fun addDebug(message: String) {
        _state.update { state ->
            state.copy(debugMessages = (state.debugMessages + message).takeLast(30))
        }
    }

    private fun setRating(rating: Float) {
        viewModelScope.launch {
            val title = _state.value.title ?: return@launch
            // Повторный тап по уже выставленной звезде снимает оценку.
            // `RatingDao.delete` был реализован, но не вызывался ниоткуда:
            // поставленную по ошибке оценку убрать было невозможно.
            val next = if (rating == _state.value.userRating) 0f else rating
            if (next <= 0f) {
                localRepository.deleteRating(title.id)
            } else {
                localRepository.setRating(title.id, next)
            }
            _state.update { it.copy(userRating = next) }

            // Оценка едет во внешний трекер вместе со статусом.
            manageCollections.getStatus(title.id)?.let { status ->
                syncQueue.enqueue(
                    kind = SyncOperationKind.SHIKIMORI_RATE,
                    titleId = title.id,
                    payload = SyncPayload(status = status.value),
                )
                syncScheduler.syncNow()
            }
        }
    }

    private fun toggleFavorite() {
        viewModelScope.launch {
            val title = _state.value.title ?: return@launch
            val current = _state.value.isFavorite
            val next = !current

            _state.update { it.copy(isFavorite = next, error = null) }

            try {
                if (next) {
                    localRepository.addFavorite(
                        title.id,
                        title.name.main,
                        title.poster?.cardUrl
                    )
                } else {
                    localRepository.removeFavorite(title.id)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isFavorite = current,
                        error = e.message ?: "Не удалось изменить избранное"
                    )
                }
                return@launch
            }

            syncQueue.enqueue(
                kind = SyncOperationKind.FAVORITE,
                titleId = title.id,
                payload = SyncPayload(inFavorites = next),
            )
            syncScheduler.syncNow()
        }
    }

    /**
     * Быстрое «Буду смотреть» — частный случай смены статуса.
     *
     * Отправку на сервер больше не делаем здесь напрямую: без сети действие
     * просто терялось. Теперь его доносит очередь синхронизации.
     */
    private fun toggleWatchLater() {
        viewModelScope.launch {
            val title = _state.value.title ?: return@launch
            manageCollections.toggleStatus(
                titleId = title.id,
                collectionType = CollectionType.WATCH_LATER,
                titleName = title.name.main,
                posterUrl = title.poster?.cardUrl,
            )
            syncScheduler.syncNow()
        }
    }

    private fun togglePlaylistMembership(playlistId: Long) {
        viewModelScope.launch {
            val title = _state.value.title ?: return@launch
            val isInPlaylist = playlistId in _state.value.playlistIdsForTitle

            if (isInPlaylist) {
                localRepository.removePlaylistItem(playlistId, title.id)
            } else {
                localRepository.addPlaylistItem(playlistId, title.id, title.name.main)
            }

            val playlists = localRepository.getPlaylists().first()
            val playlistIds = playlists
                .filter { playlist -> playlist.items.any { it.titleId == title.id } }
                .map { it.id }
                .toSet()
            _state.update {
                it.copy(
                    playlists = playlists,
                    playlistIdsForTitle = playlistIds
                )
            }
        }
    }

    private fun loadVoiceovers(title: Title, shikimoriId: Int?, malId: Long?) {
        viewModelScope.launch {
            _state.update { it.copy(isVoiceoverLoading = true) }
            val anilibriaOption = com.anilibrix.plus.domain.model.VoiceoverOption(
                id = "anilibria",
                name = "AniLibria",
                provider = com.anilibrix.plus.domain.model.VoiceoverProvider.ANILIBRIA,
                type = com.anilibrix.plus.domain.model.VoiceoverType.VOICE,
                episodesCount = title.episodes?.size,
                isDefault = true
            )
            val initialList = listOf(anilibriaOption)
            _state.update {
                it.copy(
                    availableVoiceovers = initialList,
                    selectedVoiceover = it.selectedVoiceover ?: anilibriaOption
                )
            }

            addDebug("Start searching voiceovers for '${title.name.main}' (shikimoriId=$shikimoriId, malId=$malId)")

            var kodikOptions = emptyList<com.anilibrix.plus.domain.model.VoiceoverOption>()
            var consumetOptions = emptyList<com.anilibrix.plus.domain.model.VoiceoverOption>()

            val searchJob1 = launch {
                kodikRepository.getVoiceovers(shikimoriId, malId, title.name.main)
                    .catch { e -> addDebug("Kodik voiceovers exception: ${e.message}") }
                    .collect { result ->
                        if (result is NetworkResult.Success) {
                            kodikOptions = result.data
                            addDebug("Kodik returned ${result.data.size} voiceovers/subs")
                            val combined = (listOf(anilibriaOption) + kodikOptions + consumetOptions).distinctBy { it.id }
                            _state.update { it.copy(availableVoiceovers = combined) }
                            applyPreferredVoiceover(title, combined)
                        }
                    }
            }

            val searchJob2 = launch {
                val enQuery = title.name.english ?: Transliteration.transliterate(title.name.main)
                consumetRepository.searchAndGetVoiceovers(enQuery)
                    .catch { e -> addDebug("Consumet voiceovers exception: ${e.message}") }
                    .collect { result ->
                        if (result is NetworkResult.Success) {
                            consumetOptions = result.data
                            addDebug("Consumet returned ${result.data.size} streams")
                            val combined = (listOf(anilibriaOption) + kodikOptions + consumetOptions).distinctBy { it.id }
                            _state.update { it.copy(availableVoiceovers = combined) }
                            applyPreferredVoiceover(title, combined)
                        }
                    }
            }

            searchJob1.join()
            searchJob2.join()
            _state.update { it.copy(isVoiceoverLoading = false) }
        }
    }

    private suspend fun applyPreferredVoiceover(title: Title, options: List<com.anilibrix.plus.domain.model.VoiceoverOption>) {
        val titlePreference = settingsDataStore.getTitleVoiceover(title.id).first()
        val globalPreference = settingsDataStore.globalPreferredVoiceover.first()

        val matchingOption = when {
            titlePreference != null -> options.find { it.id == titlePreference }
            globalPreference.isNotBlank() && !globalPreference.equals("AniLibria", ignoreCase = true) -> {
                options.find { it.name.contains(globalPreference, ignoreCase = true) }
            }
            else -> null
        } ?: options.find { it.isDefault } ?: options.firstOrNull()

        addDebug("Voiceover match result: titlePref=$titlePreference, globalPref='$globalPreference' -> selected='${matchingOption?.name}'")

        matchingOption?.let { opt ->
            if (opt.provider != com.anilibrix.plus.domain.model.VoiceoverProvider.ANILIBRIA && _state.value.selectedVoiceover?.id != opt.id) {
                selectVoiceover(opt, rememberForTitle = false)
            } else if (_state.value.selectedVoiceover == null) {
                _state.update { it.copy(selectedVoiceover = opt) }
            }
        }
    }

    private fun selectVoiceover(option: com.anilibrix.plus.domain.model.VoiceoverOption, rememberForTitle: Boolean) {
        viewModelScope.launch {
            val title = _state.value.title ?: return@launch
            addDebug("Select voiceover: '${option.name}' (provider=${option.provider}, remember=$rememberForTitle)")
            _state.update { it.copy(selectedVoiceover = option, showVoiceoverSheet = false) }

            if (rememberForTitle) {
                settingsDataStore.setTitleVoiceover(title.id, option.id)
            }

            if (option.provider == com.anilibrix.plus.domain.model.VoiceoverProvider.ANILIBRIA) {
                _state.update { it.copy(voiceoverEpisodes = null, isVoiceoverLoading = false) }
            } else if (option.provider == com.anilibrix.plus.domain.model.VoiceoverProvider.KODIK) {
                _state.update { it.copy(isVoiceoverLoading = true) }
                addDebug("Loading Kodik episodes for translationId=${option.translationId}, shikimoriId=${_state.value.shikimoriId}...")
                kodikRepository.getEpisodes(
                    shikimoriId = _state.value.shikimoriId,
                    malId = _state.value.malId,
                    translationId = option.translationId,
                    kodikId = option.id.removePrefix("kodik_")
                )
                .catch { e ->
                    addDebug("Kodik episodes exception: ${e.message}")
                    _state.update { it.copy(isVoiceoverLoading = false) }
                }
                .collect { epResult ->
                    when (epResult) {
                        is NetworkResult.Success -> {
                            addDebug("Kodik episodes loaded: ${epResult.data.size} episodes found")
                            _state.update { it.copy(voiceoverEpisodes = epResult.data, isVoiceoverLoading = false) }
                        }
                        is NetworkResult.Error -> {
                            addDebug("Kodik episodes error: ${epResult.message}")
                            _state.update { it.copy(isVoiceoverLoading = false) }
                        }
                        NetworkResult.Loading -> {}
                    }
                }
            } else if (option.provider == com.anilibrix.plus.domain.model.VoiceoverProvider.CONSUMET) {
                _state.update { it.copy(isVoiceoverLoading = true) }
                addDebug("Loading Consumet episodes for id=${option.id}...")
                consumetRepository.getEpisodes(option.id)
                .catch { e ->
                    addDebug("Consumet episodes exception: ${e.message}")
                    _state.update { it.copy(isVoiceoverLoading = false) }
                }
                .collect { epResult ->
                    when (epResult) {
                        is NetworkResult.Success -> {
                            addDebug("Consumet episodes loaded: ${epResult.data.size} episodes found")
                            _state.update { it.copy(voiceoverEpisodes = epResult.data, isVoiceoverLoading = false) }
                        }
                        is NetworkResult.Error -> {
                            addDebug("Consumet episodes error: ${epResult.message}")
                            _state.update { it.copy(isVoiceoverLoading = false) }
                        }
                        NetworkResult.Loading -> {}
                    }
                }
            }
        }
    }

    private fun loadNyaaTorrents(title: Title) {
        viewModelScope.launch {
            if (!settingsDataStore.nyaaEnabled.first()) {
                addDebug("Nyaa torrents disabled in settings")
                return@launch
            }
            val query = title.name.english ?: Transliteration.transliterate(title.name.main)
            addDebug("Search Nyaa torrents for query='$query'")
            nyaaRepository.searchTorrents(query)
                .catch { e -> addDebug("Nyaa torrents exception: ${e.message}") }
                .collect { result ->
                    if (result is NetworkResult.Success) {
                        addDebug("Nyaa torrents loaded: ${result.data.size} found")
                        _state.update { it.copy(nyaaTorrents = result.data) }
                    }
                }
        }
    }

    private fun selectTorrentSource(source: String) {
        addDebug("Selected torrent source: $source")
        _state.update { it.copy(selectedTorrentSource = source) }
    }

    private fun clickTorrent(torrent: Torrent) {
        _state.update {
            it.copy(
                selectedTorrentForDownload = torrent,
                torrentMetadataLoading = true,
                torrentResolvedMetadata = null
            )
        }
        viewModelScope.launch {
            val magnetOrUrl = torrent.torrentUrl ?: torrent.magnet ?: ""
            val meta = torrentDownloadManager.resolveMetadata(magnetOrUrl, torrent.series ?: "Торрент")
            _state.update {
                it.copy(
                    torrentMetadataLoading = false,
                    torrentResolvedMetadata = meta
                )
            }
        }
    }

    private fun startTorrentDownload(torrent: Torrent, selectedIndices: Set<Int>?) {
        val title = _state.value.title ?: return
        addDebug("Start built-in torrent download: ${torrent.series ?: torrent.rawTitle}, selectedIndices=$selectedIndices")
        torrentDownloadManager.startDownload(
            titleId = title.id,
            titleName = title.name.main,
            posterUrl = title.poster?.original ?: title.poster?.medium,
            torrent = torrent,
            selectedIndices = selectedIndices
        )
    }
}
