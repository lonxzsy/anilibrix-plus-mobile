package com.anilibrix.plus.ui.detail.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anilibrix.plus.domain.model.FranchiseItem
import com.anilibrix.plus.ui.components.AnilibrixImage
import com.anilibrix.plus.ui.components.EmptyKind
import com.anilibrix.plus.ui.components.EmptyState
import com.anilibrix.plus.ui.components.LoadingIndicator
import com.anilibrix.plus.ui.components.pressScale
import com.anilibrix.plus.ui.detail.DetailUiState
import com.anilibrix.plus.ui.detail.RelatedTitleItem
import com.anilibrix.plus.ui.theme.AnilibrixShapeExtras
import com.anilibrix.plus.ui.theme.Elevation
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing

fun LazyListScope.franchiseSection(
    state: DetailUiState,
    onTitleClick: (Long) -> Unit
) {
    val hasRelated = state.franchise.isNotEmpty() || state.relatedItems.isNotEmpty()

    when {
        !hasRelated && state.relatedLoading -> {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xxl),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
        }
        !hasRelated -> {
            item {
                EmptyState(
                    kind = EmptyKind.Related,
                    title = "Нет связанных тайтлов",
                    subtitle = "Другие сезоны, фильмы и спин-оффы отобразятся здесь при их наличии",
                    modifier = Modifier.padding(vertical = Spacing.xl)
                )
            }
        }
        else -> {
            if (state.franchise.isNotEmpty()) {
                item {
                    Text(
                        text = "Хронология франшизы",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        state.franchise.forEach { franchiseItem ->
                            FranchiseCardItem(
                                item = franchiseItem,
                                onClick = { onTitleClick(franchiseItem.id) }
                            )
                        }
                    }
                }
            }

            if (state.relatedItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    Text(
                        text = "Связанное на Shikimori",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        state.relatedItems.forEach { relatedItem ->
                            RelatedTitleCardItem(
                                item = relatedItem,
                                onClick = {
                                    relatedItem.anilibriaId?.let(onTitleClick)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FranchiseCardItem(
    item: FranchiseItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(130.dp)
            .pressScale(),
        shape = MaterialTheme.shapes.medium,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0)
    ) {
        Column {
            AnilibrixImage(
                model = item.poster?.cardUrl ?: item.poster?.fullUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(Sizing.POSTER_ASPECT),
                shape = AnilibrixShapeExtras.poster
            )
            Column(modifier = Modifier.padding(Spacing.sm)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!item.relation.isNullOrBlank()) {
                    Text(
                        text = item.relation,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun RelatedTitleCardItem(
    item: RelatedTitleItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(130.dp)
            .pressScale(),
        shape = MaterialTheme.shapes.medium,
        onClick = onClick,
        enabled = item.anilibriaId != null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0)
    ) {
        Column {
            AnilibrixImage(
                model = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(Sizing.POSTER_ASPECT),
                shape = AnilibrixShapeExtras.poster
            )
            Column(modifier = Modifier.padding(Spacing.sm)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!item.relation.isNullOrBlank()) {
                    Text(
                        text = item.relation,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
