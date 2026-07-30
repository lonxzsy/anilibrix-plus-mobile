package com.anilibrix.plus.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.anilibrix.plus.core.download.DownloadRepository
import com.anilibrix.plus.core.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Решает, стоит ли открыть приложение сразу на «Загрузках».
 *
 * Без сети главный экран показывает пустоту и ошибку загрузки — при том, что
 * скачанные серии лежат рядом и готовы к просмотру. Это ровно та ситуация,
 * ради которой офлайн и качают: в дороге, в самолёте, в метро.
 *
 * Переход происходит **один раз за запуск** и только если скачанное
 * действительно есть. Иначе человек, у которого просто моргнула сеть,
 * оказывался бы на пустом экране загрузок.
 */
@UnstableApi
@HiltViewModel
class OfflineStartViewModel @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val _shouldOpenDownloads = MutableStateFlow(false)
    val shouldOpenDownloads: StateFlow<Boolean> = _shouldOpenDownloads.asStateFlow()

    init {
        viewModelScope.launch {
            val online = runCatching { networkMonitor.isOnline.first() }.getOrDefault(true)
            if (online) return@launch

            val hasDownloads = runCatching {
                downloadRepository.summary.first().completed.isNotEmpty()
            }.getOrDefault(false)

            _shouldOpenDownloads.value = hasDownloads
        }
    }

    fun consume() {
        _shouldOpenDownloads.value = false
    }
}
