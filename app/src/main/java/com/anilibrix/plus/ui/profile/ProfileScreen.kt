@file:Suppress("DEPRECATION")
package com.anilibrix.plus.ui.profile

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anilibrix.plus.ui.components.ErrorView
import com.anilibrix.plus.ui.components.LoadingIndicator
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToChangelog: () -> Unit = {},
    onNavigateToTrending: () -> Unit = {},
    onNavigateToIssues: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    when {
        state.loading && !state.isLoggedIn -> {
            LoadingIndicator()
        }
        state.error != null && !state.isLoggedIn -> {
            ErrorView(
                message = state.error ?: "Ошибка загрузки",
                onRetry = { viewModel.onIntent(ProfileIntent.Load) }
            )
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (state.isLoggedIn) {
                    UserCard(
                        login = state.login ?: "Пользователь",
                        nickname = state.nickname,
                        email = state.email,
                        avatarUrl = state.avatarUrl,
                        createdAt = state.createdAt,
                        isBanned = state.isBanned,
                        uploadedBytes = state.uploadedBytes,
                        downloadedBytes = state.downloadedBytes,
                        favoritesCount = state.favoritesCount,
                        historyCount = state.historyCount,
                        totalWatchTime = state.totalWatchTime
                    )
                } else {
                    NotLoggedInCard(
                        onLoginClick = { viewModel.onIntent(ProfileIntent.ShowAuthSheet) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SettingsItem(
                            icon = if (state.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            title = "Тёмная тема",
                            trailing = {
                                Switch(
                                    checked = state.isDarkTheme,
                                    onCheckedChange = { viewModel.onIntent(ProfileIntent.ToggleTheme) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Новые серии",
                            trailing = {
                                Switch(
                                    checked = state.notificationsNewEpisodesEnabled,
                                    onCheckedChange = {
                                        viewModel.onIntent(ProfileIntent.SetNotificationsNewEpisodesEnabled(it))
                                    }
                                )
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Обновления приложения",
                            trailing = {
                                Switch(
                                    checked = state.notificationsAppUpdatesEnabled,
                                    onCheckedChange = {
                                        viewModel.onIntent(ProfileIntent.SetNotificationsAppUpdatesEnabled(it))
                                    }
                                )
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Статус синхронизации",
                            trailing = {
                                Switch(
                                    checked = state.notificationsSyncStatusEnabled,
                                    onCheckedChange = {
                                        viewModel.onIntent(ProfileIntent.SetNotificationsSyncStatusEnabled(it))
                                    }
                                )
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Качество по умолчанию",
                            trailing = {
                                DropdownSetting(
                                    value = qualityLabel(state.preferredQuality),
                                    options = listOf(
                                        "Auto" to "Авто",
                                        "480" to "480p",
                                        "720" to "720p",
                                        "1080" to "1080p"
                                    ),
                                    onSelect = {
                                        viewModel.onIntent(ProfileIntent.SetPreferredQuality(it))
                                    }
                                )
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Скорость по умолчанию",
                            trailing = {
                                DropdownSetting(
                                    value = "${state.defaultSpeed}x",
                                    options = listOf(
                                        0.5f to "0.5x",
                                        0.75f to "0.75x",
                                        1.0f to "1x",
                                        1.25f to "1.25x",
                                        1.5f to "1.5x",
                                        2.0f to "2x"
                                    ),
                                    onSelect = {
                                        viewModel.onIntent(ProfileIntent.SetDefaultSpeed(it))
                                    }
                                )
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Субтитры",
                            trailing = {
                                Switch(
                                    checked = state.subtitlesEnabled,
                                    onCheckedChange = {
                                        viewModel.onIntent(ProfileIntent.SetSubtitlesEnabled(it))
                                    }
                                )
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Размер субтитров",
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = {
                                            viewModel.onIntent(ProfileIntent.SetSubtitleSize(state.subtitleSize - 2))
                                        }
                                    ) { Text("-") }
                                    Text("${state.subtitleSize} sp")
                                    TextButton(
                                        onClick = {
                                            viewModel.onIntent(ProfileIntent.SetSubtitleSize(state.subtitleSize + 2))
                                        }
                                    ) { Text("+") }
                                }
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Цвет субтитров",
                            trailing = {
                                DropdownSetting(
                                    value = subtitleColorLabel(state.subtitleColor),
                                    options = listOf(
                                        "#FFFFFF" to "Белый",
                                        "#FFD54F" to "Жёлтый",
                                        "#80DEEA" to "Голубой",
                                        "#C5E1A5" to "Зелёный"
                                    ),
                                    onSelect = {
                                        viewModel.onIntent(ProfileIntent.SetSubtitleColor(it))
                                    }
                                )
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Кэш: ${formatBytes(state.cacheSizeBytes)}",
                            trailing = {
                                TextButton(onClick = { viewModel.onIntent(ProfileIntent.ClearCache) }) {
                                    Text("Очистить")
                                }
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Синхронизация",
                            trailing = {
                                Text(
                                    text = state.syncStatus,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "О приложении",
                            onClick = {}
                        )

                        SettingsItem(
                            icon = Icons.Default.TrendingUp,
                            title = "Тренды",
                            onClick = onNavigateToTrending
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsItem(
                            icon = Icons.Default.Schedule,
                            title = "Чейнджлог",
                            onClick = {
                                viewModel.onIntent(ProfileIntent.NavigateToChangelog)
                                onNavigateToChangelog()
                            },
                            trailing = {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Отчёты об ошибках",
                            onClick = onNavigateToIssues,
                            trailing = {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        )
                    }
                }

                if (state.isLoggedIn) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { viewModel.onIntent(ProfileIntent.ShowLogoutDialog) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Выйти из аккаунта")
                    }
                }
            }

            if (state.showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.onIntent(ProfileIntent.DismissLogoutDialog) },
                    title = { Text("Выход из аккаунта") },
                    text = { Text("Вы уверены, что хотите выйти? Локальные данные (избранное, история) останутся на устройстве.") },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.onIntent(ProfileIntent.ConfirmLogout) }
                        ) {
                            Text("Выйти", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.onIntent(ProfileIntent.DismissLogoutDialog) }
                        ) {
                            Text("Отмена")
                        }
                    }
                )
            }

            if (state.showAuthSheet) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { viewModel.onIntent(ProfileIntent.DismissAuthSheet) },
                    sheetState = sheetState
                ) {
                    AuthSheetContent(
                        onLogin = { login, password ->
                            viewModel.onIntent(ProfileIntent.Login(login, password))
                        },
                        loading = state.loading,
                        error = state.error
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    value: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 1000)
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = animatedValue.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun UserCard(
    login: String,
    nickname: String?,
    email: String?,
    avatarUrl: String?,
    createdAt: String?,
    isBanned: Boolean,
    uploadedBytes: Long,
    downloadedBytes: Long,
    favoritesCount: Int,
    historyCount: Int,
    totalWatchTime: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (avatarUrl != null) {
                    GlideImage(
                        imageModel = { avatarUrl },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        imageOptions = ImageOptions(
                            contentScale = ContentScale.Crop,
                            contentDescription = login
                        )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = nickname ?: login,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "@$login",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (email != null) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    if (isBanned) {
                        Text(
                            text = "⛔ Забанен",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (createdAt != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Регистрация: ${formatDate(createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = favoritesCount,
                    label = "Избранное",
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    value = historyCount,
                    label = "Просмотрено",
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    value = (totalWatchTime / 3600000).toInt(),
                    label = "Часов",
                    modifier = Modifier.weight(1f)
                )
            }

            if (uploadedBytes > 0 || downloadedBytes > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Статистика торрентов",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = formatBytes(uploadedBytes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Отдано",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = formatBytes(downloadedBytes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Скачано",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    if (downloadedBytes > 0) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = String.format("%.2f", uploadedBytes.toDouble() / downloadedBytes.toDouble()),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Рейтинг",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_099_511_627_776 -> String.format("%.2f ТБ", bytes / 1_099_511_627_776.0)
        bytes >= 1_073_741_824 -> String.format("%.2f ГБ", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.2f МБ", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.2f КБ", bytes / 1024.0)
        else -> "$bytes Б"
    }
}

@Composable
private fun <T> DropdownSetting(
    value: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(value)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (option, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun qualityLabel(quality: String): String {
    return when (quality) {
        "Auto" -> "Авто"
        else -> if (quality.endsWith("p", ignoreCase = true)) quality else "${quality}p"
    }
}

private fun subtitleColorLabel(color: String): String {
    return when (color.uppercase()) {
        "#FFD54F" -> "Жёлтый"
        "#80DEEA" -> "Голубой"
        "#C5E1A5" -> "Зелёный"
        else -> "Белый"
    }
}

private fun formatDate(dateString: String): String {
    return try {
        // Assuming ISO 8601 format: "2024-01-15T10:30:00Z"
        val parts = dateString.split("T")[0].split("-")
        if (parts.size == 3) {
            "${parts[2]}.${parts[1]}.${parts[0]}"
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}

@Composable
private fun NotLoggedInCard(
    onLoginClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Войдите в аккаунт",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Чтобы синхронизировать избранное и историю между устройствами",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Войти")
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
private fun AuthSheetContent(
    onLogin: (String, String) -> Unit,
    loading: Boolean,
    error: String?
) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = "Вход в аккаунт",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Войдите в аккаунт Anilibria для синхронизации данных",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            label = { Text("Логин") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !loading
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            enabled = !loading
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onLogin(login, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = login.isNotBlank() && password.isNotBlank() && !loading
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Войти")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
