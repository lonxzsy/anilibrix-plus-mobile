package com.anilibrix.plus.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val AUTH_LOGIN = stringPreferencesKey("auth_login")
        private val THEME = stringPreferencesKey("theme")
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
    }

    val authToken: Flow<String?> = dataStore.data.map { it[AUTH_TOKEN] }
    val authLogin: Flow<String?> = dataStore.data.map { it[AUTH_LOGIN] }
    val theme: Flow<String> = dataStore.data.map { it[THEME] ?: "dark" }
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
        dataStore.edit { if (token != null) it[AUTH_TOKEN] = token else it.remove(AUTH_TOKEN) }
    }

    suspend fun setAuthLogin(login: String?) {
        dataStore.edit { if (login != null) it[AUTH_LOGIN] = login else it.remove(AUTH_LOGIN) }
    }

    suspend fun setTheme(theme: String) {
        dataStore.edit { it[THEME] = theme }
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
}
