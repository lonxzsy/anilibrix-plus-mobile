package com.anilibrix.plus.core.network

import com.anilibrix.plus.app.di.ApplicationScope
import com.anilibrix.plus.core.datastore.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthTokenProvider @Inject constructor(
    settingsDataStore: SettingsDataStore,
    @ApplicationScope applicationScope: CoroutineScope
) {
    @Volatile
    private var token: String? = null

    init {
        applicationScope.launch {
            settingsDataStore.authToken
                .distinctUntilChanged()
                .collect { latestToken ->
                    token = latestToken?.takeIf { it.isNotBlank() }
                }
        }
    }

    fun currentToken(): String? = token
}
