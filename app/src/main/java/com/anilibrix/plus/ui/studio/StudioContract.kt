package com.anilibrix.plus.ui.studio

import com.anilibrix.plus.domain.model.StudioEpisode
import com.anilibrix.plus.domain.model.StudioResult

data class StudioSearchUiState(
    val query: String = "",
    val selectedSources: Set<String> = emptySet(),
    val results: Map<String, List<StudioResult>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
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
    val isLoading: Boolean = true,
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
    data object ToggleControls : StudioPlayerIntent()
    data object HideControls : StudioPlayerIntent()
    data class UpdatePosition(val position: Long) : StudioPlayerIntent()
    data class UpdateDuration(val duration: Long) : StudioPlayerIntent()
}
