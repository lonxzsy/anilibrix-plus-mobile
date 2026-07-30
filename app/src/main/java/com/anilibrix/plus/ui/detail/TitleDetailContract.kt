package com.anilibrix.plus.ui.detail

import androidx.compose.runtime.Immutable
import com.anilibrix.plus.core.download.DownloadItem
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.Episode
import com.anilibrix.plus.domain.model.FranchiseItem
import com.anilibrix.plus.domain.model.MalAnime
import com.anilibrix.plus.domain.model.MalCharacter
import com.anilibrix.plus.domain.model.Playlist
import com.anilibrix.plus.domain.model.ResumeTarget
import com.anilibrix.plus.domain.model.ShikimoriAnime
import com.anilibrix.plus.domain.model.ShikimoriScreenshot
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.domain.model.TitleProgress
import com.anilibrix.plus.domain.model.Torrent

enum class DetailTab(val displayName: String) {
    DESCRIPTION("Описание"),
    EPISODES("Эпизоды"),
    CHARACTERS("Персонажи"),
    STATISTICS("Статистика"),
    RELATED("Связанное"),
    RECOMMENDATIONS("Рекомендации"),
    TORRENTS("Торренты")
}

enum class DetailDataSource {
    ANILIBRIA,
    JIKAN,
    SHIKIMORI
}

@Immutable
data class DetailCharacterItem(
    val id: String,
    val name: String,
    val role: String?,
    val imageUrl: String?,
    val seiyuuName: String?,
    val seiyuuImageUrl: String?,
    val source: DetailDataSource,
    val malId: Long?
)

@Immutable
data class RelatedTitleItem(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val relation: String?,
    val source: DetailDataSource,
    val anilibriaId: Long?
)

@Immutable
data class DetailUiState(
    val loading: Boolean = true,
    val title: Title? = null,
    val franchise: List<FranchiseItem> = emptyList(),
    val characters: List<MalCharacter> = emptyList(),
    val torrents: List<Torrent> = emptyList(),
    val isFavorite: Boolean = false,
    val isInWatchLater: Boolean = false,
    /** Текущий статус трекера; `null` — тайтл ни в одном списке. */
    val collectionStatus: CollectionType? = null,
    val showStatusSheet: Boolean = false,
    /** Отметки о просмотре и позиции по каждой серии. */
    val progress: TitleProgress = TitleProgress(titleId = 0L),
    /** Куда ведёт кнопка «Смотреть»/«Продолжить». */
    val resumeTarget: ResumeTarget? = null,
    /** Состояние загрузок по сериям этого тайтла; ключ — `episodeId`. */
    val downloads: Map<Long, DownloadItem> = emptyMap(),
    val userRating: Float = 0f,
    val playlists: List<Playlist> = emptyList(),
    val playlistIdsForTitle: Set<Long> = emptySet(),
    val showPlaylistDialog: Boolean = false,
    val selectedTab: DetailTab = DetailTab.DESCRIPTION,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val malDetails: MalAnime? = null,
    val statistics: Map<Int, Int> = emptyMap(),
    val malRecommendations: List<MalAnime> = emptyList(),
    val recommendedTitles: List<Title> = emptyList(),
    val externalLoading: Boolean = false,
    val charactersLoading: Boolean = false,
    val statisticsLoading: Boolean = false,
    val relatedLoading: Boolean = false,
    val characterItems: List<DetailCharacterItem> = emptyList(),
    val relatedItems: List<RelatedTitleItem> = emptyList(),
    val malId: Long? = null,
    val shikimoriDetails: ShikimoriAnime? = null,
    val shikimoriId: Int? = null,
    /** Кадры из аниме; лента под описанием. */
    val screenshots: List<ShikimoriScreenshot> = emptyList(),
    /** Индекс кадра, открытого на весь экран; `null` — просмотрщик закрыт. */
    val fullscreenScreenshot: Int? = null,
    val externalErrors: Map<DetailDataSource, String> = emptyMap(),
    val debugMessages: List<String> = emptyList()
)

sealed interface DetailIntent {
    data class Load(val id: String) : DetailIntent
    data class SelectTab(val tab: DetailTab) : DetailIntent
    data class SetRating(val rating: Float) : DetailIntent
    data object ToggleFavorite : DetailIntent
    data object ToggleWatchLater : DetailIntent
    data object ShowStatusSheet : DetailIntent
    data object DismissStatusSheet : DetailIntent
    data class SetCollectionStatus(val status: CollectionType) : DetailIntent
    data object ClearCollectionStatus : DetailIntent
    data class ToggleEpisodeWatched(val episode: Episode) : DetailIntent
    /** Отметить просмотренными все серии до указанной включительно. */
    data class MarkWatchedUpTo(val episode: Episode) : DetailIntent
    data class DownloadEpisode(val episode: Episode) : DetailIntent
    data class CancelDownload(val episode: Episode) : DetailIntent
    /** Скачать следующие непросмотренные серии — сколько указано. */
    data class DownloadNext(val count: Int) : DetailIntent
    data object ShowPlaylistDialog : DetailIntent
    data object DismissPlaylistDialog : DetailIntent
    data class TogglePlaylistMembership(val playlistId: Long) : DetailIntent
    data class PlayEpisode(val episode: Episode) : DetailIntent
    data class OpenMagnet(val magnet: String) : DetailIntent
    data class OpenScreenshot(val index: Int) : DetailIntent
    data object CloseScreenshot : DetailIntent
}
