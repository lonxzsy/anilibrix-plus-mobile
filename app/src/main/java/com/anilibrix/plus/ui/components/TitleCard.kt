package com.anilibrix.plus.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.ui.theme.cardOverlay
import com.anilibrix.plus.ui.theme.shimmerBase
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

@Composable
fun TitleCardGrid(
    title: Title,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            Box {
                GlideImage(
                    imageModel = { title.poster?.medium ?: title.poster?.small },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    imageOptions = ImageOptions(
                        contentScale = ContentScale.Crop,
                        contentDescription = title.name.main
                    )
                )
                if (title.score != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", title.score),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
                if (title.type != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(cardOverlay, RoundedCornerShape(bottomStart = 8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = title.type.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
                if (progress != null && progress > 0f) {
                    val animatedProgress by animateFloatAsState(targetValue = progress)
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = title.name.main,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (title.genres.isNotEmpty()) {
                    Text(
                        text = title.genres.take(3).joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
    progress: Float? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            GlideImage(
                imageModel = { title.poster?.medium ?: title.poster?.original },
                modifier = Modifier
                    .size(width = 80.dp, height = 112.dp)
                    .clip(RoundedCornerShape(8.dp)),
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    contentDescription = title.name.main
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title.name.main,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (title.name.english != null) {
                        Text(
                            text = title.name.english,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (title.genres.isNotEmpty()) {
                        Text(
                            text = title.genres.take(3).joinToString(", ") { it.name },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (title.score != null) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", title.score),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    if (title.type != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title.type.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (title.year > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title.year.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                if (progress != null && progress > 0f) {
                    val animatedProgress by animateFloatAsState(targetValue = progress)
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else if (title.episodesTotal > 0) {
                    Text(
                        text = "${title.episodes?.size ?: 0} / ${title.episodesTotal} эп.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun TitleCardShimmer(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(shimmerBase)
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                        .background(shimmerBase, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(10.dp)
                        .background(shimmerBase, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}
