package com.anilibrix.plus.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.model.Playlist
import com.anilibrix.plus.ui.components.AnilibrixImage
import com.anilibrix.plus.ui.components.EmptyKind
import com.anilibrix.plus.ui.components.EmptyState
import com.anilibrix.plus.ui.components.LocalToastHostState
import com.anilibrix.plus.ui.components.pressScale
import com.anilibrix.plus.ui.components.rememberHaptics
import com.anilibrix.plus.ui.navigation.screenContentPadding
import com.anilibrix.plus.ui.theme.AnilibrixShapeExtras
import com.anilibrix.plus.ui.theme.Elevation
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing
import kotlinx.coroutines.launch

private val LIBRARY_TABS = listOf("Мои списки", "Избранное", "История", "Плейлисты")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onPlayEpisode: (Long, Long) -> Unit = { _, _ -> },
    onTitleClick: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toastHost = LocalToastHostState.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = state.selectedTab,
        pageCount = { LIBRARY_TABS.size },
    )
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page != state.selectedTab) {
                viewModel.handleIntent(LibraryIntent.SelectTab(page))
                haptics.tick()
            }
        }
    }

    // Отмена удаления идёт через ОБЩИЙ тост-хост приложения. Раньше ради этого
    // библиотека держала собственный вложенный Scaffold со своим SnackbarHost,
    // который дублировал общий и ломал инсеты.
    LaunchedEffect(state.pendingUndo?.token) {
        val undo = state.pendingUndo ?: return@LaunchedEffect
        val host = toastHost ?: return@LaunchedEffect
        val result = host.showAction(message = undo.message, actionLabel = "Отменить")
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.handleIntent(LibraryIntent.UndoLastRemoval)
        } else {
            viewModel.handleIntent(LibraryIntent.DismissUndo)
        }
    }

    state.renamingPlaylist?.let { playlist ->
        PlaylistNameDialog(
            title = "Переименовать плейлист",
            initialName = playlist.name,
            confirmLabel = "Сохранить",
            onDismiss = { viewModel.handleIntent(LibraryIntent.DismissRenamePlaylist) },
            onConfirm = { viewModel.handleIntent(LibraryIntent.ConfirmRenamePlaylist(it)) },
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Библиотека") },
                    actions = {
                        IconButton(onClick = { searchOpen = !searchOpen }) {
                            Icon(Icons.Rounded.Search, contentDescription = "Поиск")
                        }
                        Box {
                            IconButton(onClick = { sortMenuOpen = true }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Sort,
                                    contentDescription = "Сортировка",
                                )
                            }
                            DropdownMenu(
                                expanded = sortMenuOpen,
                                onDismissRequest = { sortMenuOpen = false },
                            ) {
                                LibrarySort.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.displayName) },
                                        onClick = {
                                            viewModel.handleIntent(LibraryIntent.SetSort(option))
                                            sortMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )

                AnimatedVisibility(
                    visible = searchOpen,
                    enter = expandVertically(MotionTokens.spatialDefault()) +
                        fadeIn(MotionTokens.effectsDefault()),
                    exit = shrinkVertically(MotionTokens.spatialFast()) +
                        fadeOut(MotionTokens.effectsFast()),
                ) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { viewModel.handleIntent(LibraryIntent.UpdateQuery(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = Spacing.screenHorizontal,
                                vertical = Spacing.sm,
                            ),
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        placeholder = { Text("Фильтр по названию") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    )
                }

                // Текстовые табы: связка «иконка + отступ + текст» и делала их
                // такими широкими, что четыре русские подписи не помещались.
                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = Spacing.sm,
                ) {
                    LIBRARY_TABS.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        page = index,
                                        animationSpec = MotionTokens.spatialDefault(),
                                    )
                                }
                            },
                            text = { Text(title, maxLines = 1) },
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) { page ->
            when (page) {
                0 -> CollectionsTab(
                    state = state,
                    onSelectStatus = { viewModel.handleIntent(LibraryIntent.SelectStatus(it)) },
                    onRemove = { viewModel.handleIntent(LibraryIntent.RemoveFromCollection(it)) },
                    onTitleClick = onTitleClick,
                )
                1 -> TitleItemList(
                    items = state.filteredFavorites,
                    emptyKind = EmptyKind.Favorites,
                    onRemove = { viewModel.handleIntent(LibraryIntent.RemoveFavorite(it)) },
                    onTitleClick = onTitleClick,
                )
                2 -> HistoryTab(
                    items = state.filteredHistory,
                    onRemove = { entry -> viewModel.handleIntent(LibraryIntent.RemoveHistory(entry)) },
                    onPlayEpisode = onPlayEpisode
                )
                3 -> PlaylistsTab(
                    playlists = state.filteredPlaylists,
                    expandedId = state.expandedPlaylistId,
                    onToggle = { viewModel.handleIntent(LibraryIntent.TogglePlaylist(it)) },
                    onCreate = { viewModel.handleIntent(LibraryIntent.CreatePlaylist(it)) },
                    onRename = { viewModel.handleIntent(LibraryIntent.StartRenamePlaylist(it)) },
                    onDelete = { viewModel.handleIntent(LibraryIntent.DeletePlaylist(it)) },
                    onTitleClick = onTitleClick
                )
            }
        }
    }
}

