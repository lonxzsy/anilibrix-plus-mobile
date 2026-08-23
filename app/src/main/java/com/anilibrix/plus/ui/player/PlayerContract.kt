package com.anilibrix.plus.ui.player

import androidx.compose.runtime.Immutable
import com.anilibrix.plus.core.util.SubtitleCue
import com.anilibrix.plus.domain.model.Episode

@Immutable
enum class AspectRatioMode(val label: String) {
    FIT("По размеру"),
    CROP("Заполнить"),
    STRETCH_16_9("16:9"),
    ZOOM("Увеличить")
}

@Immutable
data class PlayerUiState(
    val currentEpisode: Episode? = null,
    val allEpisodes: List<Episode> = emptyList(),
    val titleName: String = "",
    val titleId: Long = 0L,
    val posterUrl: String? = null,
    val quality: String = "1080",
    val availableQualities: List<String> = emptyList(),
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
    val isTouchLocked: Boolean = false,
    val brightness: Float = 0.5f,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val audioDelayMs: Long = 0L,
    val subtitleDelayMs: Long = 0L,
    val subtitlesEnabled: Boolean = false,
    val subtitleSizeSp: Int = 24,
    val subtitleColorHex: String = "#FFFFFF",
    val subtitleText: String = "",
    val subtitleCues: List<SubtitleCue> = emptyList(),
    val bufferedPercentage: Int = 0,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isBuffering: Boolean = false,
    val retryNonce: Int = 0,
    val playbackError: String? = null,
    /** Открыта шторка выбора качества, скорости и субтитров. */
    val showTracksSheet: Boolean = false,
    /**
     * Текстовые дорожки, найденные в самом HLS-манифесте.
     *
     * Anilibria отдельных субтитровых дорожек не отдаёт — в ответе API только
     * `hls_480/720/1080`. Поэтому список чаще всего пуст, и рабочий путь —
     * внешний файл. Это ограничение источника, а не приложения.
     */
    val subtitleTracks: List<PlayerSubtitleTrack> = emptyList(),
    val selectedSubtitleTrackId: String? = null,
    /** Имя загруженного пользователем файла субтитров, если он есть. */
    val externalSubtitleName: String? = null,
    /**
     * Только звук.
     *
     * Плеер живёт в сервисе, поэтому «выключить картинку» — это буквально не
     * рисовать видеоповерхность: декодирование видео прекращается, экран
     * можно погасить, а звук продолжает идти. Полезнее, чем кажется: дабы
     * часто слушают в дороге.
     */
    val audioOnly: Boolean = false,
)

@Immutable
data class PlayerSubtitleTrack(
    val id: String,
    val label: String,
    val language: String?,
)

@Immutable
data class SkipState(
    val active: Boolean = false,
    val remainingSeconds: Int = 3,
    /**
     * Доля отсчёта до автопропуска, 0..1. Гонит кольцо вокруг счётчика.
     *
     * Поле существовало и раньше, но не читалось нигде: кнопка показывала
     * только цифру, и было непонятно, дёргаться ли — она нажимается сама или
     * ждёт решения.
     */
    val progress: Float = 0f,
    /** `false` в режиме «Спрашивать»: отсчёта нет, кнопка просто висит. */
    val autoSkip: Boolean = true,
)

/**
 * Что делать с опенингом и эндингом.
 *
 * Раньше выбора не было: приложение всегда пропускало автоматически через три
 * секунды. Для тех, кто опенинги смотрит, это выглядело как самовольство.
 */
enum class SkipMode(val storageValue: String, val displayName: String) {
    ASK("ask", "Спрашивать"),
    AUTO("auto", "Пропускать автоматически"),
    NEVER("never", "Никогда");

    companion object {
        fun fromStorage(value: String): SkipMode =
            entries.find { it.storageValue == value } ?: ASK
    }
}

@Immutable
data class AutoAdvanceState(
    val active: Boolean = false,
    val remainingSeconds: Int = 5,
    val nextEpisode: Episode? = null
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
    /** Синхронизация с системой: из PiP выходят в том числе жестом. */
    data class SetPiP(val active: Boolean) : PlayerIntent()
    data object StopPiP : PlayerIntent()
    data class UpdatePosition(val position: Long) : PlayerIntent()
    data class UpdateDuration(val duration: Long) : PlayerIntent()
    data class UpdateBuffered(val percentage: Int) : PlayerIntent()
    data object ToggleSubtitles : PlayerIntent()
    data class SetSubtitleSize(val sizeSp: Int) : PlayerIntent()
    data class SetSubtitleColor(val colorHex: String) : PlayerIntent()
    data class SetVolume(val volume: Float) : PlayerIntent()
    data object ToggleMute : PlayerIntent()
    data object ToggleAudioOnly : PlayerIntent()
    data class SkipToNext(val episode: Episode) : PlayerIntent()
    data class SetSubtitleCues(val cues: List<SubtitleCue>) : PlayerIntent()
    data class SetBuffering(val isBuffering: Boolean) : PlayerIntent()
    data class ShowPlaybackError(val message: String) : PlayerIntent()
    data object ClearPlaybackError : PlayerIntent()
    data object RetryPlayback : PlayerIntent()
    data object OnVideoEnded : PlayerIntent()
    data object SeekComplete : PlayerIntent()
    data object ShowTracksSheet : PlayerIntent()
    data object DismissTracksSheet : PlayerIntent()
    data class SetSubtitleTracks(val tracks: List<PlayerSubtitleTrack>) : PlayerIntent()
    data class SelectSubtitleTrack(val trackId: String?) : PlayerIntent()
    data class LoadExternalSubtitles(val name: String, val cues: List<SubtitleCue>) : PlayerIntent()
    data class SetBrightness(val brightness: Float) : PlayerIntent()
    data object ToggleTouchLock : PlayerIntent()
    data class SetAspectRatio(val mode: AspectRatioMode) : PlayerIntent()
    data class SetAudioDelay(val offsetMs: Long) : PlayerIntent()
    data class SetSubtitleDelay(val offsetMs: Long) : PlayerIntent()
}
