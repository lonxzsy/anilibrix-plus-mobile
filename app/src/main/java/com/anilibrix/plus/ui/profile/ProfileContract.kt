package com.anilibrix.plus.ui.profile

import com.anilibrix.plus.ui.player.SkipMode
import com.anilibrix.plus.ui.theme.ThemeMode

data class ProfileUiState(
    val isLoggedIn: Boolean = false,
    val login: String? = null,
    val nickname: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val isBanned: Boolean = false,
    val createdAt: String? = null,
    val uploadedBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val favoritesCount: Int = 0,
    val historyCount: Int = 0,
    val totalWatchTime: Long = 0,
    val themeMode: ThemeMode = ThemeMode.DARK,
    /** Material You. Выключен по умолчанию — приоритет у фирменной палитры. */
    val dynamicColor: Boolean = false,
    val notificationsNewEpisodesEnabled: Boolean = true,
    val notificationsAppUpdatesEnabled: Boolean = true,
    val notificationsSyncStatusEnabled: Boolean = true,
    val notificationsResumeEnabled: Boolean = true,
    val notificationsQuietHours: Boolean = true,
    val preferredQuality: String = "1080",
    val defaultSpeed: Float = 1.0f,
    val subtitlesEnabled: Boolean = true,
    val subtitleSize: Int = 24,
    val subtitleColor: String = "#FFFFFF",
    val skipMode: SkipMode = SkipMode.ASK,
    val downloadQuality: String = "720",
    val downloadWifiOnly: Boolean = true,
    val downloadsUsedBytes: Long = 0,
    // --- Shikimori ---
    /** Ключи приложения прописаны в сборке. Без них раздел недоступен. */
    val shikimoriConfigured: Boolean = false,
    val shikimoriNickname: String? = null,
    val shikimoriAvatar: String? = null,
    val shikimoriLastSync: Long = 0,
    val shikimoriPushStatus: Boolean = true,
    val shikimoriPushRatings: Boolean = true,
    val shikimoriSyncing: Boolean = false,
    /** Сколько записей найдено на Shikimori — показывается перед импортом. */
    val shikimoriImportPreview: Int? = null,
    val cacheSizeBytes: Long = 0,
    val syncStatus: String = "Локальный режим",
    val showLogoutDialog: Boolean = false,
    /** Подтверждение удаления локальных данных — операция необратима. */
    val showClearDataDialog: Boolean = false,
    val showAuthSheet: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface ProfileIntent {
    data object Load : ProfileIntent
    data class SetThemeMode(val mode: ThemeMode) : ProfileIntent
    data class SetDynamicColor(val enabled: Boolean) : ProfileIntent
    data class SetNotificationsNewEpisodesEnabled(val enabled: Boolean) : ProfileIntent
    data class SetNotificationsAppUpdatesEnabled(val enabled: Boolean) : ProfileIntent
    data class SetNotificationsSyncStatusEnabled(val enabled: Boolean) : ProfileIntent
    data class SetNotificationsResumeEnabled(val enabled: Boolean) : ProfileIntent
    data class SetNotificationsQuietHours(val enabled: Boolean) : ProfileIntent
    data class SetPreferredQuality(val quality: String) : ProfileIntent
    data class SetDefaultSpeed(val speed: Float) : ProfileIntent
    data class SetSubtitlesEnabled(val enabled: Boolean) : ProfileIntent
    data class SetSubtitleSize(val size: Int) : ProfileIntent
    data class SetSubtitleColor(val color: String) : ProfileIntent
    data class SetSkipMode(val mode: SkipMode) : ProfileIntent
    data class SetDownloadQuality(val quality: String) : ProfileIntent
    data class SetDownloadWifiOnly(val enabled: Boolean) : ProfileIntent
    data object LinkShikimori : ProfileIntent
    data object UnlinkShikimori : ProfileIntent
    data class HandleShikimoriRedirect(val code: String) : ProfileIntent
    data object ShowShikimoriImport : ProfileIntent
    data object DismissShikimoriImport : ProfileIntent
    data object ConfirmShikimoriImport : ProfileIntent
    data object SyncNow : ProfileIntent
    data class SetShikimoriPushStatus(val enabled: Boolean) : ProfileIntent
    data class SetShikimoriPushRatings(val enabled: Boolean) : ProfileIntent
    data object ClearCache : ProfileIntent
    data object ShowClearDataDialog : ProfileIntent
    data object DismissClearDataDialog : ProfileIntent
    data object ConfirmClearData : ProfileIntent
    data object ShowLogoutDialog : ProfileIntent
    data object DismissLogoutDialog : ProfileIntent
    data object ConfirmLogout : ProfileIntent
    data object ShowAuthSheet : ProfileIntent
    data object DismissAuthSheet : ProfileIntent
    data class Login(val login: String, val password: String) : ProfileIntent
    data object NavigateToChangelog : ProfileIntent
}
