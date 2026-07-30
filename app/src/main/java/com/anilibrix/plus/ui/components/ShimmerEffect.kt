package com.anilibrix.plus.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.anilibrix.plus.ui.theme.Spacing

/**
 * Совместимость со старым API скелетонов.
 *
 * Реализация целиком переехала на [Modifier.shimmer] и [Skeletons] — здесь
 * остались только тонкие обёртки, чтобы ещё не переписанные экраны собирались.
 * Удаляются вместе с последним местом использования.
 */

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    Spacer(modifier.shimmer(shape))
}

/**
 * Скелетон главной.
 *
 * Оставлен для совместимости; в Фазе 5 главная перейдёт на скелетоны внутри
 * настоящей обвязки экрана, а полноэкранная подмена уйдёт.
 */
@Composable
fun HomeShimmer(modifier: Modifier = Modifier) {
    ShimmerHost {
        Column(modifier = modifier) {
            HeroSkeleton()
            Spacer(modifier = Modifier.height(Spacing.xl))
            ShimmerBox(
                modifier = Modifier
                    .padding(horizontal = Spacing.screenHorizontal)
                    .fillMaxWidth(0.4f)
                    .height(20.dp),
                shape = MaterialTheme.shapes.extraSmall,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            RailSkeleton()
            Spacer(modifier = Modifier.height(Spacing.xl))
            ShimmerBox(
                modifier = Modifier
                    .padding(horizontal = Spacing.screenHorizontal)
                    .fillMaxWidth(0.5f)
                    .height(20.dp),
                shape = MaterialTheme.shapes.extraSmall,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            RailSkeleton()
        }
    }
}

@Composable
fun ShimmerRow(modifier: Modifier = Modifier) {
    RailSkeleton(modifier)
}
