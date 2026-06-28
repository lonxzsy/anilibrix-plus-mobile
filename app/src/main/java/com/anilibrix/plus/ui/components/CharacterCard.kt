package com.anilibrix.plus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

data class CharacterCardData(
    val id: String,
    val imageUrl: String,
    val name: String,
    val role: String,
    val seiyuuName: String? = null,
    val seiyuuImageUrl: String? = null
)

@Composable
fun CharacterCard(
    data: CharacterCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlideImage(
                imageModel = { data.imageUrl },
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape),
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    contentDescription = data.name
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = data.role,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (data.seiyuuName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (data.seiyuuImageUrl != null) {
                            GlideImage(
                                imageModel = { data.seiyuuImageUrl },
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape),
                                imageOptions = ImageOptions(
                                    contentScale = ContentScale.Crop,
                                    contentDescription = data.seiyuuName
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = data.seiyuuName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
