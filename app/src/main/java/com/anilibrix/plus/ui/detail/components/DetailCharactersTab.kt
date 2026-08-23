package com.anilibrix.plus.ui.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anilibrix.plus.ui.components.CharacterCard
import com.anilibrix.plus.ui.components.CharacterCardData
import com.anilibrix.plus.ui.components.EmptyKind
import com.anilibrix.plus.ui.components.EmptyState
import com.anilibrix.plus.ui.components.LoadingIndicator
import com.anilibrix.plus.ui.detail.DetailUiState
import com.anilibrix.plus.ui.theme.Spacing

fun LazyListScope.charactersSection(
    state: DetailUiState,
    onCharacterClick: (Long) -> Unit
) {
    when {
        state.charactersLoading -> {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xxl),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
        }
        state.characterItems.isEmpty() -> {
            item {
                EmptyState(
                    kind = EmptyKind.Characters,
                    title = "Нет данных о персонажах",
                    subtitle = "Информация о героях и сэйю появится при наличии данных",
                    modifier = Modifier.padding(vertical = Spacing.xl)
                )
            }
        }
        else -> {
            val chunked = state.characterItems.chunked(2)
            items(
                items = chunked,
                key = { it.first().id }
            ) { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    row.forEach { character ->
                        Box(modifier = Modifier.weight(1f)) {
                            CharacterCard(
                                data = CharacterCardData(
                                    id = character.id,
                                    imageUrl = character.imageUrl.orEmpty(),
                                    name = character.name,
                                    role = character.role.orEmpty(),
                                    seiyuuName = character.seiyuuName,
                                    seiyuuImageUrl = character.seiyuuImageUrl
                                ),
                                onClick = {
                                    character.malId?.let(onCharacterClick)
                                }
                            )
                        }
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
