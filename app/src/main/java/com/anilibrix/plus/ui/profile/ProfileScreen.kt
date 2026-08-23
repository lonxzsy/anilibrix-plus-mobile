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
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.anilibrix.plus.ui.theme.Sizing
import com.anilibrix.plus.ui.theme.Spacing
import com.anilibrix.plus.ui.theme.ThemeMode
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material.icons.rounded.Wifi
import com.anilibrix.plus.ui.player.SkipMode
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anilibrix.plus.BuildConfig
import com.anilibrix.plus.ui.components.SettingsGroupHeader
import com.anilibrix.plus.ui.navigation.LocalBottomBarHeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToChangelog: () -> Unit = {},
    onNavigateToTrending: () -> Unit = {},
    onNavigateToIssues: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    shikimoriCode: String? = null,
    onShikimoriCodeConsumed: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                viewModel.onIntent(ProfileIntent.ExportBackup(outputStream))
            }
        }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                viewModel.onIntent(ProfileIntent.ImportBackup(inputStream))
            }
        }
    }

    LaunchedEffect(shikimoriCode) {
        val code = shikimoriCode ?: return@LaunchedEffect
        viewModel.onIntent(ProfileIntent.HandleShikimoriRedirect(code))
        onShikimoriCodeConsumed()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // Обычный топбар, а не LargeTopAppBar: у крупного развёрнутая
            // высота 152dp, и над заголовком оставалось около сотни точек
            // пустоты — она читалась как дыра, а не как воздух.
            // Заодно шапка стала одинаковой со всеми остальными экранами.
            TopAppBar(
                title = { Text("Профиль") },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { innerPadding ->
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
                    .padding(top = innerPadding.calculateTopPadding())
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = Spacing.screenHorizontal,
                        end = Spacing.screenHorizontal,
                        bottom = LocalBottomBarHeight.current + Spacing.lg,
                    )
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

                SettingsGroupHeader("Внешний вид")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(vertical = Spacing.sm)) {
                        ThemeModeSetting(
                            mode = state.themeMode,
                            onModeChange = { viewModel.onIntent(ProfileIntent.SetThemeMode(it)) }
                        )

                        SettingsItem(
                            icon = Icons.Default.Palette,
                            title = "Material You",
                            trailing = {
                                Switch(
                                    checked = state.dynamicColor,
                                    onCheckedChange = {
                                        viewModel.onIntent(ProfileIntent.SetDynamicColor(it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        )

                    }
                }

                SettingsGroupHeader("Уведомления")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(vertical = Spacing.sm)) {
                        SettingsItem(
                            icon = Icons.Default.NotificationsActive,
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
                            icon = Icons.Default.SystemUpdate,
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
                            icon = Icons.Default.CloudSync,
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

                        SettingsItem(
                            icon = Icons.Rounded.Replay,
                            title = "Напоминать о недосмотренном",
                            supportingText = "Не чаще раза в день",
                            trailing = {
                                Switch(
                                    checked = state.notificationsResumeEnabled,
                                    onCheckedChange = {
                                        viewModel.onIntent(ProfileIntent.SetNotificationsResumeEnabled(it))
                                    }
                                )
                            }
                        )

                        SettingsItem(
                            icon = Icons.Rounded.Bedtime,
                            title = "Тихие часы",
                            supportingText = "Не беспокоить с 23:00 до 08:00",
                            trailing = {
                                Switch(
                                    checked = state.notificationsQuietHours,
                                    onCheckedChange = {
                                        viewModel.onIntent(ProfileIntent.SetNotificationsQuietHours(it))
                                    }
                                )
                            }
                        )
                    }
                }

                SettingsGroupHeader("Плеер")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(vertical = Spacing.sm)) {
                        // Раньше выбора не было: приложение всегда пропускало
                        // заставки само через три секунды.
                        SettingsItem(
                            icon = Icons.Rounded.SkipNext,
                            title = "Опенинг и эндинг",
                            supportingText = state.skipMode.displayName,
                            trailing = {
                                DropdownSetting(
                                    value = state.skipMode.displayName,
                                    options = SkipMode.entries.map { it to it.displayName },
                                    onSelect = { viewModel.onIntent(ProfileIntent.SetSkipMode(it)) }
                                )
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.HighQuality,
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
                            icon = Icons.Default.Speed,
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
                            icon = Icons.Default.Subtitles,
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
                            icon = Icons.Default.FormatSize,
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
                            icon = Icons.Default.ColorLens,
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

                    }
                }

                SettingsGroupHeader("Синхронизация с Shikimori")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(vertical = Spacing.sm)) {
                        when {
                            // Ключи приложения задаются при сборке. Показывать
                            // кнопку, которая заведомо не сработает, — хуже,
                            // чем честно объяснить, почему её нет.
                            !state.shikimoriConfigured -> {
                                SettingsItem(
                                    icon = Icons.Default.CloudSync,
                                    title = "Синхронизация недоступна",
                                    supportingText = "В этой сборке не заданы ключи приложения Shikimori",
                                )
                            }

                            state.shikimoriNickname == null -> {
                                SettingsItem(
                                    icon = Icons.Default.CloudSync,
                                    title = "Привязать Shikimori",
                                    supportingText = "Статусы, прогресс и оценки будут синхронизироваться",
                                    onClick = { viewModel.onIntent(ProfileIntent.LinkShikimori) },
                                    trailing = {
                                        if (state.shikimoriSyncing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(Sizing.iconMd),
                                                strokeWidth = 2.dp,
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.ArrowForward,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(Sizing.iconSm),
                                            )
                                        }
                                    }
                                )
                            }

                            else -> {
                                SettingsItem(
                                    icon = Icons.Default.Person,
                                    title = state.shikimoriNickname.orEmpty(),
                                    supportingText = lastSyncLabel(state.shikimoriLastSync),
                                )

                                SettingsItem(
                                    icon = Icons.Default.Sync,
                                    title = "Синхронизировать сейчас",
                                    onClick = { viewModel.onIntent(ProfileIntent.SyncNow) },
                                    trailing = {
                                        if (state.shikimoriSyncing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(Sizing.iconMd),
                                                strokeWidth = 2.dp,
                                            )
                                        }
                                    }
                                )

                                SettingsItem(
                                    icon = Icons.Default.CloudSync,
                                    title = "Обновлять статус при просмотре",
                                    trailing = {
                                        Switch(
                                            checked = state.shikimoriPushStatus,
                                            onCheckedChange = {
                                                viewModel.onIntent(ProfileIntent.SetShikimoriPushStatus(it))
                                            }
                                        )
                                    }
                                )

                                SettingsItem(
                                    icon = Icons.Rounded.StarRate,
                                    title = "Синхронизировать оценки",
                                    trailing = {
                                        Switch(
                                            checked = state.shikimoriPushRatings,
                                            onCheckedChange = {
                                                viewModel.onIntent(ProfileIntent.SetShikimoriPushRatings(it))
                                            }
                                        )
                                    }
                                )

                                SettingsItem(
                                    icon = Icons.Default.ExitToApp,
                                    title = "Отвязать аккаунт",
                                    titleColor = MaterialTheme.colorScheme.error,
                                    onClick = { viewModel.onIntent(ProfileIntent.UnlinkShikimori) },
                                )
                            }
                        }
                    }
                }

                SettingsGroupHeader("Загрузки")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(vertical = Spacing.sm)) {
                        SettingsItem(
                            icon = Icons.Rounded.Download,
                            title = "Скачанные серии",
                            supportingText = if (state.downloadsUsedBytes > 0) {
                                "Занято ${formatBytes(state.downloadsUsedBytes)}"
                            } else {
                                "Ничего не скачано"
                            },
                            onClick = onNavigateToDownloads,
                            trailing = {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(Sizing.iconSm),
                                )
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.HighQuality,
                            title = "Качество загрузки",
                            trailing = {
                                DropdownSetting(
                                    value = qualityLabel(state.downloadQuality),
                                    options = listOf(
                                        "480" to "480p",
                                        "720" to "720p",
                                        "1080" to "1080p"
                                    ),
                                    onSelect = { viewModel.onIntent(ProfileIntent.SetDownloadQuality(it)) }
                                )
                            }
                        )

                        SettingsItem(
                            icon = Icons.Rounded.Wifi,
                            title = "Только по Wi-Fi",
                            supportingText = "Загрузки приостановятся на мобильной сети",
                            trailing = {
                                Switch(
                                    checked = state.downloadWifiOnly,
                                    onCheckedChange = {
                                        viewModel.onIntent(ProfileIntent.SetDownloadWifiOnly(it))
                                    }
                                )
                            }
                        )
                    }
                }

                SettingsGroupHeader("Данные")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(vertical = Spacing.sm)) {
                        SettingsItem(
                            icon = Icons.Default.Storage,
                            title = "Кэш",
                            supportingText = formatBytes(state.cacheSizeBytes),
                            trailing = {
                                TextButton(onClick = { viewModel.onIntent(ProfileIntent.ClearCache) }) {
                                    Text("Очистить")
                                }
                            }
                        )

                        SettingsItem(
                            icon = Icons.Rounded.QueryStats,
                            title = "Статистика просмотра",
                            supportingText = "Часы, серии, дни подряд",
                            onClick = onNavigateToStats,
                            trailing = {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(Sizing.iconSm),
                                )
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.CloudSync,
                            title = "Создать резервную копию",
                            supportingText = "Экспорт истории, закладок и плейлистов в JSON",
                            onClick = {
                                val dateStr = java.time.LocalDate.now().toString()
                                exportLauncher.launch("anilibrix_backup_$dateStr.json")
                            }
                        )

                        SettingsItem(
                            icon = Icons.Rounded.Download,
                            title = "Восстановить из копии",
                            supportingText = "Импорт данных из JSON-файла резервной копии",
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "*/*"))
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.Sync,
                            title = "Синхронизация",
                            supportingText = state.syncStatus
                        )

                        // Единственная точка, из которой стираются локальные
                        // данные. Раньше это делал выход из аккаунта — молча и
                        // вопреки тексту собственного диалога.
                        SettingsItem(
                            icon = Icons.Rounded.DeleteForever,
                            title = "Удалить локальные данные",
                            supportingText = "Избранное, история, списки, оценки и плейлисты",
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = { viewModel.onIntent(ProfileIntent.ShowClearDataDialog) },
                        )
                    }
                }

                SettingsGroupHeader("О приложении")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(vertical = Spacing.sm)) {
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "О приложении",
                            supportingText = "Версия ${BuildConfig.VERSION_NAME}",
                            onClick = { showAboutDialog = true }
                        )

                        SettingsItem(
                            icon = Icons.Default.TrendingUp,
                            title = "Тренды",
                            onClick = onNavigateToTrending
                        )

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

                        SettingsItem(
                            icon = Icons.Default.BugReport,
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

            // Импорт предлагается, а не выполняется молча: у человека может
            // быть большой список на Shikimori, и перезапись локальных
            // пометок — его решение, а не наше.
            state.shikimoriImportPreview?.let { total ->
                AlertDialog(
                    onDismissRequest = { viewModel.onIntent(ProfileIntent.DismissShikimoriImport) },
                    title = { Text("Импортировать списки?") },
                    text = {
                        Text(
                            if (total > 0) {
                                "На Shikimori найдено записей: $total. " +
                                    "Локальные изменения, сделанные позже, сохранятся."
                            } else {
                                "На Shikimori пока нет записей для импорта."
                            }
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.onIntent(ProfileIntent.ConfirmShikimoriImport) },
                            enabled = total > 0,
                        ) { Text("Импортировать") }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.onIntent(ProfileIntent.DismissShikimoriImport) }) {
                            Text("Не сейчас")
                        }
                    }
                )
            }

            if (state.showClearDataDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.onIntent(ProfileIntent.DismissClearDataDialog) },
                    title = { Text("Удалить локальные данные?") },
                    text = {
                        Text(
                            "Будут стёрты избранное, история просмотра, списки, оценки и плейлисты. " +
                                "Отменить это действие нельзя."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.onIntent(ProfileIntent.ConfirmClearData) }
                        ) {
                            Text("Удалить", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.onIntent(ProfileIntent.DismissClearDataDialog) }
                        ) {
                            Text("Отмена")
                        }
                    }
                )
            }

            state.backupMessage?.let { message ->
                AlertDialog(
                    onDismissRequest = { viewModel.onIntent(ProfileIntent.DismissBackupMessage) },
                    title = { Text("Резервная копия") },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.onIntent(ProfileIntent.DismissBackupMessage) }) {
                            Text("Понятно")
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

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

/**
 * Раньше строка «О приложении» была мёртвой: `onClick = {}`.
 */
@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text("Anilibrix+") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = "Версия ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Неофициальный клиент Anilibria.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Шрифт Inter — SIL Open Font License 1.1.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
    )
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

