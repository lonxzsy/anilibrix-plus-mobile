package com.anilibrix.plus.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.anilibrix.plus.ui.theme.AnilibrixTypeExtras
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing

/**
 * Заголовок секции.
 *
 * Снимает расхождение: на главной это был `titleLarge` обычного начертания,
 * а в библиотеке — `titleMedium` + Bold, хотя роль одна и та же. Отступы
 * теперь внутри компонента, поэтому места вызова больше не расставляют
 * `Spacer(8.dp)` вручную.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onSeeAll: (() -> Unit)? = null,
    seeAllLabel: String = "Все",
    /**
     * Ноль, когда заголовок лежит внутри контейнера, который уже задал боковые
     * поля через `contentPadding` — иначе отступ удвоится.
     */
    horizontalPadding: Dp = Spacing.screenHorizontal,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = if (onSeeAll != null) Spacing.sm else horizontalPadding,
                top = Spacing.sm,
                bottom = Spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AnilibrixTypeExtras.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text(text = seeAllLabel, style = MaterialTheme.typography.labelLarge)
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(Sizing.iconSm),
                )
            }
        }
    }
}

/**
 * Заголовок группы настроек. Отдельный компонент, потому что визуальный вес
 * у него другой: это подпись группы, а не заголовок раздела контента.
 */
@Composable
fun SettingsGroupHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            start = Spacing.screenHorizontal,
            end = Spacing.screenHorizontal,
            top = Spacing.lg,
            bottom = Spacing.sm,
        ),
    )
}
