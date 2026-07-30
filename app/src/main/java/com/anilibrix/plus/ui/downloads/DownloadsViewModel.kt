package com.anilibrix.plus.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.anilibrix.plus.core.download.DownloadItem
import com.anilibrix.plus.core.download.DownloadRepository
import com.anilibrix.plus.core.download.DownloadSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    val summary: StateFlow<DownloadSummary> = downloadRepository.summary
        // Ошибка чтения индекса Media3 не должна ронять экран: пустой список
        // честнее, чем падение при повреждённом кэше.
        .catch { emit(DownloadSummary()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadSummary())

    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused.asStateFlow()

    fun onIntent(intent: DownloadsIntent) {
        when (intent) {
            is DownloadsIntent.Remove -> downloadRepository.remove(intent.requestId)
            is DownloadsIntent.Resume -> downloadRepository.resume(intent.requestId)
            DownloadsIntent.TogglePauseAll -> {
                val next = !_paused.value
                _paused.value = next
                downloadRepository.setPaused(next)
            }
            DownloadsIntent.RemoveAll -> {
                downloadRepository.removeAll()
                _paused.value = false
            }
            DownloadsIntent.Refresh -> viewModelScope.launch {
                runCatching { downloadRepository.applyRequirements() }
            }
        }
    }
}

sealed interface DownloadsIntent {
    data class Remove(val requestId: String) : DownloadsIntent
    data class Resume(val requestId: String) : DownloadsIntent
    data object TogglePauseAll : DownloadsIntent
    data object RemoveAll : DownloadsIntent
    data object Refresh : DownloadsIntent
}

/** Загрузки одного тайтла — так экран группирует список. */
data class DownloadGroup(
    val titleId: Long,
    val titleName: String,
    val posterUrl: String?,
    val items: List<DownloadItem>,
)

fun List<DownloadItem>.groupByTitle(): List<DownloadGroup> =
    groupBy { it.titleId }
        .map { (titleId, items) ->
            DownloadGroup(
                titleId = titleId,
                titleName = items.first().titleName,
                posterUrl = items.first().posterUrl,
                items = items.sortedBy { it.episodeNumber },
            )
        }
        .sortedBy { it.titleName }
