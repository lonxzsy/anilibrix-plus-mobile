package com.anilibrix.plus.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
    }

    val authToken: Flow<String?> = dataStore.data.map { it[AUTH_TOKEN] }
    val authLogin: Flow<String?> = dataStore.data.map { it[AUTH_LOGIN] }
    val theme: Flow<String> = dataStore.data.map { it[THEME] ?: "dark" }
    val preferredQuality: Flow<String> = dataStore.data.map { it[PREFERRED_QUALITY] ?: "1080" }
    val lastSeenVersion: Flow<String?> = dataStore.data.map { it[LAST_SEEN_VERSION] }
    val malIdMap: Flow<String?> = dataStore.data.map { it[MAL_ID_MAP] }

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
}