/**
 * Тема оформления тремя вариантами вместо булева переключателя «Тёмная тема».
 *
 * Раньше это был Switch, который писал значение в DataStore, но его никто не
 * читал обратно — переключатель не работал вообще. Режим «Системная» стал
 * возможен только теперь, когда тема разрешается в AppThemeViewModel.
 */
@Composable
private fun ThemeModeSetting(
    mode: ThemeMode,
    onModeChange: (ThemeMode) -> Unit,
) {
    val options = listOf(
        ThemeMode.SYSTEM to "Системная",
        ThemeMode.LIGHT to "Светлая",
        ThemeMode.DARK to "Тёмная",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when (mode) {
                    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                    ThemeMode.LIGHT -> Icons.Default.LightMode
                    ThemeMode.DARK -> Icons.Default.DarkMode
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Sizing.iconMd)
            )
            Spacer(modifier = Modifier.width(Spacing.lg))
            Text(
                text = "Тема оформления",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = mode == value,
                    onClick = { onModeChange(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) {
                    Text(text = label, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    /**
     * Второстепенная строка под заголовком.
     *
     * Без неё статус приходилось вклеивать в сам заголовок
     * («Кэш: 128 МБ»), из-за чего название настройки менялось на глазах.
     */
    supportingText: String? = null,
    /** Цвет заголовка. Отличается только у деструктивных действий. */
    titleColor: Color? = null,
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
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = titleColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Sizing.iconMd)
        )
        Spacer(modifier = Modifier.width(Spacing.lg))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor ?: MaterialTheme.colorScheme.onSurface,
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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

/** «Синхронизировано 5 минут назад» — точное время здесь никому не нужно. */
private fun lastSyncLabel(timestamp: Long): String {
    if (timestamp <= 0L) return "Ещё не синхронизировалось"
    val minutes = (System.currentTimeMillis() - timestamp) / 60_000L
    return when {
        minutes < 1 -> "Синхронизировано только что"
        minutes < 60 -> "Синхронизировано $minutes мин назад"
        minutes < 60 * 24 -> "Синхронизировано ${minutes / 60} ч назад"
        else -> "Синхронизировано ${minutes / (60 * 24)} дн назад"
    }
}
