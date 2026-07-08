package com.anilibrix.plus.ui.profile

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
    val isDarkTheme: Boolean = true,
    val notificationsNewEpisodesEnabled: Boolean = true,
    val notificationsAppUpdatesEnabled: Boolean = true,
    val notificationsSyncStatusEnabled: Boolean = true,
    val preferredQuality: String = "1080",
    val defaultSpeed: Float = 1.0f,
    val subtitlesEnabled: Boolean = true,
    val subtitleSize: Int = 24,
    val subtitleColor: String = "#FFFFFF",
    val cacheSizeBytes: Long = 0,
    val syncStatus: String = "Локальный режим",
    val showLogoutDialog: Boolean = false,
    val showAuthSheet: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface ProfileIntent {
    data object Load : ProfileIntent
    data object ToggleTheme : ProfileIntent
    data class SetNotificationsNewEpisodesEnabled(val enabled: Boolean) : ProfileIntent
    data class SetNotificationsAppUpdatesEnabled(val enabled: Boolean) : ProfileIntent
    data class SetNotificationsSyncStatusEnabled(val enabled: Boolean) : ProfileIntent
    data class SetPreferredQuality(val quality: String) : ProfileIntent
    data class SetDefaultSpeed(val speed: Float) : ProfileIntent
    data class SetSubtitlesEnabled(val enabled: Boolean) : ProfileIntent
    data class SetSubtitleSize(val size: Int) : ProfileIntent
    data class SetSubtitleColor(val color: String) : ProfileIntent
    data object ClearCache : ProfileIntent
    data object ShowLogoutDialog : ProfileIntent
    data object DismissLogoutDialog : ProfileIntent
    data object ConfirmLogout : ProfileIntent
    data object ShowAuthSheet : ProfileIntent
    data object DismissAuthSheet : ProfileIntent
    data class Login(val login: String, val password: String) : ProfileIntent
    data object NavigateToChangelog : ProfileIntent
}
