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
    val showLogoutDialog: Boolean = false,
    val showAuthSheet: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface ProfileIntent {
    data object Load : ProfileIntent
    data object ToggleTheme : ProfileIntent
    data object ShowLogoutDialog : ProfileIntent
    data object DismissLogoutDialog : ProfileIntent
    data object ConfirmLogout : ProfileIntent
    data object ShowAuthSheet : ProfileIntent
    data object DismissAuthSheet : ProfileIntent
    data class Login(val login: String, val password: String) : ProfileIntent
    data object NavigateToChangelog : ProfileIntent
}
