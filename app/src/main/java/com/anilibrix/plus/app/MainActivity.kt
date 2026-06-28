package com.anilibrix.plus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anilibrix.plus.ui.auth.AuthModal
import com.anilibrix.plus.ui.navigation.BottomNavBar
import com.anilibrix.plus.ui.navigation.NavGraph
import com.anilibrix.plus.ui.navigation.Screen
import com.anilibrix.plus.ui.theme.AnilibrixTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnilibrixTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showAuthModal by remember { mutableStateOf(false) }

    val bottomNavRoutes = listOf(
        Screen.Home.route,
        Screen.Catalog.route,
        Screen.Schedule.route,
        Screen.Library.route,
        Screen.Profile.route
    )

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
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            onShowAuthModal = { showAuthModal = true }
        )
    }

    if (showAuthModal) {
        AuthModal(
            onDismiss = { showAuthModal = false },
            onLogin = { _, _ -> }
        )
    }
}
