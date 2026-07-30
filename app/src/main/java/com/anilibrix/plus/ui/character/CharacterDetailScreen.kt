package com.anilibrix.plus.ui.character

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anilibrix.plus.domain.model.CharacterAppearance
import com.anilibrix.plus.domain.model.Seiyuu
import com.anilibrix.plus.ui.components.AnilibrixImage
import com.anilibrix.plus.ui.components.EmptyKind
import com.anilibrix.plus.ui.components.EmptyState
import com.anilibrix.plus.ui.components.ErrorView
import com.anilibrix.plus.ui.components.ScreenState
import com.anilibrix.plus.ui.components.ScreenStateHost
import com.anilibrix.plus.ui.components.SectionHeader
import com.anilibrix.plus.ui.components.pressScale
import com.anilibrix.plus.ui.navigation.screenContentPadding
import com.anilibrix.plus.ui.theme.AnilibrixShapeExtras
import com.anilibrix.plus.ui.theme.AnilibrixTypeExtras
import com.anilibrix.plus.ui.theme.Elevation
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing
import com.anilibrix.plus.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    malId: Long,
    viewModel: CharacterDetailViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(malId) {
        viewModel.load(malId)
    }

    val screenState = when {
        state.loading -> ScreenState.Loading
        state.error != null -> ScreenState.Error
        state.character == null -> ScreenState.Empty
        else -> ScreenState.Content
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.character?.name ?: "Персонаж",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { padding ->
        ScreenStateHost(
            state = screenState,
            modifier = Modifier.fillMaxSize(),
            onRetry = { viewModel.load(malId) },
            loading = { LoadingIndicator(modifier = Modifier.padding(padding)) },
            error = {
                ErrorView(
                    message = state.error ?: "Ошибка загрузки",
                    onRetry = { viewModel.load(malId) }
                )
            },
            empty = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(kind = EmptyKind.Characters)
                }
            },
        ) {
            val character = state.character ?: return@ScreenStateHost

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = screenContentPadding(padding),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                item(key = "header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                        verticalAlignment = Alignment.Top
                    ) {
                        AnilibrixImage(
                            model = character.imageUrl,
                            contentDescription = character.name,
                            modifier = Modifier.size(Sizing.avatarLg),
                            shape = CircleShape,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = character.name,
                                style = AnilibrixTypeExtras.titleLargeEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (character.nameKanji != null) {
                                Text(
                                    text = character.nameKanji,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (character.favorites > 0) {
                                Spacer(Modifier.height(Spacing.sm))
                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = { Text("В избранном у ${character.favorites}") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.Favorite,
                                            contentDescription = null,
                                            modifier = Modifier.size(Sizing.iconSm),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                if (character.nicknames.isNotEmpty()) {
                    item(key = "nicknames") {
                        Text(
                            text = "Также известен как: ${character.nicknames.joinToString(", ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (character.about != null) {
                    item(key = "about") { AboutSection(character.about) }
                }

                if (character.voiceActors.isNotEmpty()) {
                    item(key = "voices-header") {
                        SectionHeader(title = "Озвучка", horizontalPadding = Spacing.none)
                    }
                    items(character.voiceActors, key = { "voice-${it.malId}-${it.role}" }) { seiyuu ->
                        SeiyuuRow(seiyuu)
                    }
                }

                if (character.appearsIn.isNotEmpty()) {
                    item(key = "anime-header") {
                        SectionHeader(title = "Появляется в", horizontalPadding = Spacing.none)
                    }
                    items(character.appearsIn, key = { "anime-${it.malId}" }) { appearance ->
                        AppearanceRow(appearance)
                    }
                }
            }
        }
    }
}

/**
 * Биография сворачивается до четырёх строк.
 *
 * У Jikan описания персонажей нередко на пару экранов, и без сворачивания
 * всё остальное — озвучка, список тайтлов — уезжает так далеко, что до него
 * не долистывают.
 */
@Composable
private fun AboutSection(about: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(MotionTokens.spatialDefault())
    ) {
        Text(
            text = about,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = if (expanded) "Свернуть" else "Читать полностью",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SeiyuuRow(seiyuu: Seiyuu) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnilibrixImage(
                model = seiyuu.imageUrl,
                contentDescription = seiyuu.name,
                modifier = Modifier.size(Sizing.avatarSm),
                shape = CircleShape,
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column {
                Text(
                    text = seiyuu.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (seiyuu.role != null) {
                    Text(
                        text = seiyuu.role,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceRow(appearance: CharacterAppearance) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnilibrixImage(
                model = appearance.imageUrl,
                contentDescription = appearance.title,
                modifier = Modifier.size(width = Sizing.avatarSm, height = Sizing.avatarMd),
                shape = AnilibrixShapeExtras.poster,
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column {
                Text(
                    text = appearance.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (appearance.role != null) {
                    Text(
                        text = appearance.role,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
