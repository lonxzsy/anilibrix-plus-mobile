package com.anilibrix.plus.ui.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.ui.components.AnilibrixBottomSheet
import com.anilibrix.plus.ui.components.rememberHaptics
import com.anilibrix.plus.ui.theme.AnilibrixTypeExtras
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing

/**
 * Выбор статуса просмотра.
 *
 * Пять статусов существовали в модели, в базе и на сервере с самого начала, но
 * в интерфейсе был выведен ровно один — «Буду смотреть». Остальные четыре не
 * имели ни одной точки входа, то есть готовый трекер был просто не подключён.
 *
 * Радиокнопки, а не переключатели: статус ровно один, и список должен это
 * показывать самой своей формой.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CollectionStatusSheet(
    current: CollectionType?,
    onSelect: (CollectionType) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberHaptics()

    AnilibrixBottomSheet(onDismiss = onDismiss) {
        Text(
            text = "Статус просмотра",
            style = AnilibrixTypeExtras.titleMediumEmphasized,
            modifier = Modifier.padding(
                start = Spacing.lg,
                end = Spacing.lg,
                bottom = Spacing.sm,
            ),
        )

        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(Spacing.none),
        ) {
            CollectionType.entries.forEach { type ->
                val selected = type == current
                ListItem(
                    headlineContent = { Text(type.displayName) },
                    supportingContent = { Text(type.hint()) },
                    leadingContent = {
                        Icon(
                            imageVector = type.icon(),
                            contentDescription = null,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(Sizing.iconMd),
                        )
                    },
                    trailingContent = {
                        RadioButton(selected = selected, onClick = null)
                    },
                    // Прозрачный контейнер: ListItem по умолчанию рисует
                    // `surface`, а шторка стоит на `surfaceContainerLow` —
                    // без этого под каждой строкой видна полоса другого тона.
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = {
                            haptics.confirm()
                            onSelect(type)
                        },
                    ),
                )
            }
        }

        if (current != null) {
            Spacer(Modifier.height(Spacing.sm))
            ListItem(
                headlineContent = {
                    Text(
                        text = "Убрать из списка",
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Sizing.iconMd),
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.toggleOff()
                        onClear()
                    },
            )
        }

        Spacer(Modifier.height(Spacing.sm))
    }
}

/** Иконка статуса — она же показывается в топбаре, поэтому вынесена сюда. */
fun CollectionType?.icon(): ImageVector = when (this) {
    null -> Icons.Rounded.BookmarkBorder
    CollectionType.WATCH_LATER -> Icons.Rounded.WatchLater
    CollectionType.WATCHING -> Icons.Rounded.PlayCircleOutline
    CollectionType.COMPLETED -> Icons.Rounded.CheckCircle
    CollectionType.ON_HOLD -> Icons.Rounded.PauseCircleOutline
    CollectionType.DROPPED -> Icons.Rounded.Cancel
}

fun CollectionType?.filledIcon(): ImageVector =
    if (this == null) Icons.Rounded.BookmarkBorder else Icons.Rounded.Bookmark

private fun CollectionType.hint(): String = when (this) {
    CollectionType.WATCH_LATER -> "Отложено на потом"
    CollectionType.WATCHING -> "Сейчас в процессе"
    CollectionType.COMPLETED -> "Досмотрено до конца"
    CollectionType.ON_HOLD -> "Пауза, вернусь позже"
    CollectionType.DROPPED -> "Не буду досматривать"
}
