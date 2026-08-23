package com.anilibrix.plus.ui.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.ui.components.AnilibrixImage
import com.anilibrix.plus.ui.components.SectionHeader
import com.anilibrix.plus.ui.components.pressScale
import com.anilibrix.plus.ui.detail.DetailIntent
import com.anilibrix.plus.ui.detail.DetailUiState
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing

@OptIn(ExperimentalLayoutApi::class)
fun LazyListScope.infoSection(
    title: Title,
    state: DetailUiState,
    onIntent: (DetailIntent) -> Unit,
    onGenreClick: (String) -> Unit = {},
) {
    // Описание тайтла
    item {
        if (!title.description.isNullOrBlank()) {
            ExpandableDescription(text = title.description)
        }
    }

    // Кадры из аниме (Shikimori screenshots)
    if (state.screenshots.isNotEmpty()) {
        item {
            Spacer(modifier = Modifier.height(Spacing.md))
            SectionHeader(title = "Кадры из аниме", horizontalPadding = Spacing.none)
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
                                onIntent(DetailIntent.OpenScreenshot(index))
                            },
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            }
        }
    }

    // Жанры
    if (title.genres.isNotEmpty()) {
        item {
            Spacer(modifier = Modifier.height(Spacing.lg))
            Text(
                text = "Жанры",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
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
                        ),
                        modifier = Modifier
                            .pressScale()
                            .clickable { onGenreClick(genre.name) }
                    ) {
                        Text(
                            text = genre.name,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    // Блок оценки пользователя
    item {
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = "Моя оценка",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        RatingSlider(
            rating = state.userRating,
            onRatingChange = { onIntent(DetailIntent.SetRating(it)) }
        )
    }

    // Детальная информация и съемочная группа
    item {
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = "Информация",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                title.type?.let { MetadataRow("Тип", it.displayName) }
                title.typeDescription?.let { if (it.isNotBlank()) MetadataRow("Формат", it) }
                title.seasonDescription?.let { if (it.isNotBlank()) MetadataRow("Сезон", it) }
                    ?: title.season?.let {
                        val yearStr = if (title.year > 0) " ${title.year}" else ""
                        MetadataRow("Сезон", "${it.name.name.lowercase().replaceFirstChar { c -> c.uppercase() }}$yearStr")
                    }
                if (title.year > 0 && title.seasonDescription == null && title.season == null) {
                    MetadataRow("Год", title.year.toString())
                }
                title.episodes?.let { MetadataRow("Всего серий", it.size.toString()) }
                if (title.episodesTotal > 0 && title.episodes == null) {
                    MetadataRow("Всего серий", title.episodesTotal.toString())
                }
                MetadataRow("Статус", if (title.isOngoing) "Онгоинг" else "Завершён")
            }
        }
    }
}

@Composable
private fun ExpandableDescription(
    text: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.animateContentSize(MotionTokens.spatialDefault())) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis
        )
        if (text.length > 200) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = if (expanded) "Свернуть" else "Читать далее",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
