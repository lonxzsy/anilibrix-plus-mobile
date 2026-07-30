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
import com.anilibrix.plus.ui.components.AnilibrixBottomSheet
import com.anilibrix.plus.ui.components.rememberHaptics
import com.anilibrix.plus.ui.theme.AnilibrixTypeExtras
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing

/** Одна выбираемая дорожка — качество, скорость или субтитры. */
data class TrackOption(
    val id: String,
    val label: String,
    val supporting: String? = null,
)

/**
 * Единая шторка выбора: качество, скорость, субтитры.
 *
 * Раньше качество и скорость жили в двух выпадающих меню в углу экрана —
 * на телефоне до них надо тянуться через весь экран, и попасть одной рукой
 * почти невозможно. Шторка снизу решает это и заодно собирает всё, что
 * относится к «как это играет», в одном месте.
 *
 * Про субтитры честно: Anilibria не отдаёт отдельных субтитровых дорожек —
 * в ответе API есть только HLS-потоки. Поэтому список текстовых дорожек
 * заполняется тем, что нашлось в самом манифесте, а если там пусто, остаётся
 * единственный работающий путь — загрузить файл с устройства.
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
