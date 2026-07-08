package com.anilibrix.plus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class ShortcutEntry(
    val key: String,
    val action: String
)

@Composable
fun ShortcutsModal(
    onDismiss: () -> Unit
) {
    val shortcuts = listOf(
        ShortcutEntry("Space", "Play / Pause"),
        ShortcutEntry("← / →", "Seek ±10s"),
        ShortcutEntry("↑ / ↓", "Volume ±10%"),
        ShortcutEntry("F", "Fullscreen"),
        ShortcutEntry("M", "Mute / Unmute"),
        ShortcutEntry("Esc", "Exit fullscreen"),
        ShortcutEntry("C", "Subtitles"),
        ShortcutEntry("0-9", "Seek 0-90%"),
        ShortcutEntry("> / .", "Speed up"),
        ShortcutEntry("< / ,", "Speed down")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Горячие клавиши",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                shortcuts.forEachIndexed { index, shortcut ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = shortcut.key,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = shortcut.action,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End
                        )
                    }
                    if (index < shortcuts.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}