/**
 * Вкладка «Мои списки» — пять статусов трекера.
 *
 * Полноценный трекер был готов и в базе, и на сервере (пять значений
 * `CollectionType`, свои таблицы и эндпоинты), но наружу выводился ровно один
 * статус — «Буду смотреть». Остальные четыре не имели ни одной точки входа в
 * интерфейсе.
 */
@Composable
private fun CollectionsTab(
    state: LibraryUiState,
    onSelectStatus: (CollectionType) -> Unit,
    onRemove: (FavoriteItem) -> Unit,
    onTitleClick: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = Spacing.screenHorizontal,
                vertical = Spacing.sm,
            ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(CollectionType.entries, key = { it.name }) { type ->
                val count = state.countOf(type)
                FilterChip(
                    selected = state.selectedStatus == type,
                    onClick = { onSelectStatus(type) },
                    label = { Text(if (count > 0) "${type.displayName} · $count" else type.displayName) },
                    shape = FilterChipDefaults.shape,
                )
            }
        }

        TitleItemList(
            items = state.filteredCollection,
            emptyKind = when (state.selectedStatus) {
                CollectionType.WATCH_LATER -> EmptyKind.WatchLater
                else -> EmptyKind.Favorites
            },
            emptyTitle = "В списке «${state.selectedStatus.displayName}» пусто",
            emptySubtitle = "Отметьте тайтл этим статусом на его странице",
            onRemove = onRemove,
            onTitleClick = onTitleClick,
        )
    }
}

/**
 * Единый список тайтлов для «Моих списков» и «Избранного».
 *
 * Раньше это были две почти дословно совпадающие функции на сто строк каждая,
 * с собственными захардкоженными отступами и прямыми вызовами `GlideImage` в
 * обход общей обёртки.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TitleItemList(
    items: List<FavoriteItem>,
    emptyKind: EmptyKind,
    onRemove: (FavoriteItem) -> Unit,
    onTitleClick: (Long) -> Unit,
    emptyTitle: String? = null,
    emptySubtitle: String? = null,
) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                kind = emptyKind,
                title = emptyTitle ?: emptyKind.defaultTitle,
                subtitle = emptySubtitle ?: emptyKind.defaultSubtitle,
            )
        }
        return
    }

    LazyColumn(
        contentPadding = screenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap),
    ) {
        items(items, key = { it.titleId }) { item ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        onRemove(item)
                        true
                    } else {
                        false
                    }
                }
            )
            SwipeToDismissBox(
                state = dismissState,
                modifier = Modifier.animateItem(
                    fadeInSpec = MotionTokens.effectsDefault(),
                    placementSpec = MotionTokens.spatialDefault(),
                    fadeOutSpec = MotionTokens.effectsFast(),
                ),
                backgroundContent = { DismissBackground(dismissState.progress) },
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale()
                        .clickable { onTitleClick(item.titleId) },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnilibrixImage(
                            model = item.posterUrl,
                            contentDescription = item.titleName,
                            modifier = Modifier.size(
                                width = Sizing.avatarMd,
                                height = Sizing.avatarMd,
                            ),
                            shape = MaterialTheme.shapes.small,
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.titleName.ifBlank { "Тайтл #${item.titleId}" },
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.progressLabel != null) {
                                Spacer(modifier = Modifier.height(Spacing.xxs))
                                Text(
                                    text = item.progressLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (item.progressFraction > 0f) {
                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    LinearProgressIndicator(
                                        progress = { item.progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(Spacing.xs)
                                            .clip(AnilibrixShapeExtras.pill),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Фон под свайпом.
 *
 * Иконка растёт и проявляется по мере протягивания, а не появляется сразу во
 * всю силу: так видно, что жест ещё можно отменить.
 */
