package com.anilibrix.plus.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anilibrix.plus.domain.model.MalAnime
import com.anilibrix.plus.domain.model.ShikimoriAnime
import com.anilibrix.plus.ui.components.AnilibrixImage
import com.anilibrix.plus.ui.components.EmptyKind
import com.anilibrix.plus.ui.components.EmptyState
import com.anilibrix.plus.ui.components.TitleCardList
import com.anilibrix.plus.ui.components.pressScale
import com.anilibrix.plus.ui.detail.DetailUiState
import com.anilibrix.plus.ui.theme.AnilibrixShapeExtras
import com.anilibrix.plus.ui.theme.Elevation
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing
import com.anilibrix.plus.ui.theme.extended

fun LazyListScope.recommendationsSection(
    state: DetailUiState,
    onTitleClick: (Long) -> Unit
) {
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
            ) { malAnime ->
                MalAnimeListItem(
                    anime = malAnime,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
        else -> {
            item {
                EmptyState(
                    kind = EmptyKind.SearchResult,
                    title = "Рекомендации не найдены",
                    subtitle = "Похожие тайтлы появятся при наличии данных в каталоге",
                    modifier = Modifier.padding(vertical = Spacing.xl)
                )
            }
        }
    }
}

fun LazyListScope.statisticsSection(
    state: DetailUiState
) {
    val hasStatistics = state.malDetails != null ||
        state.statistics.isNotEmpty() ||
        state.shikimoriDetails != null

    if (!hasStatistics) {
        item {
            EmptyState(
                kind = EmptyKind.History,
                title = "Нет данных статистики",
                subtitle = "Рейтинги и распределение оценок сообщества будут отображены здесь",
                modifier = Modifier.padding(vertical = Spacing.xl)
            )
        }
        return
    }

    item {
        StatisticsOverviewCard(
            malDetails = state.malDetails,
            shikimoriDetails = state.shikimoriDetails
        )
    }

    if (state.statistics.isNotEmpty()) {
        item {
            Spacer(modifier = Modifier.height(Spacing.md))
            RatingDistributionCard(statistics = state.statistics)
        }
    }
}

@Composable
fun MalAnimeListItem(
    anime: MalAnime,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnilibrixImage(
                model = anime.imageUrl,
                contentDescription = anime.title,
                modifier = Modifier
                    .width(Sizing.listThumbWidth)
                    .aspectRatio(Sizing.POSTER_ASPECT),
                shape = AnilibrixShapeExtras.poster
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = anime.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                anime.score?.let { score ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "★ ${String.format("%.1f", score)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.extended.rating
                    )
                }
            }
        }
    }
}

@Composable
fun StatisticsOverviewCard(
    malDetails: MalAnime?,
    shikimoriDetails: ShikimoriAnime?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "Рейтинг и популярность",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                malDetails?.score?.let { score ->
                    StatItem("MyAnimeList", String.format("%.2f", score))
                }
                shikimoriDetails?.score?.let { score ->
                    StatItem("Shikimori", String.format("%.2f", score))
                }
                malDetails?.rank?.let { rank ->
                    StatItem("Ранг MAL", "#$rank")
                }
            }
        }
    }
}

@Composable
fun RatingDistributionCard(
    statistics: Map<Int, Int>,
    modifier: Modifier = Modifier
) {
    val maxVotes = statistics.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "Распределение оценок",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            (10 downTo 1).forEach { star ->
                val votes = statistics[star] ?: 0
                val fraction = votes.toFloat() / maxVotes.toFloat()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$star ★",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(36.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "$votes",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(44.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
