package com.anilibrix.plus.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.core.update.AppUpdate
import com.anilibrix.plus.core.update.AppUpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateCheckViewModel @Inject constructor(
    private val updateManager: AppUpdateManager,
    private val settings: SettingsDataStore,
) : ViewModel() {

    private val _update = MutableStateFlow<AppUpdate?>(null)
    val update: StateFlow<AppUpdate?> = _update.asStateFlow()

    fun check() {
        viewModelScope.launch {
            if (!settings.notificationsAppUpdatesEnabled.first()) return@launch

            val found = runCatching { updateManager.checkForUpdate() }.getOrNull() ?: return@launch
            // Одну и ту же версию не предлагаем дважды: человек мог осознанно
            // отложить обновление, и напоминать о нём при каждом запуске —
            // навязчиво.
            if (settings.lastSeenVersion.first() == found.version) return@launch
            _update.value = found
        }
    }

    fun accept() {
        val update = _update.value ?: return
        viewModelScope.launch {
            settings.setLastSeenVersion(update.version)
            if (update.canInstallInApp) {
                updateManager.download(update)
            } else {
                // APK к релизу не приложен — честно отправляем на страницу
                // релиза вместо тихого бездействия.
                updateManager.openReleasePage(update)
            }
            _update.value = null
        }
    }

    fun dismiss() {
        val update = _update.value ?: return
        viewModelScope.launch {
            settings.setLastSeenVersion(update.version)
            _update.value = null
        }
    }
}

/**
 * Предложение обновиться.
 *
 * Этот файл существовал и раньше, но не композился нигде: проверка обновлений
 * была написана и ни разу не выполнялась, а ссылка на APK выбрасывалась при
 * разборе ответа GitHub. Приложение распространяется не через магазин, поэтому
 * без этого о новой версии человек узнавал, только если сам заходил в
 * репозиторий.
 */
@Composable
fun UpdateSnackbarEffect(
    toastHostState: ToastHostState?,
    viewModel: UpdateCheckViewModel = hiltViewModel(),
) {
    val update by viewModel.update.collectAsStateWithLifecycle()
    var handledVersion by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.check() }

    LaunchedEffect(update?.version) {
        val found = update ?: return@LaunchedEffect
        val host = toastHostState ?: return@LaunchedEffect
        if (handledVersion == found.version) return@LaunchedEffect
        handledVersion = found.version

        val result = host.showAction(
            message = "Доступна версия ${found.version}",
            actionLabel = if (found.canInstallInApp) "Обновить" else "Открыть",
            type = ToastType.Info,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.accept() else viewModel.dismiss()
    }
}
