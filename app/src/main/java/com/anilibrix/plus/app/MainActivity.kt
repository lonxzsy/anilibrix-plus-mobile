package com.anilibrix.plus.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anilibrix.plus.core.network.AuthEvent
import com.anilibrix.plus.core.network.AuthEventBus
import com.anilibrix.plus.core.notifications.NotificationHelper
import com.anilibrix.plus.core.util.NetworkMonitor
import com.anilibrix.plus.data.sync.ShikimoriAuthManager
import com.anilibrix.plus.ui.components.ConnectivityPill
import com.anilibrix.plus.ui.components.LocalToastHostState
import com.anilibrix.plus.ui.components.ToastHost
import com.anilibrix.plus.ui.components.ToastType
import com.anilibrix.plus.ui.components.UpdateSnackbarEffect
import com.anilibrix.plus.ui.components.rememberToastHostState
import com.anilibrix.plus.ui.navigation.BottomNavBar
import com.anilibrix.plus.ui.navigation.LocalBottomBarHeight
import com.anilibrix.plus.ui.navigation.NavGraph
import com.anilibrix.plus.ui.navigation.Screen
import com.anilibrix.plus.ui.player.LocalIsInPictureInPicture
import com.anilibrix.plus.ui.theme.AnilibrixTheme
import com.anilibrix.plus.ui.theme.Spacing
import com.anilibrix.plus.ui.theme.resolveDark
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var authEventBus: AuthEventBus

    @Inject
    lateinit var shikimoriAuth: ShikimoriAuthManager

    private val themeViewModel: AppThemeViewModel by viewModels()

    private var notificationTitleId by mutableStateOf<Long?>(null)
    private var notificationEpisodeId by mutableStateOf<Long?>(null)

    /**
     * Приложение в режиме «картинка в картинке».
     *
     * Держится здесь, а не в ViewModel плеера: выйти из PiP можно жестом
     * системы, о котором знает только Activity. Без этого состояние
     * расходилось бы, и контролы оставались бы спрятанными после возврата.
     */
    private var isInPip by mutableStateOf(false)

    /** Код авторизации Shikimori, пришедший редиректом из браузера. */
    private var shikimoriCode by mutableStateOf<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Держим splash, пока не прочитаны настройки темы: иначе первый кадр
        // отрисуется дефолтной темой и мигнёт при подстановке настоящей.
        splash.setKeepOnScreenCondition { themeViewModel.themeState.value == null }

        notificationTitleId = intent.extractNotificationTitleId()
        notificationEpisodeId = intent.extractNotificationEpisodeId()
        shikimoriCode = shikimoriAuth.extractCode(intent.data)
        requestNotificationPermissionIfNeeded()

        setContent {
            val themeState by themeViewModel.themeState.collectAsStateWithLifecycle()
            val state = themeState ?: return@setContent

            val dark = state.mode.resolveDark(isSystemInDarkTheme())

            // Иконки системных баров должны следовать за темой ПРИЛОЖЕНИЯ,
            // а не за системной: у пользователя есть свой переключатель.
            // Раньше вызывался безаргументный enableEdgeToEdge(), который
            // выводил полярность из системной темы.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        AndroidColor.TRANSPARENT,
                        AndroidColor.TRANSPARENT,
                    ) { dark },
                    navigationBarStyle = SystemBarStyle.auto(
                        LIGHT_NAV_SCRIM,
                        DARK_NAV_SCRIM,
                    ) { dark },
                )
                onDispose {}
            }

            AnilibrixTheme(darkTheme = dark, dynamicColor = state.dynamicColor) {
                CompositionLocalProvider(LocalIsInPictureInPicture provides isInPip) {
                    MainScreen(
                        networkMonitor = networkMonitor,
                        authEventBus = authEventBus,
                        notificationTitleId = notificationTitleId,
                        notificationEpisodeId = notificationEpisodeId,
                        shikimoriCode = shikimoriCode,
                        onShikimoriCodeHandled = { shikimoriCode = null },
                        onNotificationTitleHandled = {
                            notificationTitleId = null
                            notificationEpisodeId = null
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationTitleId = intent.extractNotificationTitleId()
        notificationEpisodeId = intent.extractNotificationEpisodeId()
        // Возврат из браузера после входа в Shikimori. Activity объявлена
        // singleTask, поэтому редирект приходит сюда, а не создаёт копию
        // экрана поверх текущего.
        shikimoriAuth.extractCode(intent.data)?.let { shikimoriCode = it }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPip = isInPictureInPictureMode
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun Intent.extractNotificationTitleId(): Long? {
        val value = getLongExtra(NotificationHelper.EXTRA_TITLE_ID, -1L)
        return value.takeIf { it > 0L }
    }

    private fun Intent.extractNotificationEpisodeId(): Long? {
        val value = getLongExtra(NotificationHelper.EXTRA_EPISODE_ID, -1L)
        return value.takeIf { it > 0L }
    }

    private companion object {
        // Те же скримы, что AndroidX ставит для трёхкнопочной навигации.
        val LIGHT_NAV_SCRIM = AndroidColor.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DARK_NAV_SCRIM = AndroidColor.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}

@Composable
fun MainScreen(
    networkMonitor: NetworkMonitor,
    authEventBus: AuthEventBus,
    notificationTitleId: Long? = null,
    notificationEpisodeId: Long? = null,
    shikimoriCode: String? = null,
    onShikimoriCodeHandled: () -> Unit = {},
    onNotificationTitleHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavRoutes = listOf(
        Screen.Home.route,
        Screen.Catalog.route,
        Screen.Schedule.route,
        Screen.Library.route,
        Screen.Profile.route
    )
    val showBottomBar = currentRoute in bottomNavRoutes

    val toastHostState = rememberToastHostState()
    var pendingShikimoriCode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(notificationTitleId, notificationEpisodeId) {
        val titleId = notificationTitleId ?: return@LaunchedEffect
        // Уведомление о новой серии ведёт прямо в плеер этой серии. Раньше оно
        // открывало главный экран, и до самой серии оставалось четыре тапа.
        val route = notificationEpisodeId
            ?.let { Screen.Player(titleId.toString(), it).route }
            ?: Screen.TitleDetail(titleId.toString()).route
        navController.navigate(route)
        onNotificationTitleHandled()
    }

    // Без сети открываем «Загрузки»: на главной в этот момент показывать
    // нечего, а скачанное лежит рядом и готово к просмотру.
    val offlineStart: OfflineStartViewModel = hiltViewModel()
    val openDownloads by offlineStart.shouldOpenDownloads.collectAsStateWithLifecycle()
    LaunchedEffect(openDownloads) {
        if (!openDownloads) return@LaunchedEffect
        navController.navigate(Screen.Downloads.route)
        offlineStart.consume()
    }

    // Проверка обновлений. Раньше UpdateSnackbarEffect был написан, но не
    // композился нигде — приложение никогда не сообщало о новых версиях.
    UpdateSnackbarEffect(toastHostState = toastHostState)

    // Код авторизации Shikimori обрабатывает профиль: там же лежит и весь
    // остальной UI привязки. Переходим на вкладку, чтобы человек увидел
    // результат, а не гадал, сработало ли.
    LaunchedEffect(shikimoriCode) {
        val code = shikimoriCode ?: return@LaunchedEffect
        navController.navigate(Screen.Profile.route) {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        pendingShikimoriCode = code
        onShikimoriCodeHandled()
    }

    // Протухшая сессия раньше проявлялась как бесконечная «Ошибка загрузки» на
    // каждом экране: 401 нигде не обрабатывался, токен оставался в хранилище, и
    // догадаться, что надо выйти и войти заново, человек мог только сам.
    LaunchedEffect(authEventBus) {
        authEventBus.events.collect { event ->
            when (event) {
                AuthEvent.SessionExpired -> {
                    authEventBus.consume()
                    val result = toastHostState.showAction(
                        message = "Сессия истекла — войдите заново",
                        actionLabel = "Войти",
                        type = ToastType.Warning,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        }
    }

    CompositionLocalProvider(LocalToastHostState provides toastHostState) {
        Scaffold(
            // Корень не претендует на системные бары — их обслуживает каждый
            // экран сам. Это снимает двойной паддинг во всех вложенных Scaffold
            // разом, не трогая их файлы.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                // Бар ВСЕГДА в композиции, скрывается своим AnimatedVisibility.
                // Раньше он выкидывался из композиции через `if`, поэтому
                // анимация ухода не проигрывалась никогда.
                BottomNavBar(
                    currentRoute = currentRoute,
                    visible = showBottomBar,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            },
            // Корневой Scaffold объявляет contentWindowInsets = 0, поэтому
            // снекбар-хост НЕ получает нижний системный инсет сам — без этого
            // тост уезжал под жестовую полосу (заметно на экране тайтла,
            // где нижнего бара нет).
            snackbarHost = {
                ToastHost(
                    toastHostState = toastHostState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = Spacing.sm),
                )
            }
        ) { innerPadding ->
            CompositionLocalProvider(
                LocalBottomBarHeight provides innerPadding.calculateBottomPadding(),
            ) {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .fillMaxSize()
                ) {
                    NavGraph(
                        navController = navController,
                        shikimoriCode = pendingShikimoriCode,
                        onShikimoriCodeConsumed = { pendingShikimoriCode = null },
                        modifier = Modifier.fillMaxSize()
                    )
                    ConnectivityPill(
                        networkMonitor = networkMonitor,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = Spacing.sm),
                    )
                }
            }
        }
    }
}
