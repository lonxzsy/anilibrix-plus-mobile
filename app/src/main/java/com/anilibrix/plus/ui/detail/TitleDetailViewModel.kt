package com.anilibrix.plus.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.core.util.Transliteration
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.MalAnime
import com.anilibrix.plus.domain.model.MalCharacter
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.ShikimoriAnime
import com.anilibrix.plus.domain.model.ShikimoriCharacter
import com.anilibrix.plus.domain.model.ShikimoriRelated
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.domain.repository.JikanRepository
import com.anilibrix.plus.domain.repository.LocalRepository
import com.anilibrix.plus.domain.repository.ShikimoriRepository
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

@HiltViewModel
class TitleDetailViewModel @Inject constructor(
    private val anilibriaRepository: AnilibriaRepository,
    private val jikanRepository: JikanRepository,
    private val shikimoriRepository: ShikimoriRepository,
    private val localRepository: LocalRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    fun onIntent(intent: DetailIntent) {
        when (intent) {
            is DetailIntent.Load -> loadDetail(intent.id)
            is DetailIntent.SelectTab -> _state.update { it.copy(selectedTab = intent.tab) }
            is DetailIntent.SetRating -> setRating(intent.rating)
            DetailIntent.ToggleFavorite -> toggleFavorite()
            DetailIntent.ToggleWatchLater -> toggleWatchLater()
            is DetailIntent.PlayEpisode -> {}
            is DetailIntent.OpenMagnet -> {}
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
            val isWl = localRepository.isInWatchLater(title.id)
            val rating = localRepository.getRating(title.id) ?: 0f

            _state.update {
                it.copy(
                    isFavorite = isFav,
                    isInWatchLater = isWl,
                    userRating = rating
                )
            }
        }
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
                }
            }
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
        posterUrl = matchedTitle?.poster?.medium ?: matchedTitle?.poster?.small ?: imageUrl.toShikimoriUrl(),
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
            localRepository.setRating(title.id, rating)
            _state.update { it.copy(userRating = rating) }
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
                        title.poster?.medium ?: title.poster?.small
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

            runCatching {
                if (settingsDataStore.authToken.first() != null) {
                    if (next) {
                        anilibriaRepository.addFavorite(title.id).collect {}
                    } else {
                        anilibriaRepository.removeFavorite(title.id).collect {}
                    }
                }
            }
        }
    }

    private fun toggleWatchLater() {
        viewModelScope.launch {
            val title = _state.value.title ?: return@launch
            val current = _state.value.isInWatchLater
            val next = !current

            _state.update { it.copy(isInWatchLater = next, error = null) }

            try {
                if (next) {
                    localRepository.addWatchLater(
                        title.id,
                        titleName = title.name.main,
                        posterUrl = title.poster?.medium ?: title.poster?.small
                    )
                } else {
                    localRepository.removeWatchLater(title.id)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isInWatchLater = current,
                        error = e.message ?: "Не удалось изменить список «Буду смотреть»"
                    )
                }
                return@launch
            }

            runCatching {
                if (settingsDataStore.authToken.first() != null) {
                    if (next) {
                        anilibriaRepository.addToCollection(title.id, CollectionType.WATCH_LATER).collect {}
                    } else {
                        anilibriaRepository.removeFromCollection(title.id, CollectionType.WATCH_LATER).collect {}
                    }
                }
            }
        }
    }
}
