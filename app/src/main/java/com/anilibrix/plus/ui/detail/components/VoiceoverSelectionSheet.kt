package com.anilibrix.plus.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anilibrix.plus.domain.model.VoiceoverOption
import com.anilibrix.plus.domain.model.VoiceoverProvider
import com.anilibrix.plus.domain.model.VoiceoverType
import com.anilibrix.plus.ui.theme.Elevation
import com.anilibrix.plus.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceoverSelectionSheet(
    availableVoiceovers: List<VoiceoverOption>,
    selectedVoiceover: VoiceoverOption?,
    onSelectVoiceover: (VoiceoverOption, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rememberForTitle by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(availableVoiceovers, searchQuery) {
        if (searchQuery.isBlank()) availableVoiceovers
        else availableVoiceovers.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val primaryOptions = filteredList.filter { it.provider == VoiceoverProvider.ANILIBRIA }
    val ruVoiceOptions = filteredList.filter { it.provider == VoiceoverProvider.KODIK && it.type == VoiceoverType.VOICE }
    val subOptions = filteredList.filter { it.type == VoiceoverType.SUBTITLES && it.provider != VoiceoverProvider.CONSUMET && it.provider != VoiceoverProvider.ANIFY }
    val foreignOptions = filteredList.filter { it.provider == VoiceoverProvider.CONSUMET || it.provider == VoiceoverProvider.ANIFY }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl)
        ) {
            Text(
                text = "Выбор озвучки",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Доступно вариантов: ${availableVoiceovers.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (availableVoiceovers.size > 6) {
                Spacer(Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск студии озвучки...") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Translate, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(Spacing.md))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                if (primaryOptions.isNotEmpty()) {
                    item {
                        VoiceoverGroupHeader(title = "Официальная")
                    }
                    items(primaryOptions, key = { it.id }) { option ->
                        VoiceoverOptionItem(
                            option = option,
                            isSelected = selectedVoiceover?.id == option.id || (selectedVoiceover == null && option.isDefault),
                            onClick = {
                                onSelectVoiceover(option, rememberForTitle)
                                onDismiss()
                            }
                        )
                    }
                }

                if (ruVoiceOptions.isNotEmpty()) {
                    item {
                        VoiceoverGroupHeader(title = "Русская озвучка")
                    }
                    items(ruVoiceOptions, key = { it.id }) { option ->
                        VoiceoverOptionItem(
                            option = option,
                            isSelected = selectedVoiceover?.id == option.id,
                            onClick = {
                                onSelectVoiceover(option, rememberForTitle)
                                onDismiss()
                            }
                        )
                    }
                }

                if (subOptions.isNotEmpty()) {
                    item {
                        VoiceoverGroupHeader(title = "Субтитры")
                    }
                    items(subOptions, key = { it.id }) { option ->
                        VoiceoverOptionItem(
                            option = option,
                            isSelected = selectedVoiceover?.id == option.id,
                            onClick = {
                                onSelectVoiceover(option, rememberForTitle)
                                onDismiss()
                            }
                        )
                    }
                }

                if (foreignOptions.isNotEmpty()) {
                    item {
                        VoiceoverGroupHeader(title = "Зарубежные стримы")
                    }
                    items(foreignOptions, key = { it.id }) { option ->
                        VoiceoverOptionItem(
                            option = option,
                            isSelected = selectedVoiceover?.id == option.id,
                            onClick = {
                                onSelectVoiceover(option, rememberForTitle)
                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { rememberForTitle = !rememberForTitle }
                    .padding(vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberForTitle,
                    onCheckedChange = { rememberForTitle = it }
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = "Запомнить выбор для этого тайтла",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun VoiceoverGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xs)
    )
}

@Composable
private fun VoiceoverOptionItem(
    option: VoiceoverOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = Elevation.level0,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (option.type) {
                            VoiceoverType.SUBTITLES -> Icons.Rounded.Subtitles
                            else -> Icons.Rounded.Mic
                        },
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(Modifier.width(Spacing.md))

                Column {
                    Text(
                        text = option.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.provider.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (option.episodesCount != null && option.episodesCount > 0) {
                            Text(
                                text = "• ${option.episodesCount} серий",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Выбрано",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
