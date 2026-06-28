package com.anilibrix.plus.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
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
import com.anilibrix.plus.domain.model.MalCharacter
import com.anilibrix.plus.domain.model.Torrent
import com.anilibrix.plus.ui.components.CharacterCard
import com.anilibrix.plus.ui.components.CharacterCardData
import com.anilibrix.plus.ui.components.ErrorView
import com.anilibrix.plus.ui.components.LoadingIndicator
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleDetailScreen(
    id: String,
    viewModel: TitleDetailViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onPlayEpisode: (Long, Long) -> Unit = { _, _ -> }
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(id) {
        viewModel.onIntent(DetailIntent.Load(id))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title?.name?.main ?: "", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            if (state.title != null && state.title!!.episodes?.isNotEmpty() == true) {
                FloatingActionButton(
                    onClick = {
                        val firstEpisode = state.title!!.episodes!!.first()
                        onPlayEpisode(state.title!!.id, firstEpisode.id)
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Смотреть",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
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
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        ) {
                            GlideImage(
                                imageModel = { title.poster?.original ?: title.poster?.medium },
                                modifier = Modifier
                                    .fillMaxSize(),
                                imageOptions = ImageOptions(
                                    contentScale = ContentScale.Crop,
                                    contentDescription = title.name.main
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.surface
                                            ),
                                            startY = 80f
                                        )
                                    )
                            )

                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                GlideImage(
                                    imageModel = { title.poster?.medium ?: title.poster?.small },
                                    modifier = Modifier
                                        .width(80.dp)
                                        .aspectRatio(3f / 4f)
                                        .clip(RoundedCornerShape(8.dp)),
                                    imageOptions = ImageOptions(
                                        contentScale = ContentScale.Crop,
                                        contentDescription = title.name.main
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title.name.main,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (title.name.english != null) {
                                        Text(
                                            text = title.name.english,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                        if (title.score != null) {
                                            Text(
                                                text = String.format("%.1f", title.score),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFFFFC107)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.onIntent(DetailIntent.ToggleFavorite) },
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (state.isFavorite) Icons.Default.Favorite
                                        else Icons.Default.FavoriteBorder,
                                        contentDescription = if (state.isFavorite) "Удалить из избранного"
                                        else "Добавить в избранное",
                                        tint = if (state.isFavorite) MaterialTheme.colorScheme.error
                                        else Color.White
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.onIntent(DetailIntent.ToggleWatchLater) },
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (state.isInWatchLater) Icons.Default.Bookmark
                                        else Icons.Default.BookmarkBorder,
                                        contentDescription = if (state.isInWatchLater) "Удалить из списка"
                                        else "Добавить в список",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        ScrollableTabRow(
                            selectedTabIndex = state.selectedTab.ordinal,
                            edgePadding = 8.dp
                        ) {
                            DetailTab.entries.forEach { tab ->
                                Tab(
                                    selected = state.selectedTab == tab,
                                    onClick = { viewModel.onIntent(DetailIntent.SelectTab(tab)) },
                                    text = { Text(tab.displayName) }
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp)
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

                                    item {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Жанры",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                                onClick = { onPlayEpisode(title.id, episode.id) }
                                            )
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        }
                                    }
                                }

                                DetailTab.CHARACTERS -> {
                                    if (state.characters.isEmpty()) {
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
                                    } else {
                                        item {
                                            LazyVerticalGrid(
                                                columns = GridCells.Fixed(2),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(((state.characters.size / 2 + 1) * 220).dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(
                                                    items = state.characters,
                                                    key = { it.malId }
                                                ) { character ->
                                                    CharacterCard(
                                                        data = CharacterCardData(
                                                            id = character.malId.toString(),
                                                            imageUrl = character.imageUrl ?: "",
                                                            name = character.name,
                                                            role = character.role ?: "",
                                                            seiyuuName = character.seiyuu?.name,
                                                            seiyuuImageUrl = character.seiyuu?.imageUrl
                                                        ),
                                                        onClick = {}
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                DetailTab.RELATED -> {
                                    if (state.franchise.isEmpty()) {
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
                                        item {
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                items(
                                                    items = state.franchise,
                                                    key = { it.id }
                                                ) { item ->
                                                    FranchiseCard(
                                                        item = item,
                                                        onClick = { onBack() }
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
                    }
                }
            }
        }
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
    var localRating by remember { mutableFloatStateOf(rating) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { index ->
                val starValue = (index + 1).toFloat() * 2f
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (localRating >= starValue - 1f)
                        Color(0xFFFFC107)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable {
                            localRating = starValue
                            onRatingChange(starValue)
                        }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = String.format("%.0f/10", localRating),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Slider(
            value = localRating,
            onValueChange = { newValue ->
                localRating = newValue
                onRatingChange(newValue)
            },
            valueRange = 0f..10f,
            steps = 9,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun EpisodeCard(
    episode: Episode,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = episode.ordinal.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (episode.duration > 0) {
                    Text(
                        text = "${episode.duration / 60} мин.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Button(
                onClick = onClick,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Смотреть",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
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
                imageModel = { item.poster?.medium ?: item.poster?.small },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    contentDescription = item.name
                )
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
                            color = Color(0xFF4CAF50)
                        )
                    }
                    if (torrent.leechers != null) {
                        Text(
                            text = "↓ ${torrent.leechers}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = "Magnet",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
