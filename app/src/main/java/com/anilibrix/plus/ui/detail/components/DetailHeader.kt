package com.anilibrix.plus.ui.detail.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.ui.components.AnilibrixImage
import com.anilibrix.plus.ui.components.rememberHaptics
import com.anilibrix.plus.ui.detail.DetailTab
import com.anilibrix.plus.ui.theme.AnilibrixShapeExtras
import com.anilibrix.plus.ui.theme.AnilibrixTypeExtras
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing
import com.anilibrix.plus.ui.theme.extended
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailHeader(
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

@Composable
fun RatingSlider(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    var localRating by remember(rating) { mutableFloatStateOf(rating) }
    var rowWidth by remember { mutableIntStateOf(0) }

    fun commit(value: Float) {
        val clamped = value.coerceIn(0f, 10f)
        if (clamped != localRating) localRating = clamped
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .onSizeChanged { rowWidth = it.width }
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
