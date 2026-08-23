package com.anilibrix.plus.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anilibrix.plus.ui.components.ErrorView
import com.anilibrix.plus.ui.components.LoadingIndicator
import com.anilibrix.plus.ui.components.LocalToastHostState
import com.anilibrix.plus.ui.components.WatchFab
import com.anilibrix.plus.ui.components.rememberHaptics
import com.anilibrix.plus.ui.components.rememberIsScrollingUp
import com.anilibrix.plus.ui.detail.components.CollectionStatusSheet
import com.anilibrix.plus.ui.detail.components.DetailHeader
import com.anilibrix.plus.ui.detail.components.FullscreenScreenshotDialog
import com.anilibrix.plus.ui.detail.components.PlaylistSelectionDialog
import com.anilibrix.plus.ui.detail.components.charactersSection
import com.anilibrix.plus.ui.detail.components.episodesSection
import com.anilibrix.plus.ui.detail.components.franchiseSection
import com.anilibrix.plus.ui.detail.components.icon
import com.anilibrix.plus.ui.detail.components.infoSection
import com.anilibrix.plus.ui.detail.components.recommendationsSection
import com.anilibrix.plus.ui.detail.components.statisticsSection
import com.anilibrix.plus.ui.detail.components.torrentsSection
import com.anilibrix.plus.ui.navigation.LocalBottomBarHeight
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing

private val TOOLBAR_HEIGHT = 64.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleDetailScreen(
    id: String,
    viewModel: TitleDetailViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onPlayEpisode: (Long, Long) -> Unit = { _, _ -> },
    onCharacterClick: (Long) -> Unit = {},
    onTitleClick: (Long) -> Unit = {},
    onGenreClick: (String) -> Unit = {}
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
    val collapseFraction by remember(maxCollapsePx) {
        derivedStateOf { (-headerOffset / maxCollapsePx).coerceIn(0f, 1f) }
    }

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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.title?.name?.main.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = collapseFraction)
                )
            )
        },
        floatingActionButton = {
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
                            contentPadding = PaddingValues(
                                start = Spacing.screenHorizontal,
                                end = Spacing.screenHorizontal,
                                top = headerMax + tabRowHeight,
                                bottom = LocalBottomBarHeight.current + Spacing.xxxl * 2,
                            )
                        ) {
                            when (state.selectedTab) {
                                DetailTab.DESCRIPTION -> infoSection(
                                    title = title,
                                    state = state,
                                    onIntent = viewModel::onIntent,
                                    onGenreClick = onGenreClick
                                )
                                DetailTab.EPISODES -> episodesSection(
                                    state = state,
                                    onIntent = viewModel::onIntent,
                                    onPlayEpisode = onPlayEpisode
                                )
                                DetailTab.CHARACTERS -> charactersSection(
                                    state = state,
                                    onCharacterClick = onCharacterClick
                                )
                                DetailTab.RELATED -> franchiseSection(
                                    state = state,
                                    onTitleClick = onTitleClick
                                )
                                DetailTab.STATISTICS -> statisticsSection(
                                    state = state
                                )
                                DetailTab.RECOMMENDATIONS -> recommendationsSection(
                                    state = state,
                                    onTitleClick = onTitleClick
                                )
                                DetailTab.TORRENTS -> torrentsSection(
                                    state = state,
                                    onIntent = { intent ->
                                        if (intent is DetailIntent.OpenMagnet) {
                                            runCatching {
                                                val mIntent = Intent(Intent.ACTION_VIEW, Uri.parse(intent.magnet))
                                                context.startActivity(mIntent)
                                            }.onFailure {
                                                toastHostState?.showError("Не найдено приложение для торрентов")
                                            }
                                        } else {
                                            viewModel.onIntent(intent)
                                        }
                                    }
                                )
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

    // Полноэкранный просмотр скриншота
    state.fullscreenScreenshot?.let { index ->
        val shot = state.screenshots.getOrNull(index)
        FullscreenScreenshotDialog(
            screenshot = shot,
            onDismiss = { viewModel.onIntent(DetailIntent.CloseScreenshot) }
        )
    }

    // Шторка статуса в списках (Смотрю, В планах...)
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

    // Диалог выбора плейлистов
    if (state.showPlaylistDialog) {
        PlaylistSelectionDialog(
            playlists = state.playlists,
            playlistIdsForTitle = state.playlistIdsForTitle,
            onTogglePlaylist = { viewModel.onIntent(DetailIntent.TogglePlaylistMembership(it)) },
            onDismiss = { viewModel.onIntent(DetailIntent.DismissPlaylistDialog) }
        )
    }
}
