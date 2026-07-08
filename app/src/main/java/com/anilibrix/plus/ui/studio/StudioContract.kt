package com.anilibrix.plus.ui.studio

import com.anilibrix.plus.domain.model.StudioEpisode
import com.anilibrix.plus.domain.model.StudioResult
import com.anilibrix.plus.domain.model.StudioVideo

data class StudioSearchUiState(
    val query: String = "",
    val selectedSources: Set<String> = emptySet(),
    val results: Map<String, List<StudioResult>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false
)

data class StudioEpisodesUiState(
    val source: String = "",
    val animeId: String = "",
    val title: String = "",
    val episodes: List<StudioEpisode> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class StudioPlayerUiState(
    val videoUrl: String = "",
    val videos: List<StudioVideo> = emptyList(),
    val selectedQuality: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val retryNonce: Int = 0,
    val isPlaying: Boolean = true,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val speed: Float = 1.0f,
    val showControls: Boolean = true
)

sealed class StudioSearchIntent {
    data class UpdateQuery(val query: String) : StudioSearchIntent()
    data class ToggleSource(val source: String) : StudioSearchIntent()
    data object Search : StudioSearchIntent()
}

sealed class StudioEpisodesIntent {
    data class LoadEpisodes(val source: String, val id: String, val title: String) : StudioEpisodesIntent()
}

sealed class StudioPlayerIntent {
    data object PlayPause : StudioPlayerIntent()
    data class SeekTo(val position: Long) : StudioPlayerIntent()
    data class SetSpeed(val speed: Float) : StudioPlayerIntent()
    data class SetQuality(val quality: String?) : StudioPlayerIntent()
    data object ToggleControls : StudioPlayerIntent()
    data object HideControls : StudioPlayerIntent()
    data object Retry : StudioPlayerIntent()
    data class ShowPlaybackError(val message: String) : StudioPlayerIntent()
    data class UpdatePosition(val position: Long) : StudioPlayerIntent()
    data class UpdateDuration(val duration: Long) : StudioPlayerIntent()
}
