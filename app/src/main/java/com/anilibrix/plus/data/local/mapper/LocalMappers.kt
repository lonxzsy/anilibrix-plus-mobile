package com.anilibrix.plus.data.local.mapper

import com.anilibrix.plus.core.database.entity.FavoriteEntity
import com.anilibrix.plus.core.database.entity.HistoryEntity
import com.anilibrix.plus.core.database.entity.PlaylistEntity
import com.anilibrix.plus.core.database.entity.PlaylistItemEntity
import com.anilibrix.plus.core.database.entity.RatingEntity
import com.anilibrix.plus.core.database.entity.WatchLaterEntity
import com.anilibrix.plus.domain.model.FavoriteTitle
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.Playlist
import com.anilibrix.plus.domain.model.PlaylistItem

fun FavoriteEntity.toDomain(): FavoriteTitle = FavoriteTitle(
    titleId = titleId,
    titleName = titleName,
    posterUrl = posterUrl
)

fun HistoryEntity.toDomain(): HistoryEntry = HistoryEntry(
    titleId = titleId,
    titleName = titleName,
    posterUrl = posterUrl,
    episodeId = episodeId,
    episodeNumber = episodeNumber,
    timestamp = timestamp,
    duration = duration,
    watchedAt = watchedAt
)

fun HistoryEntry.toEntity(): HistoryEntity = HistoryEntity(
    titleId = titleId,
    episodeId = episodeId,
    episodeNumber = episodeNumber,
    timestamp = timestamp,
    duration = duration,
    watchedAt = watchedAt,
    titleName = titleName,
    posterUrl = posterUrl
)

fun PlaylistEntity.toDomain(items: List<PlaylistItem>): Playlist = Playlist(
    id = id,
    name = name,
    createdAt = createdAt,
    items = items
)

fun PlaylistItemEntity.toDomain(): PlaylistItem = PlaylistItem(
    playlistId = playlistId,
    titleId = titleId,
    titleName = titleName
)

fun PlaylistItem.toEntity(): PlaylistItemEntity = PlaylistItemEntity(
    playlistId = playlistId,
    titleId = titleId,
    titleName = titleName
)

fun WatchLaterEntity.toDomain(): com.anilibrix.plus.domain.model.FavoriteTitle = FavoriteTitle(
    titleId = titleId,
    titleName = titleName,
    posterUrl = posterUrl
)

fun RatingEntity.toDomain(): Pair<Long, Float> = titleId to rating
