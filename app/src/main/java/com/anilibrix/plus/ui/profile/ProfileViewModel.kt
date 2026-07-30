package com.anilibrix.plus.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.core.download.DownloadRepository
import com.anilibrix.plus.data.sync.ShikimoriAuthManager
import com.anilibrix.plus.data.sync.ShikimoriImporter
import com.anilibrix.plus.work.SyncScheduler
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.domain.repository.LocalRepository
import com.anilibrix.plus.domain.usecase.ManageCollectionsUseCase
import com.anilibrix.plus.ui.player.SkipMode
import com.anilibrix.plus.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val anilibriaRepository: AnilibriaRepository,
    private val localRepository: LocalRepository,
    private val manageCollections: ManageCollectionsUseCase,
    private val downloadRepository: DownloadRepository,
    private val shikimoriAuth: ShikimoriAuthManager,
    private val shikimoriImporter: ShikimoriImporter,
    private val syncScheduler: SyncScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Load -> loadProfile()
            is ProfileIntent.SetThemeMode -> setThemeMode(intent.mode)
            is ProfileIntent.SetDynamicColor -> setDynamicColor(intent.enabled)
            is ProfileIntent.SetNotificationsNewEpisodesEnabled -> setNotificationsNewEpisodesEnabled(intent.enabled)
            is ProfileIntent.SetNotificationsAppUpdatesEnabled -> setNotificationsAppUpdatesEnabled(intent.enabled)
            is ProfileIntent.SetNotificationsSyncStatusEnabled -> setNotificationsSyncStatusEnabled(intent.enabled)
            is ProfileIntent.SetNotificationsResumeEnabled -> viewModelScope.launch {
                settingsDataStore.setNotificationsResumeEnabled(intent.enabled)
                _state.update { it.copy(notificationsResumeEnabled = intent.enabled) }
            }
            is ProfileIntent.SetNotificationsQuietHours -> viewModelScope.launch {
                settingsDataStore.setNotificationsQuietHours(intent.enabled)
                _state.update { it.copy(notificationsQuietHours = intent.enabled) }
            }
            is ProfileIntent.SetPreferredQuality -> setPreferredQuality(intent.quality)
            is ProfileIntent.SetDefaultSpeed -> setDefaultSpeed(intent.speed)
            is ProfileIntent.SetSubtitlesEnabled -> setSubtitlesEnabled(intent.enabled)
            is ProfileIntent.SetSubtitleSize -> setSubtitleSize(intent.size)
            is ProfileIntent.SetSubtitleColor -> setSubtitleColor(intent.color)
            is ProfileIntent.SetSkipMode -> viewModelScope.launch {
                settingsDataStore.setPlayerSkipMode(intent.mode.storageValue)
                _state.update { it.copy(skipMode = intent.mode) }
            }
            is ProfileIntent.SetDownloadQuality -> viewModelScope.launch {
                settingsDataStore.setDownloadQuality(intent.quality)
                _state.update { it.copy(downloadQuality = intent.quality) }
            }
            is ProfileIntent.SetDownloadWifiOnly -> viewModelScope.launch {
                settingsDataStore.setDownloadWifiOnly(intent.enabled)
                _state.update { it.copy(downloadWifiOnly = intent.enabled) }
                // Требование переносим сразу в DownloadManager, иначе уже
                // стоящие в очереди загрузки продолжат жить по старому правилу.
                runCatching { downloadRepository.applyRequirements() }
            }
            ProfileIntent.ClearCache -> clearCache()
            ProfileIntent.ShowClearDataDialog -> _state.update { it.copy(showClearDataDialog = true) }
            ProfileIntent.DismissClearDataDialog -> _state.update { it.copy(showClearDataDialog = false) }
            ProfileIntent.ConfirmClearData -> clearLocalData()
            ProfileIntent.ShowLogoutDialog -> _state.update { it.copy(showLogoutDialog = true) }
            ProfileIntent.DismissLogoutDialog -> _state.update { it.copy(showLogoutDialog = false) }
            ProfileIntent.ConfirmLogout -> logout()
            ProfileIntent.ShowAuthSheet -> _state.update { it.copy(showAuthSheet = true) }
            ProfileIntent.DismissAuthSheet -> _state.update { it.copy(showAuthSheet = false) }
            is ProfileIntent.Login -> login(intent.login, intent.password)
            ProfileIntent.LinkShikimori -> shikimoriAuth.startAuthorization()
            ProfileIntent.UnlinkShikimori -> viewModelScope.launch {
                shikimoriAuth.unlink()
                _state.update {
                    it.copy(
                        shikimoriNickname = null,
                        shikimoriAvatar = null,
                        shikimoriLastSync = 0,
                        shikimoriImportPreview = null,
                    )
                }
            }
            is ProfileIntent.HandleShikimoriRedirect -> completeShikimoriLink(intent.code)
            ProfileIntent.ShowShikimoriImport -> viewModelScope.launch {
                val preview = runCatching { shikimoriImporter.preview() }.getOrNull()
                _state.update { it.copy(shikimoriImportPreview = preview?.total ?: 0) }
            }
            ProfileIntent.DismissShikimoriImport ->
                _state.update { it.copy(shikimoriImportPreview = null) }
            ProfileIntent.ConfirmShikimoriImport -> runShikimoriImport()
            ProfileIntent.SyncNow -> viewModelScope.launch {
                _state.update { it.copy(shikimoriSyncing = true) }
                syncScheduler.syncNow()
                runCatching { manageCollections.syncFromApi(settingsDataStore.authToken.first()) }
                val result = runCatching { shikimoriImporter.import() }.getOrNull()
                _state.update {
                    it.copy(
                        shikimoriSyncing = false,
                        shikimoriLastSync = System.currentTimeMillis(),
                        syncStatus = result?.summary() ?: it.syncStatus,
                    )
                }
            }
            is ProfileIntent.SetShikimoriPushStatus -> viewModelScope.launch {
                settingsDataStore.setShikimoriPushStatus(intent.enabled)
                _state.update { it.copy(shikimoriPushStatus = intent.enabled) }
            }
            is ProfileIntent.SetShikimoriPushRatings -> viewModelScope.launch {
                settingsDataStore.setShikimoriPushRatings(intent.enabled)
                _state.update { it.copy(shikimoriPushRatings = intent.enabled) }
            }
            ProfileIntent.NavigateToChangelog -> {}
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }

            val token = settingsDataStore.authToken.first()
            val login = settingsDataStore.authLogin.first()
            val theme = settingsDataStore.theme.first()
            val dynamicColor = settingsDataStore.dynamicColor.first()
            val preferredQuality = settingsDataStore.preferredQuality.first()
            val defaultSpeed = settingsDataStore.playerSpeed.first().toFloatOrNull() ?: 1.0f
            val subtitlesEnabled = settingsDataStore.playerSubtitlesEnabled.first()
            val subtitleSize = settingsDataStore.playerSubtitlesSize.first()
            val subtitleColor = settingsDataStore.playerSubtitlesColor.first()
            val notificationsNewEpisodesEnabled = settingsDataStore.notificationsNewEpisodesEnabled.first()
            val notificationsAppUpdatesEnabled = settingsDataStore.notificationsAppUpdatesEnabled.first()
            val notificationsSyncStatusEnabled = settingsDataStore.notificationsSyncStatusEnabled.first()
            val skipMode = SkipMode.fromStorage(settingsDataStore.playerSkipMode.first())
            val downloadQuality = settingsDataStore.downloadQuality.first()
            val downloadWifiOnly = settingsDataStore.downloadWifiOnly.first()
            val downloadsUsed = runCatching { downloadRepository.usedBytes() }.getOrDefault(0L)
            val notificationsResumeEnabled = settingsDataStore.notificationsResumeEnabled.first()
            val notificationsQuietHours = settingsDataStore.notificationsQuietHours.first()
            val shikimoriNickname = settingsDataStore.shikimoriNickname.first()
            val shikimoriAvatar = settingsDataStore.shikimoriAvatar.first()
            val shikimoriLastSync = settingsDataStore.shikimoriLastSync.first()
            val shikimoriPushStatus = settingsDataStore.shikimoriPushStatus.first()
            val shikimoriPushRatings = settingsDataStore.shikimoriPushRatings.first()

            _state.update {
                it.copy(
                    isLoggedIn = token != null,
                    login = login,
                    themeMode = ThemeMode.fromStorage(theme),
                    dynamicColor = dynamicColor,
                    notificationsNewEpisodesEnabled = notificationsNewEpisodesEnabled,
                    notificationsAppUpdatesEnabled = notificationsAppUpdatesEnabled,
                    notificationsSyncStatusEnabled = notificationsSyncStatusEnabled,
                    notificationsResumeEnabled = notificationsResumeEnabled,
                    notificationsQuietHours = notificationsQuietHours,
                    preferredQuality = preferredQuality,
                    defaultSpeed = defaultSpeed.coerceIn(0.25f, 3.0f),
                    subtitlesEnabled = subtitlesEnabled,
                    subtitleSize = subtitleSize.coerceIn(14, 48),
                    subtitleColor = subtitleColor,
                    skipMode = skipMode,
                    downloadQuality = downloadQuality,
                    downloadWifiOnly = downloadWifiOnly,
                    downloadsUsedBytes = downloadsUsed,
                    shikimoriConfigured = shikimoriAuth.isConfigured,
                    shikimoriNickname = shikimoriNickname,
                    shikimoriAvatar = shikimoriAvatar,
                    shikimoriLastSync = shikimoriLastSync,
                    shikimoriPushStatus = shikimoriPushStatus,
                    shikimoriPushRatings = shikimoriPushRatings,
                    cacheSizeBytes = calculateCacheSize(),
                    syncStatus = if (token != null) "Синхронизация включена" else "Локальный режим"
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
                                    _state.update {
                                        it.copy(
                                            error = result.message,
                                            loading = false,
                                            syncStatus = "Ошибка синхронизации"
                                        )
                                    }
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
                                        title.poster?.cardUrl
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

                // Коллекции подтягиваются через use case: он единственный
                // знает правило разрешения конфликтов и не перетирает статусы,
                // изменённые локально позже.
                launch { runCatching { manageCollections.syncFromApi(token) } }
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
                    // Считаем в базе, а не суммированием `timestamp` по списку:
                    // позиция возобновления досмотренной серии равна нулю
                    // (при завершении она не сохраняется), поэтому прежняя
                    // сумма занижала время просмотра тем сильнее, чем больше
                    // человек досмотрел до конца.
                    val totalTime = localRepository.getTotalWatchTimeMs()
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

    /**
     * Пишет режим в DataStore. Оболочку обновлять отдельно не нужно:
     * AppThemeViewModel слушает тот же самый ключ.
     */
    private fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setTheme(mode.storageValue)
            _state.update { it.copy(themeMode = mode) }
        }
    }

    private fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setDynamicColor(enabled)
            _state.update { it.copy(dynamicColor = enabled) }
        }
    }

    private fun setNotificationsNewEpisodesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setNotificationsNewEpisodesEnabled(enabled)
            _state.update { it.copy(notificationsNewEpisodesEnabled = enabled) }
        }
    }

    private fun setNotificationsAppUpdatesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setNotificationsAppUpdatesEnabled(enabled)
            _state.update { it.copy(notificationsAppUpdatesEnabled = enabled) }
        }
    }

    private fun setNotificationsSyncStatusEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setNotificationsSyncStatusEnabled(enabled)
            _state.update { it.copy(notificationsSyncStatusEnabled = enabled) }
        }
    }

    private fun setPreferredQuality(quality: String) {
        viewModelScope.launch {
            settingsDataStore.setPreferredQuality(quality)
            _state.update { it.copy(preferredQuality = quality) }
        }
    }

    private fun setDefaultSpeed(speed: Float) {
        viewModelScope.launch {
            val normalized = speed.coerceIn(0.25f, 3.0f)
            settingsDataStore.setPlayerSpeed(normalized.toString())
            _state.update { it.copy(defaultSpeed = normalized) }
        }
    }

    private fun setSubtitlesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setPlayerSubtitlesEnabled(enabled)
            _state.update { it.copy(subtitlesEnabled = enabled) }
        }
    }

    private fun setSubtitleSize(size: Int) {
        viewModelScope.launch {
            val normalized = size.coerceIn(14, 48)
            settingsDataStore.setPlayerSubtitlesSize(normalized)
            _state.update { it.copy(subtitleSize = normalized) }
        }
    }

    private fun setSubtitleColor(color: String) {
        viewModelScope.launch {
            settingsDataStore.setPlayerSubtitlesColor(color)
            _state.update { it.copy(subtitleColor = color) }
        }
    }

    private fun clearCache() {
        viewModelScope.launch {
            cacheDirs().forEach { dir ->
                if (dir.exists()) dir.deleteRecursively()
            }
            _state.update { it.copy(cacheSizeBytes = calculateCacheSize()) }
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

    /**
     * Выход из аккаунта.
     *
     * Локальные данные **остаются** — ровно так, как обещает текст диалога.
     * Раньше здесь вызывался `clearAccountData()`, который стирал избранное,
     * историю, коллекции, оценки и плейлисты: диалог говорил одно, код делал
     * прямо противоположное, и восстановить стёртое было уже нечем.
     */
    private fun logout() {
        viewModelScope.launch {
            settingsDataStore.setAuthToken(null)
            settingsDataStore.setAuthLogin(null)
            _state.update {
                it.copy(
                    isLoggedIn = false,
                    login = null,
                    nickname = null,
                    email = null,
                    avatarUrl = null,
                    showLogoutDialog = false,
                    syncStatus = "Локальный режим"
                )
            }
            // Счётчики пересчитываем, а не обнуляем: данные никуда не делись.
            refreshLocalCounters()
        }
    }

    /** Явное и подтверждённое удаление всего локального. */
    private fun clearLocalData() {
        viewModelScope.launch {
            localRepository.clearAllUserData()
            _state.update {
                it.copy(
                    showClearDataDialog = false,
                    favoritesCount = 0,
                    historyCount = 0,
                    totalWatchTime = 0,
                )
            }
        }
    }

    private suspend fun refreshLocalCounters() {
        val favorites = localRepository.getFavorites().first()
        _state.update {
            it.copy(
                favoritesCount = favorites.size,
                historyCount = localRepository.getHistoryCount(),
                totalWatchTime = localRepository.getTotalWatchTimeMs(),
            )
        }
    }

    /**
     * Завершение привязки после возврата из браузера.
     *
     * Сразу после успеха предлагаем импорт, а не запускаем его молча: у
     * человека может быть большой список, и перезапись своих локальных
     * пометок должна быть его решением.
     */
    private fun completeShikimoriLink(code: String) {
        viewModelScope.launch {
            _state.update { it.copy(shikimoriSyncing = true) }
            val nickname = runCatching { shikimoriAuth.completeAuthorization(code) }.getOrNull()
            if (nickname == null) {
                _state.update {
                    it.copy(shikimoriSyncing = false, error = "Не удалось привязать Shikimori")
                }
                return@launch
            }
            val preview = runCatching { shikimoriImporter.preview() }.getOrNull()
            _state.update {
                it.copy(
                    shikimoriSyncing = false,
                    shikimoriNickname = nickname,
                    shikimoriAvatar = settingsDataStore.shikimoriAvatar.first(),
                    shikimoriImportPreview = preview?.total?.takeIf { total -> total > 0 },
                )
            }
        }
    }

    private fun runShikimoriImport() {
        viewModelScope.launch {
            _state.update { it.copy(shikimoriSyncing = true, shikimoriImportPreview = null) }
            val result = runCatching { shikimoriImporter.import() }.getOrNull()
            _state.update {
                it.copy(
                    shikimoriSyncing = false,
                    shikimoriLastSync = System.currentTimeMillis(),
                    syncStatus = result?.summary() ?: "Импорт не удался",
                )
            }
        }
    }

    private fun calculateCacheSize(): Long {
        return cacheDirs().sumOf { it.sizeBytes() }
    }

    private fun cacheDirs(): List<File> {
        return listOf(
            File(context.cacheDir, "media_cache"),
            File(context.cacheDir, "http_cache"),
            File(context.cacheDir, "glide_cache")
        )
    }

    private fun File.sizeBytes(): Long {
        if (!exists()) return 0L
        return if (isFile) {
            length()
        } else {
            listFiles()?.sumOf { it.sizeBytes() } ?: 0L
        }
    }
}
