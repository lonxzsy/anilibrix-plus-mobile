package com.anilibrix.plus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.theme.Sizing
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.crossfade.CrossfadePlugin
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.glide.GlideImage

/**
 * Единая обёртка над загрузкой картинок.
 *
 * Заменяет 30+ прямых вызовов `GlideImage`, ни один из которых не делал
 * crossfade — каждая картинка появлялась рывком, включая бэкдроп на экране
 * тайтла. Плюс плейсхолдеры были разными от места к месту, а обработчик
 * ошибки существовал ровно в одном.
 *
 * @param shape форма применяется и к картинке, и к плейсхолдеру — иначе на
 *   подстановке заметен «прыжок» скругления.
 */
@Composable
fun AnilibrixImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RectangleShape,
) {
    GlideImage(
        imageModel = { model },
        modifier = modifier.clip(shape),
        imageOptions = ImageOptions(
            contentScale = contentScale,
            contentDescription = contentDescription,
        ),
        component = rememberImageComponent {
            +CrossfadePlugin(duration = MotionTokens.IMAGE_CROSSFADE)
        },
        loading = {
            Box(Modifier.matchParentSize().shimmer(shape))
        },
        failure = {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.BrokenImage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(Sizing.iconMd),
                )
            }
        },
    )
}
