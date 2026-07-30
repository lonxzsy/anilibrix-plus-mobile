package com.anilibrix.plus.ui.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.ui.components.AnilibrixImage
import com.anilibrix.plus.ui.components.EmptyKind
import com.anilibrix.plus.ui.components.EmptyState
import com.anilibrix.plus.ui.components.LoadingIndicator
import com.anilibrix.plus.ui.components.SectionHeader
import com.anilibrix.plus.ui.components.pressScale
import com.anilibrix.plus.ui.navigation.screenContentPadding
import com.anilibrix.plus.ui.theme.AnilibrixShapeExtras
import com.anilibrix.plus.ui.theme.AnilibrixTypeExtras
import com.anilibrix.plus.ui.theme.Elevation
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onTitleClick: (Long) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { padding ->
        when {
            state.loading -> LoadingIndicator(modifier = Modifier.padding(padding))

            state.totalMs == 0L && state.recent.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        kind = EmptyKind.History,
                        title = "Пока нечего показать",
                        subtitle = "Посмотрите первую серию — статистика появится здесь",
                    )
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = screenContentPadding(padding),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                item(key = "time") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0),
                    ) {
                        Column(modifier = Modifier.padding(Spacing.lg)) {
                            Text(
                                text = "Всего просмотрено",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                            Text(
                                text = formatDuration(state.totalMs),
                                style = AnilibrixTypeExtras.titleLargeEmphasized,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(Modifier.height(Spacing.md))
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                                MiniStat("За неделю", formatDuration(state.weekMs))
                                MiniStat("За месяц", formatDuration(state.monthMs))
                            }
                        }
                    }
                }

                item(key = "counters") {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        StatCard("Серий", state.episodesWatched.toString(), Modifier.weight(1f))
                        StatCard("Тайтлов", state.titlesStarted.toString(), Modifier.weight(1f))
                        StatCard("Дней подряд", state.streakDays.toString(), Modifier.weight(1f))
                    }
                }

                if (state.byStatus.isNotEmpty()) {
                    item(key = "status-header") {
                        SectionHeader(title = "По спискам", horizontalPadding = Spacing.none)
                    }
                    item(key = "status-body") {
                        val total = state.byStatus.values.sum().coerceAtLeast(1)
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            CollectionType.entries.forEach { type ->
                                val count = state.byStatus[type] ?: 0
                                if (count == 0) return@forEach
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(type.displayName, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            count.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Spacer(Modifier.height(Spacing.xs))
                                    LinearProgressIndicator(
                                        progress = { count.toFloat() / total },
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

                if (state.recent.isNotEmpty()) {
                    item(key = "recent-header") {
                        SectionHeader(title = "Недавнее", horizontalPadding = Spacing.none)
                    }
                    items(state.recent, key = { "${it.titleId}-${it.episodeId}" }) { entry ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressScale()
                                .clickable { onTitleClick(entry.titleId) },
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0),
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AnilibrixImage(
                                    model = entry.posterUrl,
                                    contentDescription = entry.titleName,
                                    modifier = Modifier.size(width = Sizing.avatarSm, height = Sizing.avatarMd),
                                    shape = AnilibrixShapeExtras.poster,
                                )
                                Spacer(Modifier.width(Spacing.md))
                                Column {
                                    Text(
                                        text = entry.titleName.ifBlank { "Тайтл #${entry.titleId}" },
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "Серия ${entry.episodeNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
private fun MiniStat(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = value, style = AnilibrixTypeExtras.titleMediumEmphasized)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** «12 ч 30 мин» — часы без минут выглядят приблизительно, минуты без часов не читаются. */
private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0 мин"
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    return when {
        hours <= 0L -> "$minutes мин"
        minutes == 0L -> "$hours ч"
        else -> "$hours ч $minutes мин"
    }
}
