package com.anilibrix.plus.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anilibrix.plus.core.notifications.NotificationHelper
import com.anilibrix.plus.core.util.NetworkMonitor
import com.anilibrix.plus.ui.components.LocalToastHostState
import com.anilibrix.plus.ui.components.OfflineBanner
import com.anilibrix.plus.ui.components.ToastHost
import com.anilibrix.plus.ui.components.rememberToastHostState
import com.anilibrix.plus.ui.navigation.BottomNavBar
import com.anilibrix.plus.ui.navigation.NavGraph
import com.anilibrix.plus.ui.navigation.Screen
import com.anilibrix.plus.ui.theme.AnilibrixTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    private var notificationTitleId by mutableStateOf<Long?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        notificationTitleId = intent.extractNotificationTitleId()
        requestNotificationPermissionIfNeeded()
        setContent {
            AnilibrixTheme {
                MainScreen(
                    networkMonitor = networkMonitor,
                    notificationTitleId = notificationTitleId,
                    onNotificationTitleHandled = { notificationTitleId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationTitleId = intent.extractNotificationTitleId()
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
}

@Composable
fun MainScreen(
    networkMonitor: NetworkMonitor,
    notificationTitleId: Long? = null,
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

    val toastHostState = rememberToastHostState()

    LaunchedEffect(notificationTitleId) {
        notificationTitleId?.let { titleId ->
            navController.navigate(Screen.TitleDetail(titleId.toString()).route)
            onNotificationTitleHandled()
        }
    }

    CompositionLocalProvider(LocalToastHostState provides toastHostState) {
        Scaffold(
            bottomBar = {
                if (currentRoute in bottomNavRoutes) {
                    BottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            },
            snackbarHost = { ToastHost(toastHostState = toastHostState) }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                OfflineBanner(networkMonitor = networkMonitor)
                NavGraph(
                    navController = navController,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