@Composable
private fun DismissBackground(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = Spacing.xl),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            Icons.Rounded.Delete,
            contentDescription = "Удалить",
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.graphicsLayer {
                val t = ((progress - 0.1f) / 0.4f).coerceIn(0f, 1f)
                alpha = t
                scaleX = 0.6f + 0.4f * t
                scaleY = scaleX
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTab(
    items: List<HistoryEntry>,
    onRemove: (HistoryEntry) -> Unit,
    onPlayEpisode: (Long, Long) -> Unit
) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(kind = EmptyKind.History)
        }
        return
    }

    LazyColumn(
        contentPadding = screenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap)
    ) {
        items(items, key = { "${it.titleId}_${it.episodeId}" }) { entry ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        onRemove(entry)
                        true
                    } else {
                        false
                    }
                }
            )
            SwipeToDismissBox(
                state = dismissState,
                modifier = Modifier.animateItem(
                    fadeInSpec = MotionTokens.effectsDefault(),
                    placementSpec = MotionTokens.spatialDefault(),
                    fadeOutSpec = MotionTokens.effectsFast(),
                ),
                backgroundContent = { DismissBackground(dismissState.progress) },
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale()
                        .clickable { onPlayEpisode(entry.titleId, entry.episodeId) },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnilibrixImage(
                                model = entry.posterUrl,
                                contentDescription = entry.titleName,
                                modifier = Modifier.size(
                                    width = Sizing.avatarSm,
                                    height = Sizing.avatarSm,
                                ),
                                shape = MaterialTheme.shapes.small,
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.titleName.ifBlank { "Тайтл #${entry.titleId}" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Серия ${entry.episodeNumber} · ${entry.remainingLabel()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        val progress = if (entry.duration > 0) {
                            entry.timestamp.toFloat() / entry.duration.toFloat()
                        } else {
                            0f
                        }
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Spacing.xs)
                                .clip(AnilibrixShapeExtras.pill),
                        )
                    }
                }
            }
        }
    }
}

/** «Осталось 14 мин» информативнее голой позиции в секундах. */
private fun HistoryEntry.remainingLabel(): String {
    if (duration <= 0L) return "не начата"
    val remainingMs = (duration - timestamp).coerceAtLeast(0L)
    val remainingMin = remainingMs / 60_000L
    return when {
        remainingMs <= 0L || timestamp * 100 >= duration * 90 -> "просмотрено"
        remainingMin < 1L -> "осталось меньше минуты"
        else -> "осталось $remainingMin мин"
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<Playlist>,
    expandedId: Long?,
    onToggle: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (Playlist) -> Unit,
    onDelete: (Long) -> Unit,
    onTitleClick: (Long) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        PlaylistNameDialog(
            title = "Новый плейлист",
            initialName = "",
            confirmLabel = "Создать",
            onDismiss = { showCreateDialog = false },
            onConfirm = {
                onCreate(it)
                showCreateDialog = false
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(Sizing.iconSm))
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text("Создать")
            }
        }

        if (playlists.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(kind = EmptyKind.Playlists)
            }
            return@Column
        }

        LazyColumn(
            contentPadding = screenContentPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap)
        ) {
            items(playlists, key = { it.id }) { playlist ->
                val isExpanded = expandedId == playlist.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale()
                        .clickable { onToggle(playlist.id) }
                        .animateItem(
                            fadeInSpec = MotionTokens.effectsDefault(),
                            placementSpec = MotionTokens.spatialDefault(),
                            fadeOutSpec = MotionTokens.effectsFast(),
                        ),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ListAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Sizing.iconMd)
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = playlistCountLabel(playlist.items.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // `PlaylistDao.update` был реализован с самого
                            // начала, но переименовать плейлист было нельзя —
                            // вызывать его было неоткуда.
                            IconButton(onClick = { onRename(playlist) }) {
                                Icon(
                                    Icons.Rounded.DriveFileRenameOutline,
                                    contentDescription = "Переименовать",
                                )
                            }
                            IconButton(onClick = { onDelete(playlist.id) }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Удалить",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (isExpanded && playlist.items.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            playlist.items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onTitleClick(item.titleId) }
                                        .padding(vertical = Spacing.sm, horizontal = Spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.titleName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** «1 тайтл / 3 тайтла / 12 тайтлов» — без этого получалось «1 элементов». */
private fun playlistCountLabel(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    val word = when {
        mod100 in 11..14 -> "тайтлов"
        mod10 == 1 -> "тайтл"
        mod10 in 2..4 -> "тайтла"
        else -> "тайтлов"
    }
    return "$count $word"
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
