package com.anilibrix.plus.ui.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import com.anilibrix.plus.core.download.DownloadItem
import com.anilibrix.plus.core.download.DownloadState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anilibrix.plus.ui.components.SectionHeader
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.RemoveDone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.mutableStateOf
import com.anilibrix.plus.domain.model.EpisodeProgress
import com.anilibrix.plus.ui.components.pressScale
import com.anilibrix.plus.ui.detail.components.CollectionStatusSheet
import com.anilibrix.plus.ui.detail.components.icon
import com.anilibrix.plus.ui.theme.Elevation
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anilibrix.plus.domain.model.Episode
import com.anilibrix.plus.ui.detail.DetailIntent
import com.anilibrix.plus.domain.model.FranchiseItem
import com.anilibrix.plus.domain.model.MalAnime
import com.anilibrix.plus.domain.model.ShikimoriAnime
import com.anilibrix.plus.domain.model.Torrent
import com.anilibrix.plus.ui.components.CharacterCard
import com.anilibrix.plus.ui.components.CharacterCardData
import com.anilibrix.plus.ui.components.ErrorView
import com.anilibrix.plus.ui.components.LoadingIndicator
import com.anilibrix.plus.ui.components.LocalToastHostState
import com.anilibrix.plus.ui.components.TitleCardList
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.ui.components.AnilibrixImage
import com.anilibrix.plus.ui.components.WatchFab
import com.anilibrix.plus.ui.components.rememberHaptics
import com.anilibrix.plus.ui.components.rememberIsScrollingUp
import com.anilibrix.plus.ui.navigation.LocalBottomBarHeight
import com.anilibrix.plus.ui.theme.AnilibrixShapeExtras
import com.anilibrix.plus.ui.theme.AnilibrixTypeExtras
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing
import com.anilibrix.plus.ui.theme.extended
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import com.anilibrix.plus.ui.theme.MotionTokens
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleDetailScreen(
    id: String,
    viewModel: TitleDetailViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onPlayEpisode: (Long, Long) -> Unit = { _, _ -> },
    onCharacterClick: (Long) -> Unit = {},
    onTitleClick: (Long) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val toastHostState = LocalToastHostState.current
    val haptics = rememberHaptics()
    val listState = rememberLazyListState()
    val isScrollingUp by rememberIsScrollingUp(listState)

    val density = LocalDensity.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerMax = Sizing.detailHeaderHeight
    val tabRowHeight = Sizing.tabRowHeight
    val maxCollapsePx = with(density) {
        (headerMax - (TOOLBAR_HEIGHT + statusBarPadding)).toPx().coerceAtLeast(1f)
    }

    var headerOffset by remember { mutableFloatStateOf(0f) }
    // derivedStateOf — чтобы производная величина пересчитывалась только при
    // реальном изменении смещения, а не на каждую рекомпозицию.
    val collapseFraction by remember(maxCollapsePx) {
        derivedStateOf { (-headerOffset / maxCollapsePx).coerceIn(0f, 1f) }
    }

    /**
     * Хедер сворачивается ПЕРВЫМ, и только остаток прокрутки уходит в список.
     *
     * Раньше здесь был `TopAppBarDefaults.pinnedScrollBehavior()`, который
     * не сворачивается по определению, и вдобавок его nestedScrollConnection
     * никуда не подключался — то есть код был мёртв дважды. Сам бэкдроп лежал
     * СОСЕДОМ списка внутри Column, поэтому физически не мог уехать.
     */
    val headerConnection = remember(maxCollapsePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0f) return Offset.Zero
                val next = (headerOffset + available.y).coerceIn(-maxCollapsePx, 0f)
                val used = next - headerOffset
                headerOffset = next
                return Offset(0f, used)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (available.y <= 0f) return Offset.Zero
                val next = (headerOffset + available.y).coerceIn(-maxCollapsePx, 0f)
                val used = next - headerOffset
                headerOffset = next
                return Offset(0f, used)
            }
        }
    }

    LaunchedEffect(id) {
        viewModel.onIntent(DetailIntent.Load(id))
    }

    Scaffold(
        // Хедер должен уходить под системные бары — иначе никакого edge-to-edge.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.title?.name?.main ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // Заголовок проявляется во второй половине сворачивания,
                        // когда название в самом хедере уже ушло.
                        modifier = Modifier.graphicsLayer {
                            alpha = ((collapseFraction - 0.45f) * 2.2f).coerceIn(0f, 1f)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    if (state.title != null) {
                        IconButton(
                            onClick = {
                                val willAdd = !state.isFavorite
                                viewModel.onIntent(DetailIntent.ToggleFavorite)
                                if (willAdd) {
                                    toastHostState?.showSuccess("Добавлено в избранное")
                                } else {
                                    toastHostState?.showInfo("Удалено из избранного")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (state.isFavorite) Icons.Default.Favorite
                                else Icons.Default.FavoriteBorder,
                                contentDescription = if (state.isFavorite) "Удалить из избранного"
                                else "Добавить в избранное",
                                tint = if (state.isFavorite) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // Одна кнопка на все пять статусов вместо прежнего
                        // тумблера «Буду смотреть»: остальные четыре статуса
                        // существовали в модели и на сервере, но открыть их
                        // из интерфейса было нельзя.
                        IconButton(
                            onClick = { viewModel.onIntent(DetailIntent.ShowStatusSheet) }
                        ) {
                            Icon(
                                imageVector = state.collectionStatus.icon(),
                                contentDescription = state.collectionStatus
                                    ?.let { "Статус: ${it.displayName}" }
                                    ?: "Добавить в список",
                                tint = if (state.collectionStatus != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(
                        alpha = collapseFraction
                    )
                )
            )
        },
        floatingActionButton = {
            // Была иконочная кнопка без подписи, без реакции на скролл и без
            // анимации появления — читалась как «маленький квадрат непонятно чего».
            //
            // Теперь она ещё и знает, где человек остановился: подпись и цель
            // берутся из реального прогресса, а не «всегда первая серия».
            val episodes = state.title?.episodes
            val resume = state.resumeTarget
            WatchFab(
                label = when {
                    resume == null -> "Смотреть"
                    resume.isResume -> "Продолжить · Серия ${resume.episode.ordinal}"
                    resume.episode.ordinal > 1 -> "Серия ${resume.episode.ordinal}"
                    else -> "Смотреть"
                },
                expanded = isScrollingUp,
                visible = !episodes.isNullOrEmpty(),
                onClick = {
                    val target = resume?.episode ?: episodes?.firstOrNull() ?: return@WatchFab
                    haptics.confirm()
                    onPlayEpisode(state.title!!.id, target.id)
                },
            )
        }
    ) { padding ->
        when {
            state.loading -> {
                LoadingIndicator(modifier = Modifier.padding(padding))
            }
            state.error != null && state.title == null -> {
                ErrorView(
                    message = state.error ?: "Ошибка загрузки",
                    onRetry = { viewModel.onIntent(DetailIntent.Load(id)) }
                )
            }
            state.title != null -> {
                val title = state.title!!

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(headerConnection)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            // Сверху резервируем место под хедер: он не сосед
                            // списка, а слой НАД ним.
                            contentPadding = PaddingValues(
                                start = Spacing.screenHorizontal,
                                end = Spacing.screenHorizontal,
                                top = headerMax + tabRowHeight,
                                bottom = LocalBottomBarHeight.current + Spacing.xxxl * 2,
                            )
                        ) {
                            when (state.selectedTab) {
                                DetailTab.DESCRIPTION -> {
                                    item {
                                        if (!title.description.isNullOrBlank()) {
                                            Text(
                                                text = title.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Кадры из аниме. Данные приходят от
                                    // Shikimori; если тайтл там не нашёлся,
                                    // секции просто нет.
                                    if (state.screenshots.isNotEmpty()) {
                                        item {
                                            Spacer(modifier = Modifier.height(Spacing.lg))
                                            SectionHeader(title = "Кадры", horizontalPadding = Spacing.none)
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                            ) {
                                                itemsIndexed(
                                                    items = state.screenshots,
                                                    key = { _, shot -> shot.preview ?: shot.original ?: "" },
                                                ) { index, shot ->
                                                    AnilibrixImage(
                                                        model = shot.preview ?: shot.original,
                                                        contentDescription = "Кадр ${index + 1}",
                                                        modifier = Modifier
                                                            .width(Sizing.railCardWidth)
                                                            .aspectRatio(16f / 9f)
                                                            .pressScale()
                                                            .clickable {
                                                                viewModel.onIntent(
                                                                    DetailIntent.OpenScreenshot(index)
                                                                )
                                                            },
                                                        shape = MaterialTheme.shapes.medium,
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Жанры",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            title.genres.forEach { genre ->
                                                Card(
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                                    )
                                                ) {
                                                    Text(
                                                        text = genre.name,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        modifier = Modifier.padding(
                                                            horizontal = 12.dp,
                                                            vertical = 4.dp
                                                        ),
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Моя оценка",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        RatingSlider(
                                            rating = state.userRating,
                                            onRatingChange = {
                                                viewModel.onIntent(DetailIntent.SetRating(it))
                                            }
                                        )
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                viewModel.onIntent(DetailIntent.ShowPlaylistDialog)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Плейлисты")
                                        }
                                    }

                                    if (title.season != null) {
                                        item {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                DetailInfoChip("Сезон", "${title.season.name} ${title.season.year}")
                                                DetailInfoChip("Эпизодов", "${title.episodes?.size ?: 0} / ${title.episodesTotal}")
                                                if (title.isOngoing) {
                                                    DetailInfoChip("Статус", "Онгоинг")
                                                }
                                            }
                                        }
                                    }

                                    if (title.name.alternative != null) {
                                        item {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Альтернативное название: ${title.name.alternative}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }

                                DetailTab.EPISODES -> {
                                    val episodes = title.episodes ?: emptyList()
                                    if (episodes.isEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Нет доступных эпизодов",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    } else {
                                        items(
                                            items = episodes,
                                            key = { it.id }
                                        ) { episode ->
                                            EpisodeCard(
                                                episode = episode,
                                                progress = state.progress.progressOf(episode.id),
                                                download = state.downloads[episode.id],
                                                onClick = { onPlayEpisode(title.id, episode.id) },
                                                onDownload = {
                                                    viewModel.onIntent(DetailIntent.DownloadEpisode(episode))
                                                    toastHostState?.showInfo("Серия ${episode.ordinal} в очереди загрузки")
                                                },
                                                onCancelDownload = {
                                                    viewModel.onIntent(DetailIntent.CancelDownload(episode))
                                                },
                                                onToggleWatched = {
                                                    viewModel.onIntent(
                                                        DetailIntent.ToggleEpisodeWatched(episode)
                                                    )
                                                },
                                                onMarkUpTo = {
                                                    viewModel.onIntent(
                                                        DetailIntent.MarkWatchedUpTo(episode)
                                                    )
                                                    toastHostState?.showSuccess(
                                                        "Отмечено серий: ${episode.ordinal}"
                                                    )
                                                },
                                            )
                                            Spacer(modifier = Modifier.height(Spacing.sm))
                                        }
                                    }
                                }

                                DetailTab.CHARACTERS -> {
                                    when {
                                        state.charactersLoading -> {
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(32.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    LoadingIndicator()
                                                }
                                            }
                                        }
                                        state.characterItems.isEmpty() -> {
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(32.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "Нет данных о персонажах",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        }
                                        else -> {
                                            val chunked = state.characterItems.chunked(2)
                                            items(
                                                items = chunked,
                                                key = { it.first().id }
                                            ) { row ->
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    row.forEach { character ->
                                                        Box(modifier = Modifier.weight(1f)) {
                                                            CharacterCard(
                                                                data = CharacterCardData(
                                                                    id = character.id,
                                                                    imageUrl = character.imageUrl ?: "",
                                                                    name = character.name,
                                                                    role = character.role ?: "",
                                                                    seiyuuName = character.seiyuuName,
                                                                    seiyuuImageUrl = character.seiyuuImageUrl
                                                                ),
                                                                onClick = {
                                                                    character.malId?.let(onCharacterClick)
                                                                }
                                                            )
                                                        }
                                                    }
                                                    if (row.size == 1) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                DetailTab.RELATED -> {
                                    val hasRelated = state.franchise.isNotEmpty() || state.relatedItems.isNotEmpty()
                                    if (!hasRelated && state.relatedLoading) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                LoadingIndicator()
                                            }
                                        }
                                    } else if (!hasRelated) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Нет связанных тайтлов",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    } else {
                                        if (state.franchise.isNotEmpty()) {
                                            item {
                                                Text(
                                                    text = "Франшиза",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    state.franchise.forEach { franchiseItem ->
                                                        FranchiseCard(
                                                            item = franchiseItem,
                                                            onClick = { onTitleClick(franchiseItem.id) }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (state.relatedItems.isNotEmpty()) {
                                            item {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    text = "Связанное на Shikimori",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    state.relatedItems.forEach { relatedItem ->
                                                        RelatedTitleCard(
                                                            item = relatedItem,
                                                            onClick = {
                                                                relatedItem.anilibriaId?.let(onTitleClick)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                DetailTab.STATISTICS -> {
                                    val hasStatistics = state.malDetails != null ||
                                        state.statistics.isNotEmpty() ||
                                        state.shikimoriDetails != null

                                    if (!hasStatistics && state.statisticsLoading) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                LoadingIndicator()
                                            }
                                        }
                                    } else if (!hasStatistics) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Нет данных статистики",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    } else {
                                        item {
                                            StatisticsOverviewCard(
                                                malDetails = state.malDetails,
                                                shikimoriDetails = state.shikimoriDetails
                                            )
                                        }

                                        if (state.statistics.isNotEmpty()) {
                                            item {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                RatingDistributionCard(statistics = state.statistics)
                                            }
                                        }
                                    }
                                }

                                DetailTab.RECOMMENDATIONS -> {
                                    when {
                                        state.recommendedTitles.isNotEmpty() -> {
                                            items(
                                                items = state.recommendedTitles,
                                                key = { it.id }
                                            ) { recommendedTitle ->
                                                TitleCardList(
                                                    title = recommendedTitle,
                                                    onClick = { onTitleClick(recommendedTitle.id) },
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            }
                                        }
                                        state.malRecommendations.isNotEmpty() -> {
                                            items(
                                                items = state.malRecommendations,
                                                key = { it.malId }
                                            ) { rec ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (rec.imageUrl != null) {
                                                            GlideImage(
                                                                imageModel = { rec.imageUrl },
                                                                modifier = Modifier
                                                                    .size(48.dp)
                                                                    .clip(RoundedCornerShape(8.dp)),
                                                                imageOptions = ImageOptions(
                                                                    contentScale = ContentScale.Crop,
                                                                    contentDescription = rec.title
                                                                )
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text(
                                                                text = rec.title,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "Рекомендация MAL",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        else -> {
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(32.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "Нет рекомендаций",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                DetailTab.TORRENTS -> {
                                    if (state.torrents.isEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Нет доступных торрентов",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    } else {
                                        items(
                                            items = state.torrents,
                                            key = { it.id }
                                        ) { torrent ->
                                            TorrentCard(
                                                torrent = torrent,
                                                onMagnetClick = { magnet ->
                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                        data = Uri.parse(magnet)
                                                    }
                                                    context.startActivity(intent)
                                                }
                                            )
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        }
                                    }
                                }
                            }
                        }

                        DetailHeader(
                            title = title,
                            headerHeight = headerMax,
                            tabRowHeight = tabRowHeight,
                            headerOffset = { headerOffset },
                            collapseFraction = { collapseFraction },
                            selectedTab = state.selectedTab,
                            onTabSelected = { viewModel.onIntent(DetailIntent.SelectTab(it)) },
                        )
                    }
                }
            }
        }
    }

    // Полноэкранный просмотр кадра. Диалог, а не отдельный маршрут: это
    // модальный просмотр, из которого возвращаются назад, а не переход.
    state.fullscreenScreenshot?.let { index ->
        val shot = state.screenshots.getOrNull(index)
        if (shot != null) {
            Dialog(
                onDismissRequest = { viewModel.onIntent(DetailIntent.CloseScreenshot) },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.92f))
                        .clickable { viewModel.onIntent(DetailIntent.CloseScreenshot) },
                    contentAlignment = Alignment.Center,
                ) {
                    AnilibrixImage(
                        model = shot.original ?: shot.preview,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
    }

    if (state.showStatusSheet) {
        CollectionStatusSheet(
            current = state.collectionStatus,
            onSelect = { status ->
                viewModel.onIntent(DetailIntent.SetCollectionStatus(status))
                toastHostState?.showSuccess("Статус: ${status.displayName}")
            },
            onClear = {
                viewModel.onIntent(DetailIntent.ClearCollectionStatus)
                toastHostState?.showInfo("Убрано из списков")
            },
            onDismiss = { viewModel.onIntent(DetailIntent.DismissStatusSheet) },
        )
    }

    if (state.showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onIntent(DetailIntent.DismissPlaylistDialog)
            },
            title = { Text("Плейлисты") },
            text = {
                if (state.playlists.isEmpty()) {
                    Text("Создайте плейлист в библиотеке, чтобы добавить сюда тайтл.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.playlists.forEach { playlist ->
                            val checked = playlist.id in state.playlistIdsForTitle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.onIntent(
                                            DetailIntent.TogglePlaylistMembership(playlist.id)
                                        )
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        viewModel.onIntent(
                                            DetailIntent.TogglePlaylistMembership(playlist.id)
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "${playlist.items.size} элементов",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onIntent(DetailIntent.DismissPlaylistDialog)
                    }
                ) {
                    Text("Готово")
                }
            }
        )
    }
}

@Composable
private fun StatisticsOverviewCard(
    malDetails: MalAnime?,
    shikimoriDetails: ShikimoriAnime?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Общая статистика",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "MAL",
                    value = malDetails?.score?.toString() ?: "—",
                    subtitle = "оценка",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Ранг",
                    value = malDetails?.rank?.let { "#$it" } ?: "—",
                    subtitle = "MyAnimeList",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Популярность",
                    value = malDetails?.popularity?.let { "#$it" } ?: "—",
                    subtitle = "MAL",
                    modifier = Modifier.weight(1f)
                )
            }

            shikimoriDetails?.let { details ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "Shikimori",
                        value = details.score?.toString() ?: "—",
                        subtitle = "оценка",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Статус",
                        value = details.status ?: "—",
                        subtitle = details.kind ?: "тип неизвестен",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Эпизоды",
                        value = if (details.episodesAired > 0) {
                            "${details.episodesAired}/${details.episodes}"
                        } else {
                            details.episodes.toString()
                        },
                        subtitle = details.airedOn ?: "выпуск",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            malDetails?.synopsis?.takeIf { it.isNotBlank() }?.let { synopsis ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Описание MAL",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = synopsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RatingDistributionCard(statistics: Map<Int, Int>) {
    val maxVotes = statistics.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val totalVotes = statistics.values.sum()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Распределение оценок",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$totalVotes голосов MAL",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "10-бальная шкала",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            statistics.entries.sortedByDescending { it.key }.forEach { (score, votes) ->
                RatingDistributionRow(
                    score = score,
                    votes = votes,
                    fraction = votes.toFloat() / maxVotes.toFloat()
                )
            }
        }
    }
}

@Composable
private fun RatingDistributionRow(
    score: Int,
    votes: Int,
    fraction: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(20.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (votes > 0) fraction.coerceIn(0.02f, 1f) else 0f)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Text(
            text = votes.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp)
        )
    }
}

@Composable
private fun DetailInfoChip(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RatingSlider(
    rating: Float,
    onRatingChange: (Float) -> Unit
) {
    val haptics = rememberHaptics()
    // remember С КЛЮЧОМ: без ключа локальное значение расходилось с моделью,
    // когда ViewModel отдавала уже сохранённую оценку после загрузки.
    var localRating by remember(rating) { mutableFloatStateOf(rating) }
    var rowWidth by remember { mutableIntStateOf(0) }

    fun commit(value: Float) {
        val clamped = value.coerceIn(0f, 10f)
        if (clamped != localRating) localRating = clamped
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .onSizeChanged { rowWidth = it.width }
                // Перетаскивание по ряду заменяет отдельный Slider, который
                // дублировал то же самое действие второй раз.
                .pointerInput(rowWidth) {
                    if (rowWidth == 0) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { haptics.threshold() },
                        onDragEnd = {
                            onRatingChange(localRating)
                            haptics.confirm()
                        },
                    ) { change, _ ->
                        val v = ((change.position.x / rowWidth) * 10f)
                            .roundToInt().coerceIn(0, 10).toFloat()
                        if (v != localRating) {
                            localRating = v
                            haptics.frequentTick()
                        }
                        change.consume()
                    }
                }
        ) {
            repeat(5) { index ->
                val starValue = (index + 1).toFloat() * 2f
                val filled = localRating >= starValue - 1f

                val tint by animateColorAsState(
                    targetValue = if (filled) {
                        MaterialTheme.extended.rating
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
                    },
                    animationSpec = MotionTokens.effectsFast(),
                    label = "starTint",
                )

                // Пружинный «поп» при заполнении, со ступенчатой задержкой —
                // раньше звёзды заливались мгновенно и без отклика.
                val pop = remember { Animatable(1f) }
                LaunchedEffect(filled) {
                    if (filled) {
                        delay(index * 40L)
                        pop.animateTo(1.28f, MotionTokens.spatialFast())
                        pop.animateTo(1f, MotionTokens.spatialDefault())
                    }
                }

                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Оценка ${starValue.toInt()}",
                    tint = tint,
                    modifier = Modifier
                        .size(Sizing.touchTarget)
                        .graphicsLayer {
                            scaleX = pop.value
                            scaleY = pop.value
                        }
                        // clip до нажатия: раньше на звезде вспыхивал
                        // КВАДРАТНЫЙ ripple, потому что форма не задавалась.
                        .clip(CircleShape)
                        .clickable {
                            commit(starValue)
                            onRatingChange(starValue)
                            haptics.confirm()
                        }
                        .padding(Spacing.sm)
                )
            }
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = String.format("%.0f/10", localRating),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Карточка серии с отметкой о просмотре и полосой прогресса.
 *
 * Прогресс по каждой серии всё это время писался в базу, но не читался нигде:
 * `HistoryDao.getByTitleId()` не вызывался ни разу. Вкладка «Серии» выглядела
 * одинаково и до просмотра, и после, а чтобы понять, где остановился, надо
 * было открывать серии по очереди.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeCard(
    episode: Episode,
    progress: EpisodeProgress?,
    download: DownloadItem?,
    onClick: () -> Unit,
    onToggleWatched: () -> Unit,
    onMarkUpTo: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()
    val isWatched = progress?.isWatched == true
    val fraction = progress?.fraction ?: 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptics.longPress()
                        menuOpen = true
                    },
                )
        ) {
            Row(
                modifier = Modifier.padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(Sizing.touchTarget)
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (isWatched) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Просмотренная серия показывает галку вместо номера:
                    // взгляд сразу цепляется за границу «досмотрено / нет».
                    AnimatedContent(
                        targetState = isWatched,
                        transitionSpec = {
                            (fadeIn(MotionTokens.effectsDefault()) +
                                scaleIn(MotionTokens.spatialDefault(), initialScale = 0.6f)) togetherWith
                                fadeOut(MotionTokens.effectsFast())
                        },
                        label = "episodeWatched",
                    ) { watched ->
                        if (watched) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Просмотрено",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(Sizing.iconMd),
                            )
                        } else {
                            Text(
                                text = episode.ordinal.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Серия ${episode.ordinal}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (episode.name.isNotBlank()) {
                        Text(
                            text = episode.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = episode.metaLabel(progress),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                EpisodeDownloadButton(
                    download = download,
                    onDownload = onDownload,
                    onCancel = onCancelDownload,
                )

                Box {
                    FilledTonalIconButton(
                        onClick = onClick,
                        modifier = Modifier.size(Sizing.touchTarget),
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Смотреть",
                            modifier = Modifier.size(Sizing.iconSm)
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(if (isWatched) "Снять отметку" else "Отметить просмотренной")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isWatched) {
                                        Icons.Rounded.RemoveDone
                                    } else {
                                        Icons.Rounded.DoneAll
                                    },
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onToggleWatched()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Отметить все до этой") },
                            leadingIcon = {
                                Icon(Icons.Rounded.PlaylistAddCheck, contentDescription = null)
                            },
                            onClick = {
                                menuOpen = false
                                onMarkUpTo()
                            },
                        )
                    }
                }
            }

            // Полоса рисуется только у начатых серий: сплошной пустой трек
            // под каждой строкой был бы визуальным шумом.
            if (fraction > 0f && !isWatched) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Spacing.xs),
                    drawStopIndicator = {},
                )
            }
        }
    }
}

/**
 * Кнопка загрузки серии: скачать → прогресс → галка.
 *
 * Прогресс рисуется кольцом вокруг иконки, а не отдельной полосой: строка
 * серии и так плотная, а место под кнопку уже занято.
 */
@Composable
private fun EpisodeDownloadButton(
    download: DownloadItem?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
) {
    val haptics = rememberHaptics()

    when (download?.state) {
        null, DownloadState.FAILED -> {
            IconButton(
                onClick = {
                    haptics.confirm()
                    onDownload()
                },
                modifier = Modifier.size(Sizing.touchTarget),
            ) {
                Icon(
                    imageVector = if (download?.state == DownloadState.FAILED) {
                        Icons.Rounded.ErrorOutline
                    } else {
                        Icons.Rounded.Download
                    },
                    contentDescription = if (download?.state == DownloadState.FAILED) {
                        "Загрузка не удалась, повторить"
                    } else {
                        "Скачать серию"
                    },
                    tint = if (download?.state == DownloadState.FAILED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(Sizing.iconMd),
                )
            }
        }

        DownloadState.COMPLETED -> {
            IconButton(
                onClick = {
                    haptics.toggleOff()
                    onCancel()
                },
                modifier = Modifier.size(Sizing.touchTarget),
            ) {
                Icon(
                    imageVector = Icons.Rounded.DownloadDone,
                    contentDescription = "Скачано — нажмите, чтобы удалить",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Sizing.iconMd),
                )
            }
        }

        else -> {
            val animatedProgress by animateFloatAsState(
                targetValue = download.progress,
                animationSpec = MotionTokens.effectsDefault(),
                label = "downloadProgress",
            )
            Box(
                modifier = Modifier.size(Sizing.touchTarget),
                contentAlignment = Alignment.Center,
            ) {
                if (download.progress > 0f) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(Sizing.iconMd),
                        strokeWidth = 2.dp,
                    )
                } else {
                    // Пока размер неизвестен, доля прогресса всегда ноль —
                    // честнее показать неопределённое ожидание, чем пустое кольцо.
                    CircularProgressIndicator(
                        modifier = Modifier.size(Sizing.iconMd),
                        strokeWidth = 2.dp,
                    )
                }
                IconButton(
                    onClick = {
                        haptics.toggleOff()
                        onCancel()
                    },
                    modifier = Modifier.size(Sizing.touchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Отменить загрузку",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Sizing.iconSm),
                    )
                }
            }
        }
    }
}

/** «24 мин · осталось 12 мин» — одна строка вместо двух состояний. */
private fun Episode.metaLabel(progress: EpisodeProgress?): String {
    val durationLabel = if (duration > 0) "${duration / 60} мин" else null
    val progressLabel = when {
        progress == null -> null
        progress.isWatched -> "просмотрено"
        progress.isStarted -> {
            val minutes = progress.remainingMs / 60_000L
            if (minutes < 1L) "почти досмотрено" else "осталось $minutes мин"
        }
        else -> null
    }
    return listOfNotNull(durationLabel, progressLabel).joinToString(" · ")
}

@Composable
private fun FranchiseCard(
    item: FranchiseItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.width(140.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            GlideImage(
                imageModel = { item.poster?.cardUrl },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    contentDescription = item.name
                ),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    )
                }
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.relation != null) {
                    Text(
                        text = item.relation,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun RelatedTitleCard(
    item: RelatedTitleItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.width(140.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        enabled = item.anilibriaId != null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            GlideImage(
                imageModel = { item.posterUrl },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    contentDescription = item.title
                ),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    )
                }
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.relation != null) {
                    Text(
                        text = item.relation,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun TorrentCard(
    torrent: Torrent,
    onMagnetClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = { torrent.magnet?.let(onMagnetClick) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (torrent.quality != null) {
                        Text(
                            text = torrent.quality,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (torrent.series != null) {
                        Text(
                            text = torrent.series,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                if (torrent.size != null && torrent.size > 0) {
                    val sizeGb = torrent.size / (1024.0 * 1024.0 * 1024.0)
                    Text(
                        text = String.format("%.1f GB", sizeGb),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (torrent.seeders != null) {
                        Text(
                            text = "↑ ${torrent.seeders}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.extended.seeders
                        )
                    }
                    if (torrent.leechers != null) {
                        Text(
                            text = "↓ ${torrent.leechers}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.extended.leechers
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = "Скачать торрент",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Высота топбара MD3 без учёта системного инсета. */
private val TOOLBAR_HEIGHT = 64.dp

/**
 * Сворачивающийся хедер экрана тайтла.
 *
 * Едет как единое целое вместе с рядом вкладок; бэкдроп внутри движется
 * медленнее (0.4 от смещения) — это и даёт параллакс. Смещение и производные
 * величины читаются лямбдами ВНУТРИ `graphicsLayer`, то есть на этапе
 * отрисовки: за кадр не происходит ни одной рекомпозиции.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DetailHeader(
    title: Title,
    headerHeight: Dp,
    tabRowHeight: Dp,
    headerOffset: () -> Float,
    collapseFraction: () -> Float,
    selectedTab: DetailTab,
    onTabSelected: (DetailTab) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = headerOffset() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
        ) {
            AnilibrixImage(
                model = title.poster?.fullUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val fraction = collapseFraction()
                        translationY = -headerOffset() * 0.4f
                        // Гаснуть нужно ДО НУЛЯ: при остаточной непрозрачности
                        // свёрнутый бэкдроп просвечивал сквозь текст списка,
                        // потому что хедер рисуется слоем поверх него.
                        alpha = (1f - fraction * 1.4f).coerceIn(0f, 1f)
                        val scale = 1f + fraction * 0.06f
                        scaleX = scale
                        scaleY = scale
                    },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = (1f - collapseFraction() * 1.4f).coerceIn(0f, 1f)
                    }
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = (1f - collapseFraction() * 1.4f).coerceIn(0f, 1f)
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface,
                            ),
                            // Раньше здесь стояло сырое `80f` — это ПИКСЕЛИ,
                            // то есть градиент начинался в разном месте
                            // на разных плотностях экрана.
                            startY = with(LocalDensity.current) { 96.dp.toPx() },
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Spacing.md)
                    .graphicsLayer {
                        val fraction = collapseFraction()
                        alpha = (1f - fraction * 1.6f).coerceIn(0f, 1f)
                        translationY = fraction * 24.dp.toPx()
                    },
                verticalAlignment = Alignment.Bottom
            ) {
                AnilibrixImage(
                    model = title.poster?.cardUrl,
                    contentDescription = title.name.main,
                    modifier = Modifier
                        .width(Sizing.listThumbWidth)
                        .aspectRatio(3f / 4f),
                    shape = AnilibrixShapeExtras.poster,
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.name.main,
                        style = AnilibrixTypeExtras.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (title.name.english != null) {
                        Text(
                            text = title.name.english,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        if (title.type != null) {
                            Text(
                                text = title.type.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (title.year > 0) {
                            Text(
                                text = title.year.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (title.score != null) {
                            Text(
                                text = String.format("%.1f", title.score),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.extended.rating
                            )
                        }
                    }
                }
            }
        }

        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            modifier = Modifier.height(tabRowHeight),
            edgePadding = Spacing.sm,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            DetailTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = { Text(tab.displayName, maxLines = 1) }
                )
            }
        }
    }
}
