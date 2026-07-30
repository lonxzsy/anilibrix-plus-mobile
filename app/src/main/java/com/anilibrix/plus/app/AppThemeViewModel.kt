package com.anilibrix.plus.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AppThemeState(
    val mode: ThemeMode,
    val dynamicColor: Boolean,
)

/**
 * Единственный источник правды об оформлении для оболочки приложения.
 *
 * Раньше ProfileViewModel писал выбранную тему в DataStore, но её никто не
 * читал обратно: MainActivity вызывал `AnilibrixTheme { }` без аргументов,
 * поэтому переключатель «Тёмная тема» вообще ни на что не влиял.
 *
 * `null` до первой загрузки — на это значение опирается splash screen,
 * чтобы первый кадр отрисовался уже с правильной темой, без вспышки.
 */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    settings: SettingsDataStore,
) : ViewModel() {

    val themeState: StateFlow<AppThemeState?> =
        combine(settings.theme, settings.dynamicColor) { theme, dynamic ->
            AppThemeState(
                mode = ThemeMode.fromStorage(theme),
                dynamicColor = dynamic,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )
}
