package com.anilibrix.plus.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Star
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

private val TITLE_BLOCK_MIN_HEIGHT = 56.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TitleCardGrid(
    title: Title,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    progress: Float? = null,
    isFavorite: Boolean = false,
) {
    val haptics = rememberHaptics()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale()
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = {
                            haptics.longPress()
                            onLongClick()
                        }
                    )
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            ),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TitleCardList(
    title: Title,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    progress: Float? = null,
) {
    val haptics = rememberHaptics()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale()
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = {
                            haptics.longPress()
                            onLongClick()
                        }
                    )
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            ),
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
                }
            }
        }
    }
}

@Composable
fun ScorePill(score: Double, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MaterialTheme.extended.mediaScrim, CircleShape)
            .padding(horizontal = Spacing.sm, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = MaterialTheme.extended.rating,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = String.format("%.1f", score),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.extended.onMediaScrim,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

@Composable
fun TypePill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.extended.mediaScrim, CircleShape)
            .padding(horizontal = Spacing.sm, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.extended.onMediaScrim
        )
    }
}
