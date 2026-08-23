package com.anilibrix.plus.ui.player

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.anilibrix.plus.ui.components.AnilibrixBottomSheet
import com.anilibrix.plus.ui.components.rememberHaptics
import com.anilibrix.plus.ui.theme.AnilibrixTypeExtras
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.ui.Alignment

/** Одна выбираемая дорожка — качество, скорость или субтитры. */
data class TrackOption(
    val id: String,
    val label: String,
    val supporting: String? = null,
)

/**
 * Единая шторка выбора: качество, скорость, субтитры и синхронизация.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracksSheet(
    qualities: List<TrackOption>,
    selectedQuality: String,
    onQualitySelected: (String) -> Unit,
    speeds: List<TrackOption>,
    selectedSpeed: String,
    onSpeedSelected: (String) -> Unit,
    subtitleTracks: List<TrackOption>,
    selectedSubtitle: String?,
    onSubtitleSelected: (String?) -> Unit,
    onLoadSubtitleFile: () -> Unit,
    audioDelayMs: Long = 0L,
    onAudioDelayChange: (Long) -> Unit = {},
    subtitleDelayMs: Long = 0L,
    onSubtitleDelayChange: (Long) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val haptics = rememberHaptics()

    AnilibrixBottomSheet(onDismiss = onDismiss) {
        SheetSection(
            title = "Качество",
            icon = { Icon(Icons.Rounded.HighQuality, contentDescription = null, modifier = Modifier.size(Sizing.iconMd)) },
        )
        OptionGroup(
            options = qualities,
            selectedId = selectedQuality,
            onSelect = {
                haptics.tick()
                onQualitySelected(it)
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))

        SheetSection(
            title = "Скорость",
            icon = { Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(Sizing.iconMd)) },
        )
        OptionGroup(
            options = speeds,
            selectedId = selectedSpeed,
            onSelect = {
                haptics.tick()
                onSpeedSelected(it)
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))

        SheetSection(
            title = "Субтитры",
            icon = { Icon(Icons.Rounded.ClosedCaption, contentDescription = null, modifier = Modifier.size(Sizing.iconMd)) },
        )

        ListItem(
            headlineContent = { Text("Выключены") },
            trailingContent = { RadioButton(selected = selectedSubtitle == null, onClick = null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.selectable(
                selected = selectedSubtitle == null,
                role = Role.RadioButton,
                onClick = {
                    haptics.tick()
                    onSubtitleSelected(null)
                },
            ),
        )

        OptionGroup(
            options = subtitleTracks,
            selectedId = selectedSubtitle,
            onSelect = {
                haptics.tick()
                onSubtitleSelected(it)
            },
        )

        if (subtitleTracks.isEmpty()) {
            Text(
                text = "У этого потока нет встроенных субтитров",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            )
        }

        ListItem(
            headlineContent = { Text("Загрузить файл…") },
            supportingContent = { Text("SRT или VTT с устройства") },
            leadingContent = {
                Icon(
                    Icons.Rounded.FileOpen,
                    contentDescription = null,
                    modifier = Modifier.size(Sizing.iconMd),
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = false,
                    onClick = {
                        haptics.confirm()
                        onLoadSubtitleFile()
                    },
                ),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))

        SheetSection(
            title = "Синхронизация",
            icon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(Sizing.iconMd)) },
        )

        ListItem(
            headlineContent = { Text("Смещение субтитров") },
            supportingContent = { Text("${if (subtitleDelayMs > 0) "+" else ""}${subtitleDelayMs} мс") },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    OutlinedIconButton(
                        onClick = {
                            haptics.tick()
                            onSubtitleDelayChange(subtitleDelayMs - 50L)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "-50ms", modifier = Modifier.size(18.dp))
                    }
                    OutlinedIconButton(
                        onClick = {
                            haptics.tick()
                            onSubtitleDelayChange(subtitleDelayMs + 50L)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "+50ms", modifier = Modifier.size(18.dp))
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )

        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun SheetSection(title: String, icon: @Composable () -> Unit) {
    ListItem(
        headlineContent = {
            Text(text = title, style = AnilibrixTypeExtras.titleMediumEmphasized)
        },
        leadingContent = icon,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionGroup(
    options: List<TrackOption>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    if (options.isEmpty()) return
    androidx.compose.foundation.layout.Column(modifier = Modifier.selectableGroup()) {
        options.forEach { option ->
            val selected = option.id == selectedId
            ListItem(
                headlineContent = { Text(option.label) },
                supportingContent = option.supporting?.let { { Text(it) } },
                trailingContent = { RadioButton(selected = selected, onClick = null) },
                // Прозрачный контейнер: у ListItem по умолчанию `surface`,
                // а шторка стоит на `surfaceContainerLow` — иначе под каждой
                // строкой видна полоса другого тона.
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = { onSelect(option.id) },
                ),
            )
        }
    }
}
