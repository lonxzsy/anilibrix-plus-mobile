package com.anilibrix.plus.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.ui.theme.AnilibrixBrushes
import com.anilibrix.plus.ui.theme.AnilibrixShapeExtras
import com.anilibrix.plus.ui.theme.Elevation
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing
import com.anilibrix.plus.ui.theme.extended

/**
 * Карточка тайтла в сетке.
 *
 * Ключевые изменения против прежней версии:
 *
 * * **Убрана `cardElevation(6dp/10dp)`.** Тень на четырёх десятках карточек
 *   сетки — основной источник подтормаживания при скролле, и MD3 для карточек
 *   списка предписывает тональную поверхность, а не тень.
 * * **Постер задан пропорцией, а не `height(180.dp)`,** а текстовый блок имеет
 *   минимальную высоту ровно под две строки. Раньше ячейки с длинными
 *   названиями были выше соседних — сетка выглядела рваной.
 * * Все хардкоды (`0xFFFFC107`, `Color.White`, `Color.Black.copy(0.68f)`,
 *   `RoundedCornerShape(999.dp)`, три разных радиуса) заменены токенами.
 * * Картинка грузится через [AnilibrixImage] — с crossfade и скелетоном.
 */
@Composable
fun TitleCardGrid(
    title: Title,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    /**
     * Сердце на карточке.
     *
     * `getFavoriteIds()` существовал в API с самого начала, но не вызывался
     * ниоткуда, а `Title.inFavorites` отбрасывался при маппинге — понять, что
     * тайтл уже в избранном, можно было только открыв его.
     */
    isFavorite: Boolean = false,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(Sizing.POSTER_ASPECT)
            ) {
                AnilibrixImage(
                    model = title.poster?.cardUrl,
                    contentDescription = title.name.main,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AnilibrixBrushes.heroOverlay)
                )

                if (title.score != null) {
                    ScorePill(
                        score = title.score,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(Spacing.sm),
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isFavorite) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "В избранном",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(Sizing.iconSm),
                        )
                    }
                    if (title.type != null) {
                        TypePill(text = title.type.displayName)
                    }
                }

                if (progress != null && progress > 0f) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = MotionTokens.spatialDefault(),
                        label = "watchProgress",
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.extended.onMediaScrim.copy(alpha = 0.3f),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(Spacing.sm)
                    // Ровно две строки заголовка + строка жанров: не даёт
                    // соседним ячейкам сетки разъезжаться по высоте.
                    .heightIn(min = TITLE_BLOCK_MIN_HEIGHT)
            ) {
                Text(
                    text = title.name.main,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (title.genres.isNotEmpty()) {
                    Text(
                        text = title.genres.take(3).joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun TitleCardList(
    title: Title,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0),
    ) {
        Row(modifier = Modifier.padding(Spacing.sm)) {
            AnilibrixImage(
                model = title.poster?.cardUrl,
                contentDescription = title.name.main,
                modifier = Modifier.size(Sizing.listThumbWidth, Sizing.listThumbHeight),
                shape = AnilibrixShapeExtras.poster,
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = title.name.main,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (title.name.english != null) {
                        Text(
                            text = title.name.english,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    if (title.genres.isNotEmpty()) {
                        Text(
                            text = title.genres.take(3).joinToString(", ") { it.name },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (title.score != null) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MaterialTheme.extended.rating,
                            modifier = Modifier.size(Sizing.iconXs),
                        )
                        Spacer(modifier = Modifier.width(Spacing.xxs))
                        Text(
                            text = String.format("%.1f", title.score),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (title.type != null) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = title.type.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (title.year > 0) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = title.year.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (progress != null && progress > 0f) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = MotionTokens.spatialDefault(),
                        label = "watchProgressList",
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                } else if (title.episodesTotal > 0) {
                    Text(
                        text = "${title.episodes?.size ?: 0} / ${title.episodesTotal} эп.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Оценка поверх постера: тёмная пилюля, читаемая на любой обложке. */
@Composable
private fun ScorePill(score: Double, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MaterialTheme.extended.mediaScrim, CircleShape)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = MaterialTheme.extended.rating,
            modifier = Modifier.size(Sizing.iconXs),
        )
        Spacer(modifier = Modifier.width(Spacing.xxs))
        Text(
            text = String.format("%.1f", score),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.extended.onMediaScrim,
        )
    }
}

@Composable
private fun TypePill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(AnilibrixBrushes.primaryGradient, CircleShape)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/**
 * Скелетон карточки. Делегирует в общую систему скелетонов, чтобы форма и
 * радиусы гарантированно совпадали с настоящей карточкой.
 */
@Composable
fun TitleCardShimmer(modifier: Modifier = Modifier) {
    TitleCardGridSkeleton(modifier)
}

/** Две строки labelLarge (20sp leading) + строка bodySmall (16sp) + отступы. */
private val TITLE_BLOCK_MIN_HEIGHT = 58.dp
