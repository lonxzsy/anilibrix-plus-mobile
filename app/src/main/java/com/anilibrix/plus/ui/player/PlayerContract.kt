package com.anilibrix.plus.ui.player

import com.anilibrix.plus.domain.model.Episode

data class PlayerUiState(
    val currentEpisode: Episode? = null,
    val allEpisodes: List<Episode> = emptyList(),
    val titleName: String = "",
    val titleId: Long = 0L,
    val posterUrl: String? = null,
    val quality: String = "1080",
    val speed: Float = 1.0f,
    val isPlaying: Boolean = true,
    val currentPosition: Long = 0L,
    val seekPosition: Long = -1L,
    val duration: Long = 0L,
    val isLoading: Boolean = true,
    val skipOpening: SkipState = SkipState(),
    val skipEnding: SkipState = SkipState(),
    val autoAdvance: AutoAdvanceState = AutoAdvanceState(),
    val showControls: Boolean = true,
    val isPiP: Boolean = false,
    val isFullscreen: Boolean = true,
    val subtitlesEnabled: Boolean = false,
    val subtitleText: String = "",
    val subtitleCues: List<SubtitleCue> = emptyList(),
    val bufferedPercentage: Int = 0,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false
)

data class SkipState(
    val active: Boolean = false,
    val remainingSeconds: Int = 3,
    val progress: Float = 0f
)

data class AutoAdvanceState(
    val active: Boolean = false,
    val remainingSeconds: Int = 5,
    val nextEpisode: Episode? = null
)

data class SubtitleCue(
    val startTime: Long,
    val endTime: Long,
    val text: String
)

sealed class PlayerIntent {
    data object PlayPause : PlayerIntent()
    data class SeekTo(val position: Long) : PlayerIntent()
    data class SeekRelative(val deltaMs: Long) : PlayerIntent()
    data class SetQuality(val quality: String) : PlayerIntent()
    data class SetSpeed(val speed: Float) : PlayerIntent()
    data object ToggleControls : PlayerIntent()
    data object HideControls : PlayerIntent()
    data object ShowControls : PlayerIntent()
    data class SaveProgress(val position: Long, val duration: Long) : PlayerIntent()
    data object SkipOpening : PlayerIntent()
    data object SkipEnding : PlayerIntent()
    data object DismissAutoAdvance : PlayerIntent()
    data object SkipAutoAdvance : PlayerIntent()
    data object ToggleFullscreen : PlayerIntent()
    data object StartPiP : PlayerIntent()
    data object StopPiP : PlayerIntent()
    data class UpdatePosition(val position: Long) : PlayerIntent()
    data class UpdateDuration(val duration: Long) : PlayerIntent()
    data class UpdateBuffered(val percentage: Int) : PlayerIntent()
    data object ToggleSubtitles : PlayerIntent()
    data class SetVolume(val volume: Float) : PlayerIntent()
    data object ToggleMute : PlayerIntent()
    data class SkipToNext(val episode: Episode) : PlayerIntent()
    data class SetSubtitleCues(val cues: List<SubtitleCue>) : PlayerIntent()
    data object OnVideoEnded : PlayerIntent()
}
