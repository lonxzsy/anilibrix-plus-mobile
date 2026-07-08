package com.anilibrix.plus.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.anilibrix.plus.BuildConfig
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.GitHubRepository
import kotlinx.coroutines.flow.first

@Composable
fun UpdateSnackbarEffect(
    settingsDataStore: SettingsDataStore,
    gitHubRepository: GitHubRepository,
    snackbarHostState: SnackbarHostState,
    onViewChangelog: () -> Unit
) {
    var checked by remember { mutableStateOf(false) }

    LaunchedEffect(checked) {
        if (checked) return@LaunchedEffect
        checked = true

        val lastSeen = settingsDataStore.lastSeenVersion.first()
        var latestVersion = ""

        gitHubRepository.getReleases().collect { result ->
            if (result is NetworkResult.Success && result.data.isNotEmpty()) {
                latestVersion = result.data.first().tagName
            }
        }

        val currentVersion = BuildConfig.VERSION_NAME
        if (latestVersion.isNotBlank() && latestVersion != currentVersion) {
            if (lastSeen != latestVersion) {
                snackbarHostState.showSnackbar(
                    message = "Доступно обновление $latestVersion",
                    actionLabel = "Посмотреть",
                    duration = SnackbarDuration.Indefinite
                )
                settingsDataStore.setLastSeenVersion(latestVersion)
            }
        }
    }
}
