package com.anilibrix.plus.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import com.anilibrix.plus.ui.theme.AnilibrixBrushes

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(AnilibrixBrushes.shimmerBrush)
            .alpha(alpha)
    )
}

@Composable
fun HomeShimmer(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        HeroCarouselShimmer()
        Spacer(modifier = Modifier.height(24.dp))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(20.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        ShimmerRow()
        Spacer(modifier = Modifier.height(24.dp))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(20.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        ShimmerRow()
    }
}

@Composable
fun ShimmerRow(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(modifier = modifier) {
        repeat(4) {
            Column(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .width(140.dp)
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(10.dp)
                )
            }
        }
    }
}
