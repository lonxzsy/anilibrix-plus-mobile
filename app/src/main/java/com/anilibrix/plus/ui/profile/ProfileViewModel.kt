package com.anilibrix.plus.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.domain.repository.LocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val anilibriaRepository: AnilibriaRepository,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Load -> loadProfile()
            ProfileIntent.ToggleTheme -> toggleTheme()
            ProfileIntent.ShowLogoutDialog -> _state.update { it.copy(showLogoutDialog = true) }
            ProfileIntent.DismissLogoutDialog -> _state.update { it.copy(showLogoutDialog = false) }
            ProfileIntent.ConfirmLogout -> logout()
            ProfileIntent.ShowAuthSheet -> _state.update { it.copy(showAuthSheet = true) }
            ProfileIntent.DismissAuthSheet -> _state.update { it.copy(showAuthSheet = false) }
            is ProfileIntent.Login -> login(intent.login, intent.password)
            ProfileIntent.NavigateToChangelog -> {}
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }

            val token = settingsDataStore.authToken.first()
            val login = settingsDataStore.authLogin.first()
            val theme = settingsDataStore.theme.first()

            _state.update {
                it.copy(
                    isLoggedIn = token != null,
                    login = login,
                    isDarkTheme = theme != "light"
                )
            }

            if (token != null) {
                launch {
                    anilibriaRepository.getProfile()
                        .catch { e ->
                            _state.update { it.copy(error = e.message ?: "Ошибка загрузки профиля", loading = false) }
                        }
                        .collect { result ->
                            when (result) {
                                is NetworkResult.Success -> {
                                    _state.update {
                                        it.copy(
                                            login = result.data.login,
                                            nickname = result.data.nickname,
                                            email = result.data.email,
                                            avatarUrl = result.data.avatarUrl,
                                            isBanned = result.data.isBanned,
                                            createdAt = result.data.createdAt,
                                            uploadedBytes = result.data.torrentStats?.uploaded ?: 0,
                                            downloadedBytes = result.data.torrentStats?.downloaded ?: 0,
                                            loading = false
                                        )
                                    }
                                }
                                is NetworkResult.Error -> {
                                    _state.update { it.copy(error = result.message, loading = false) }
                                }
                                is NetworkResult.Loading -> {}
                            }
                        }
                }

                launch {
                    anilibriaRepository.getFavoriteReleases()
                        .catch { }
                        .collect { result ->
                            if (result is NetworkResult.Success) {
                                result.data.forEach { title ->
                                    localRepository.addFavorite(
                                        title.id,
                                        title.name.main,
                                        title.poster?.medium ?: title.poster?.small
                                    )
                                }
                            }
                        }
                }

                launch {
                    anilibriaRepository.getTimecodes()
                        .catch { }
                        .collect { result ->
                            if (result is NetworkResult.Success) {
                                result.data.forEach { entry ->
                                    localRepository.addHistory(entry)
                                }
                            }
                        }
                }

                // Синхронизация всех типов коллекций с сервера
                launch {
                    anilibriaRepository.getCollectionIds()
                        .catch { }
                        .collect { result ->
                            if (result is NetworkResult.Success) {
                                // Группируем по типам коллекций
                                val collectionsByType = result.data.groupBy { it.collectionType }
                                
                                // Для каждого типа коллекции загружаем полные данные
                                collectionsByType.forEach { (collectionType, items) ->
                                    launch {
                                        anilibriaRepository.getCollectionReleases(collectionType)
                                            .catch { }
                                            .collect { releasesResult ->
                                                if (releasesResult is NetworkResult.Success) {
                                                    releasesResult.data.forEach { title ->
                                                        localRepository.addToCollection(
                                                            title.id,
                                                            collectionType,
                                                            title.name.main,
                                                            title.poster?.medium ?: title.poster?.small
                                                        )
                                                    }
                                                }
                                            }
                                    }
                                }
                            }
                        }
                }
            } else {
                _state.update { it.copy(loading = false) }
            }

            launch {
                localRepository.getFavorites().collect { favs ->
                    _state.update { it.copy(favoritesCount = favs.size) }
                }
            }

            launch {
                localRepository.getHistory().collect { history ->
                    val uniqueTitles = history.distinctBy { it.titleId }.size
                    val totalTime = history.sumOf { it.timestamp }
                    _state.update {
                        it.copy(
                            historyCount = uniqueTitles,
                            totalWatchTime = totalTime
                        )
                    }
                }
            }
        }
    }

    private fun toggleTheme() {
        viewModelScope.launch {
            val current = _state.value.isDarkTheme
            val newTheme = if (current) "light" else "dark"
            settingsDataStore.setTheme(newTheme)
            _state.update { it.copy(isDarkTheme = !current) }
        }
    }

    private fun login(login: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            anilibriaRepository.login(login, password)
                .catch { e ->
                    _state.update {
                        it.copy(loading = false, error = e.message ?: "Ошибка авторизации")
                    }
                }
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            settingsDataStore.setAuthToken(result.data)
                            settingsDataStore.setAuthLogin(login)
                            _state.update {
                                it.copy(
                                    isLoggedIn = true,
                                    login = login,
                                    showAuthSheet = false
                                )
                            }
                            // Fetch profile immediately after login
                            loadProfile()
                        }
                        is NetworkResult.Error -> {
                            _state.update {
                                it.copy(loading = false, error = result.message)
                            }
                        }
                        is NetworkResult.Loading -> {}
                    }
                }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            settingsDataStore.setAuthToken(null)
            settingsDataStore.setAuthLogin(null)
            localRepository.clearAccountData()
            _state.update {
                it.copy(
                    isLoggedIn = false,
                    login = null,
                    avatarUrl = null,
                    showLogoutDialog = false,
                    favoritesCount = 0,
                    historyCount = 0,
                    totalWatchTime = 0
                )
            }
        }
    }
}
