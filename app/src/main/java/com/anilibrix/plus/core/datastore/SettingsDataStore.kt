package com.anilibrix.plus.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.anilibrix.plus.core.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val crypto: CryptoManager,
) {

    companion object {
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val AUTH_LOGIN = stringPreferencesKey("auth_login")
        private val THEME = stringPreferencesKey("theme")
        private val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val PREFERRED_QUALITY = stringPreferencesKey("preferred_quality")
        private val LAST_SEEN_VERSION = stringPreferencesKey("last_seen_version")
        private val MAL_ID_MAP = stringPreferencesKey("mal_id_map")
        private val PLAYER_SUBTITLES_ENABLED = booleanPreferencesKey("player_subtitles_enabled")
        private val PLAYER_SUBTITLES_SIZE = intPreferencesKey("player_subtitles_size")
        private val PLAYER_SUBTITLES_COLOR = stringPreferencesKey("player_subtitles_color")
        private val PLAYER_SPEED = stringPreferencesKey("player_speed")
        private val CATALOG_FILTER_GENRE = stringPreferencesKey("catalog_filter_genre")
        private val CATALOG_FILTER_YEAR = intPreferencesKey("catalog_filter_year")
        private val CATALOG_FILTER_TYPE = stringPreferencesKey("catalog_filter_type")
        private val CATALOG_FILTER_SEASON = stringPreferencesKey("catalog_filter_season")
        private val CATALOG_FILTER_STATUS = stringPreferencesKey("catalog_filter_status")
        private val CATALOG_SORT = stringPreferencesKey("catalog_sort")
        private val CATALOG_VIEW_MODE = stringPreferencesKey("catalog_view_mode")
        private val CATALOG_SEARCH_HISTORY = stringPreferencesKey("catalog_search_history")
        private val NOTIFICATIONS_NEW_EPISODES_ENABLED = booleanPreferencesKey("notifications_new_episodes_enabled")
        private val NOTIFICATIONS_APP_UPDATES_ENABLED = booleanPreferencesKey("notifications_app_updates_enabled")
        private val NOTIFICATIONS_SYNC_STATUS_ENABLED = booleanPreferencesKey("notifications_sync_status_enabled")
        private val NOTIFICATIONS_LAST_EPISODE_SNAPSHOT = stringPreferencesKey("notifications_last_episode_snapshot")

        // Shikimori
        private val SHIKIMORI_ACCESS_TOKEN = stringPreferencesKey("shikimori_access_token")
        private val SHIKIMORI_REFRESH_TOKEN = stringPreferencesKey("shikimori_refresh_token")
        private val SHIKIMORI_EXPIRES_AT = longPreferencesKey("shikimori_expires_at")
        private val SHIKIMORI_USER_ID = intPreferencesKey("shikimori_user_id")
        private val SHIKIMORI_NICKNAME = stringPreferencesKey("shikimori_nickname")
        private val SHIKIMORI_AVATAR = stringPreferencesKey("shikimori_avatar")
        private val SHIKIMORI_LAST_SYNC = longPreferencesKey("shikimori_last_sync")
        private val SHIKIMORI_PUSH_STATUS = booleanPreferencesKey("shikimori_push_status")
        private val SHIKIMORI_PUSH_RATINGS = booleanPreferencesKey("shikimori_push_ratings")

        // Плеер
        private val PLAYER_SKIP_MODE = stringPreferencesKey("player_skip_mode")
        private val PLAYER_SUBTITLE_BACKGROUND = booleanPreferencesKey("player_subtitle_background")
        private val PLAYER_SUBTITLE_OFFSET_MS = longPreferencesKey("player_subtitle_offset_ms")

        // Загрузки
        private val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        private val DOWNLOAD_WIFI_ONLY = booleanPreferencesKey("download_wifi_only")

        // Удержание
        private val NOTIFICATIONS_RESUME_ENABLED = booleanPreferencesKey("notifications_resume_enabled")
        private val NOTIFICATIONS_QUIET_HOURS = booleanPreferencesKey("notifications_quiet_hours")
        private val LAST_RESUME_REMINDER_AT = longPreferencesKey("last_resume_reminder_at")
        private val LAST_UPDATE_CHECK_AT = longPreferencesKey("last_update_check_at")

        // Сторонние озвучки и провайдеры
        private val KODIK_CUSTOM_TOKEN = stringPreferencesKey("kodik_custom_token")
        private val GLOBAL_PREFERRED_VOICEOVER = stringPreferencesKey("global_preferred_voiceover")
        private val TITLE_VOICEOVER_MAP = stringPreferencesKey("title_voiceover_map")
        private val ANISKIP_ENABLED = booleanPreferencesKey("aniskip_enabled")
        private val CONSUMET_ENABLED = booleanPreferencesKey("consumet_enabled")
        private val NYAA_ENABLED = booleanPreferencesKey("nyaa_enabled")
    }

    /**
     * Токен авторизации. В DataStore лежит зашифрованным ([CryptoManager]).
     *
     * Значения, записанные предыдущими версиями приложения, лежат открытым
     * текстом — они читаются как есть и перешифровываются при первой же записи
     * (см. [migrateAuthTokenIfNeeded]). Иначе обновление приложения выкинуло бы
     * всех из аккаунта.
     */
    val authToken: Flow<String?> = dataStore.data.map { prefs ->
        prefs[AUTH_TOKEN]?.let { stored ->
            if (crypto.isEncrypted(stored)) crypto.decrypt(stored) else stored
        }
    }
    val authLogin: Flow<String?> = dataStore.data.map { it[AUTH_LOGIN] }
    /** "system" | "light" | "dark". Historically only "light"/"dark" были записаны. */
    val theme: Flow<String> = dataStore.data.map { it[THEME] ?: "dark" }

    /**
     * Material You. По умолчанию ВЫКЛЮЧЕН — приложение показывает фирменную
     * палитру. Раньше dynamicColor было захардкожено в true, из-за чего вся
     * палитра проекта не отрисовывалась ни разу.
     */
    val dynamicColor: Flow<Boolean> = dataStore.data.map { it[DYNAMIC_COLOR] ?: false }
    val preferredQuality: Flow<String> = dataStore.data.map { it[PREFERRED_QUALITY] ?: "1080" }
    val lastSeenVersion: Flow<String?> = dataStore.data.map { it[LAST_SEEN_VERSION] }
    val malIdMap: Flow<String?> = dataStore.data.map { it[MAL_ID_MAP] }
    val playerSubtitlesEnabled: Flow<Boolean> = dataStore.data.map { it[PLAYER_SUBTITLES_ENABLED] ?: true }
    val playerSubtitlesSize: Flow<Int> = dataStore.data.map { it[PLAYER_SUBTITLES_SIZE] ?: 24 }
    val playerSubtitlesColor: Flow<String> = dataStore.data.map { it[PLAYER_SUBTITLES_COLOR] ?: "#FFFFFF" }
    val playerSpeed: Flow<String> = dataStore.data.map { it[PLAYER_SPEED] ?: "1.0" }
    val catalogFilterGenre: Flow<String?> = dataStore.data.map { it[CATALOG_FILTER_GENRE] }
    val catalogFilterYear: Flow<Int?> = dataStore.data.map { it[CATALOG_FILTER_YEAR] }
    val catalogFilterType: Flow<String?> = dataStore.data.map { it[CATALOG_FILTER_TYPE] }
    val catalogFilterSeason: Flow<String?> = dataStore.data.map { it[CATALOG_FILTER_SEASON] }
    val catalogFilterStatus: Flow<String?> = dataStore.data.map { it[CATALOG_FILTER_STATUS] }
    val catalogSort: Flow<String> = dataStore.data.map { it[CATALOG_SORT] ?: "UPDATED" }
    val catalogViewMode: Flow<String> = dataStore.data.map { it[CATALOG_VIEW_MODE] ?: "GRID" }
    val catalogSearchHistory: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[CATALOG_SEARCH_HISTORY]
            ?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }
    val notificationsNewEpisodesEnabled: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS_NEW_EPISODES_ENABLED] ?: true }
    val notificationsAppUpdatesEnabled: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS_APP_UPDATES_ENABLED] ?: true }
    val notificationsSyncStatusEnabled: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS_SYNC_STATUS_ENABLED] ?: true }
    val notificationsLastEpisodeSnapshot: Flow<String?> = dataStore.data.map { it[NOTIFICATIONS_LAST_EPISODE_SNAPSHOT] }

    suspend fun setAuthToken(token: String?) {
        dataStore.edit {
            if (token != null) it[AUTH_TOKEN] = crypto.encrypt(token) else it.remove(AUTH_TOKEN)
        }
    }

    /**
     * Одноразово перешифровывает plaintext-токен, доставшийся от предыдущих
     * версий. Вызывается на старте приложения; для уже зашифрованного значения
     * и для отсутствующего токена ничего не делает.
     */
    suspend fun migrateAuthTokenIfNeeded() {
        dataStore.edit { prefs ->
            val stored = prefs[AUTH_TOKEN] ?: return@edit
            if (crypto.isEncrypted(stored)) return@edit
            prefs[AUTH_TOKEN] = crypto.encrypt(stored)
        }
    }

    suspend fun setAuthLogin(login: String?) {
        dataStore.edit { if (login != null) it[AUTH_LOGIN] = login else it.remove(AUTH_LOGIN) }
    }

    suspend fun setTheme(theme: String) {
        dataStore.edit { it[THEME] = theme }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setPreferredQuality(quality: String) {
        dataStore.edit { it[PREFERRED_QUALITY] = quality }
    }

    suspend fun setLastSeenVersion(version: String) {
        dataStore.edit { it[LAST_SEEN_VERSION] = version }
    }

    suspend fun setMalIdMap(map: String?) {
        dataStore.edit { if (map != null) it[MAL_ID_MAP] = map else it.remove(MAL_ID_MAP) }
    }

    suspend fun setPlayerSubtitlesEnabled(enabled: Boolean) {
        dataStore.edit { it[PLAYER_SUBTITLES_ENABLED] = enabled }
    }

    suspend fun setPlayerSubtitlesSize(size: Int) {
        dataStore.edit { it[PLAYER_SUBTITLES_SIZE] = size }
    }

    suspend fun setPlayerSubtitlesColor(color: String) {
        dataStore.edit { it[PLAYER_SUBTITLES_COLOR] = color }
    }

    suspend fun setPlayerSpeed(speed: String) {
        dataStore.edit { it[PLAYER_SPEED] = speed }
    }

    suspend fun setCatalogFilterGenre(genre: String?) {
        dataStore.edit { if (genre != null) it[CATALOG_FILTER_GENRE] = genre else it.remove(CATALOG_FILTER_GENRE) }
    }

    suspend fun setCatalogFilterYear(year: Int?) {
        dataStore.edit { if (year != null) it[CATALOG_FILTER_YEAR] = year else it.remove(CATALOG_FILTER_YEAR) }
    }

    suspend fun setCatalogFilterType(type: String?) {
        dataStore.edit { if (type != null) it[CATALOG_FILTER_TYPE] = type else it.remove(CATALOG_FILTER_TYPE) }
    }

    suspend fun setCatalogFilterSeason(season: String?) {
        dataStore.edit { if (season != null) it[CATALOG_FILTER_SEASON] = season else it.remove(CATALOG_FILTER_SEASON) }
    }

    suspend fun setCatalogFilterStatus(status: String?) {
        dataStore.edit { if (status != null) it[CATALOG_FILTER_STATUS] = status else it.remove(CATALOG_FILTER_STATUS) }
    }

    suspend fun setCatalogSort(sort: String) {
        dataStore.edit { it[CATALOG_SORT] = sort }
    }

    suspend fun setCatalogViewMode(viewMode: String) {
        dataStore.edit { it[CATALOG_VIEW_MODE] = viewMode }
    }

    suspend fun setCatalogSearchHistory(history: List<String>) {
        dataStore.edit {
            val encoded = history
                .map { item -> item.replace("\n", " ").trim() }
                .filter { item -> item.isNotEmpty() }
                .take(10)
                .joinToString("\n")
            if (encoded.isBlank()) {
                it.remove(CATALOG_SEARCH_HISTORY)
            } else {
                it[CATALOG_SEARCH_HISTORY] = encoded
            }
        }
    }

    suspend fun setNotificationsNewEpisodesEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_NEW_EPISODES_ENABLED] = enabled }
    }

    suspend fun setNotificationsAppUpdatesEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_APP_UPDATES_ENABLED] = enabled }
    }

    suspend fun setNotificationsSyncStatusEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_SYNC_STATUS_ENABLED] = enabled }
    }

    suspend fun setNotificationsLastEpisodeSnapshot(snapshot: String?) {
        dataStore.edit { if (snapshot != null) it[NOTIFICATIONS_LAST_EPISODE_SNAPSHOT] = snapshot else it.remove(NOTIFICATIONS_LAST_EPISODE_SNAPSHOT) }
    }

    // --- Shikimori ---------------------------------------------------------
    // Токены шифруются тем же ключом Keystore, что и токен Anilibria: это
    // такой же доступ к чужому аккаунту, и хранить его иначе оснований нет.

    val shikimoriAccessToken: Flow<String?> = dataStore.data.map { prefs ->
        prefs[SHIKIMORI_ACCESS_TOKEN]?.let { crypto.decrypt(it) }
    }
    val shikimoriRefreshToken: Flow<String?> = dataStore.data.map { prefs ->
        prefs[SHIKIMORI_REFRESH_TOKEN]?.let { crypto.decrypt(it) }
    }
    val shikimoriExpiresAt: Flow<Long> = dataStore.data.map { it[SHIKIMORI_EXPIRES_AT] ?: 0L }
    val shikimoriUserId: Flow<Int?> = dataStore.data.map { it[SHIKIMORI_USER_ID] }
    val shikimoriNickname: Flow<String?> = dataStore.data.map { it[SHIKIMORI_NICKNAME] }
    val shikimoriAvatar: Flow<String?> = dataStore.data.map { it[SHIKIMORI_AVATAR] }
    val shikimoriLastSync: Flow<Long> = dataStore.data.map { it[SHIKIMORI_LAST_SYNC] ?: 0L }
    val shikimoriPushStatus: Flow<Boolean> = dataStore.data.map { it[SHIKIMORI_PUSH_STATUS] ?: true }
    val shikimoriPushRatings: Flow<Boolean> = dataStore.data.map { it[SHIKIMORI_PUSH_RATINGS] ?: true }

    suspend fun setShikimoriTokens(accessToken: String?, refreshToken: String?, expiresAt: Long) {
        dataStore.edit { prefs ->
            if (accessToken != null) {
                prefs[SHIKIMORI_ACCESS_TOKEN] = crypto.encrypt(accessToken)
            } else {
                prefs.remove(SHIKIMORI_ACCESS_TOKEN)
            }
            if (refreshToken != null) {
                prefs[SHIKIMORI_REFRESH_TOKEN] = crypto.encrypt(refreshToken)
            } else {
                prefs.remove(SHIKIMORI_REFRESH_TOKEN)
            }
            prefs[SHIKIMORI_EXPIRES_AT] = expiresAt
        }
    }

    suspend fun setShikimoriUser(userId: Int?, nickname: String?, avatar: String?) {
        dataStore.edit { prefs ->
            if (userId != null) prefs[SHIKIMORI_USER_ID] = userId else prefs.remove(SHIKIMORI_USER_ID)
            if (nickname != null) prefs[SHIKIMORI_NICKNAME] = nickname else prefs.remove(SHIKIMORI_NICKNAME)
            if (avatar != null) prefs[SHIKIMORI_AVATAR] = avatar else prefs.remove(SHIKIMORI_AVATAR)
        }
    }

    suspend fun setShikimoriLastSync(timestamp: Long) {
        dataStore.edit { it[SHIKIMORI_LAST_SYNC] = timestamp }
    }

    suspend fun setShikimoriPushStatus(enabled: Boolean) {
        dataStore.edit { it[SHIKIMORI_PUSH_STATUS] = enabled }
    }

    suspend fun setShikimoriPushRatings(enabled: Boolean) {
        dataStore.edit { it[SHIKIMORI_PUSH_RATINGS] = enabled }
    }

    suspend fun clearShikimori() {
        dataStore.edit { prefs ->
            prefs.remove(SHIKIMORI_ACCESS_TOKEN)
            prefs.remove(SHIKIMORI_REFRESH_TOKEN)
            prefs.remove(SHIKIMORI_EXPIRES_AT)
            prefs.remove(SHIKIMORI_USER_ID)
            prefs.remove(SHIKIMORI_NICKNAME)
            prefs.remove(SHIKIMORI_AVATAR)
            prefs.remove(SHIKIMORI_LAST_SYNC)
        }
    }

    // --- Плеер -------------------------------------------------------------

    /** "ask" | "auto" | "never" — см. `SkipMode`. */
    val playerSkipMode: Flow<String> = dataStore.data.map { it[PLAYER_SKIP_MODE] ?: "ask" }
    val playerSubtitleBackground: Flow<Boolean> = dataStore.data.map { it[PLAYER_SUBTITLE_BACKGROUND] ?: true }
    val playerSubtitleOffsetMs: Flow<Long> = dataStore.data.map { it[PLAYER_SUBTITLE_OFFSET_MS] ?: 0L }

    suspend fun setPlayerSkipMode(mode: String) {
        dataStore.edit { it[PLAYER_SKIP_MODE] = mode }
    }

    suspend fun setPlayerSubtitleBackground(enabled: Boolean) {
        dataStore.edit { it[PLAYER_SUBTITLE_BACKGROUND] = enabled }
    }

    suspend fun setPlayerSubtitleOffsetMs(offset: Long) {
        dataStore.edit { it[PLAYER_SUBTITLE_OFFSET_MS] = offset.coerceIn(-30_000L, 30_000L) }
    }

    // --- Загрузки ----------------------------------------------------------

    val downloadQuality: Flow<String> = dataStore.data.map { it[DOWNLOAD_QUALITY] ?: "720" }
    val downloadWifiOnly: Flow<Boolean> = dataStore.data.map { it[DOWNLOAD_WIFI_ONLY] ?: true }

    suspend fun setDownloadQuality(quality: String) {
        dataStore.edit { it[DOWNLOAD_QUALITY] = quality }
    }

    suspend fun setDownloadWifiOnly(enabled: Boolean) {
        dataStore.edit { it[DOWNLOAD_WIFI_ONLY] = enabled }
    }

    // --- Уведомления и обновления -----------------------------------------

    val notificationsResumeEnabled: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS_RESUME_ENABLED] ?: true }
    val notificationsQuietHours: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS_QUIET_HOURS] ?: true }
    val lastResumeReminderAt: Flow<Long> = dataStore.data.map { it[LAST_RESUME_REMINDER_AT] ?: 0L }
    val lastUpdateCheckAt: Flow<Long> = dataStore.data.map { it[LAST_UPDATE_CHECK_AT] ?: 0L }

    suspend fun setNotificationsResumeEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_RESUME_ENABLED] = enabled }
    }

    suspend fun setNotificationsQuietHours(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_QUIET_HOURS] = enabled }
    }

    suspend fun setLastResumeReminderAt(timestamp: Long) {
        dataStore.edit { it[LAST_RESUME_REMINDER_AT] = timestamp }
    }

    suspend fun setLastUpdateCheckAt(timestamp: Long) {
        dataStore.edit { it[LAST_UPDATE_CHECK_AT] = timestamp }
    }

    // --- Сторонние озвучки и провайдеры ------------------------------------

    val kodikCustomToken: Flow<String> = dataStore.data.map { it[KODIK_CUSTOM_TOKEN] ?: "" }
    val globalPreferredVoiceover: Flow<String> = dataStore.data.map { it[GLOBAL_PREFERRED_VOICEOVER] ?: "AniLibria" }
    val aniskipEnabled: Flow<Boolean> = dataStore.data.map { it[ANISKIP_ENABLED] ?: true }
    val consumetEnabled: Flow<Boolean> = dataStore.data.map { it[CONSUMET_ENABLED] ?: true }
    val nyaaEnabled: Flow<Boolean> = dataStore.data.map { it[NYAA_ENABLED] ?: true }

    fun getTitleVoiceover(titleId: Long): Flow<String?> = dataStore.data.map { prefs ->
        val raw = prefs[TITLE_VOICEOVER_MAP] ?: return@map null
        val entries = raw.split(";").mapNotNull { entry ->
            val parts = entry.split("=")
            if (parts.size == 2) parts[0].toLongOrNull() to parts[1] else null
        }.toMap()
        entries[titleId]
    }

    suspend fun setTitleVoiceover(titleId: Long, voiceoverId: String?) {
        dataStore.edit { prefs ->
            val raw = prefs[TITLE_VOICEOVER_MAP] ?: ""
            val entries = raw.split(";").mapNotNull { entry ->
                val parts = entry.split("=")
                if (parts.size == 2) parts[0].toLongOrNull() to parts[1] else null
            }.filter { it.first != null }.map { it.first!! to it.second }.toMap().toMutableMap()

            if (voiceoverId != null) {
                entries[titleId] = voiceoverId
            } else {
                entries.remove(titleId)
            }

            prefs[TITLE_VOICEOVER_MAP] = entries.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }

    suspend fun setKodikCustomToken(token: String) {
        dataStore.edit { it[KODIK_CUSTOM_TOKEN] = token.trim() }
    }

    suspend fun setGlobalPreferredVoiceover(voiceover: String) {
        dataStore.edit { it[GLOBAL_PREFERRED_VOICEOVER] = voiceover.trim() }
    }

    suspend fun setAniSkipEnabled(enabled: Boolean) {
        dataStore.edit { it[ANISKIP_ENABLED] = enabled }
    }

    suspend fun setConsumetEnabled(enabled: Boolean) {
        dataStore.edit { it[CONSUMET_ENABLED] = enabled }
    }

    suspend fun setNyaaEnabled(enabled: Boolean) {
        dataStore.edit { it[NYAA_ENABLED] = enabled }
    }
}
