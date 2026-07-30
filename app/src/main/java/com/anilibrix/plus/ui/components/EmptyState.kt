package com.anilibrix.plus.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GroupOff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing

/**
 * Контекст пустого состояния.
 *
 * Раньше во всех четырёх вкладках библиотеки рисовалась одна и та же иконка
 * закладки, а на главной, в результатах поиска и в дне расписания пустого
 * состояния не было вовсе — пользователь видел просто пустой экран.
 */
enum class EmptyKind(
    val icon: ImageVector,
    val defaultTitle: String,
    val defaultSubtitle: String,
) {
    Favorites(
        Icons.Rounded.StarBorder,
        "Нет избранного",
        "Добавляйте аниме в избранное из карточки тайтла",
    ),
    WatchLater(
        Icons.Rounded.WatchLater,
        "Список пуст",
        "Сохраняйте тайтлы, чтобы посмотреть позже",
    ),
    History(
        Icons.Rounded.History,
        "История пуста",
        "Начните смотреть — серии появятся здесь",
    ),
    Playlists(
        Icons.AutoMirrored.Rounded.PlaylistAdd,
        "Нет плейлистов",
        "Создайте плейлист, чтобы собирать аниме",
    ),
    SearchResult(
        Icons.Rounded.SearchOff,
        "Ничего не найдено",
        "Измените запрос или сбросьте фильтры",
    ),
    Schedule(
        Icons.Rounded.EventBusy,
        "Нет релизов",
        "В этот день ничего не выходит",
    ),
    Episodes(
        Icons.Rounded.VideocamOff,
        "Нет эпизодов",
        "Серии ещё не добавлены",
    ),
    Characters(
        Icons.Rounded.GroupOff,
        "Нет персонажей",
        "Данные пока недоступны",
    ),
    Related(
        Icons.Rounded.LinkOff,
        "Нет связанных тайтлов",
        "",
    ),
    Torrents(
        Icons.Rounded.CloudOff,
        "Нет торрентов",
        "",
    ),
    Home(
        Icons.Rounded.Explore,
        "Пока пусто",
        "Потяните вниз, чтобы обновить",
    ),
}

/**
 * Пустое состояние.
 *
 * По умолчанию занимает ширину и фиксированный вертикальный отступ, чтобы
 * работать как `item {}` внутри LazyColumn. Раньше это был `Box(fillMaxSize)`
 * внутри `Column` без веса — его сплющивало.
 */
@Composable
fun EmptyState(
    kind: EmptyKind,
    modifier: Modifier = Modifier,
    title: String = kind.defaultTitle,
    subtitle: String = kind.defaultSubtitle,
    action: (@Composable () -> Unit)? = null,
) {
    val appear = remember { MutableTransitionState(false).apply { targetState = true } }

    AnimatedVisibility(
        visibleState = appear,
        enter = fadeIn(MotionTokens.effectsDefault()) +
            scaleIn(MotionTokens.spatialDefault(), initialScale = 0.9f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl, vertical = Spacing.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(Sizing.avatarLg)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = kind.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Sizing.iconLg),
                )
            }

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            if (action != null) {
                Spacer(Modifier.height(Spacing.lg))
                action()
            }
        }
    }
}
