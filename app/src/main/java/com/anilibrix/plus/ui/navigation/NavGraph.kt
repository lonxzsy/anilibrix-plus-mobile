package com.anilibrix.plus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.anilibrix.plus.ui.catalog.CatalogScreen
import com.anilibrix.plus.ui.changelog.ChangelogScreen
import com.anilibrix.plus.ui.detail.TitleDetailScreen
import com.anilibrix.plus.ui.home.HomeScreen
import com.anilibrix.plus.ui.library.LibraryScreen
import com.anilibrix.plus.ui.player.PlayerScreen
import com.anilibrix.plus.ui.profile.ProfileScreen
import com.anilibrix.plus.ui.schedule.ScheduleScreen
import com.anilibrix.plus.ui.studio.StudioEpisodesScreen
import com.anilibrix.plus.ui.studio.StudioPlayerScreen
import com.anilibrix.plus.ui.studio.StudioSearchScreen
import com.anilibrix.plus.ui.trending.TrendingScreen

@Suppress("DEPRECATION")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    onShowAuthModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onTitleClick = { id -> navController.navigate("title_detail/$id") }
            )
        }
        composable(Screen.Catalog.route) {
            CatalogScreen(
                onTitleClick = { id -> navController.navigate("title_detail/$id") },
                onNavigateToStudioSearch = { query ->
                    navController.navigate("studio_search/$query")
                }
            )
        }
        composable(Screen.Schedule.route) {
            ScheduleScreen(
                onTitleClick = { id -> navController.navigate("title_detail/$id") }
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                onPlayEpisode = { titleId, episodeId ->
                    navController.navigate("player/$titleId/$episodeId")
                },
                onTitleClick = { id -> navController.navigate("title_detail/$id") }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToTrending = { navController.navigate(Screen.Trending.route) },
                onNavigateToChangelog = { navController.navigate(Screen.Changelog.route) }
            )
        }
        composable(Screen.Trending.route) {
            TrendingScreen()
        }
        composable(Screen.Changelog.route) {
            ChangelogScreen()
        }
        composable(
            route = Screen.TitleDetail.ROUTE,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            TitleDetailScreen(
                id = id,
                onPlayEpisode = { titleId, episodeId ->
                    navController.navigate("player/$titleId/$episodeId")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Player.ROUTE,
            arguments = listOf(
                navArgument("titleId") { type = NavType.StringType },
                navArgument("episodeId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val titleId = backStackEntry.arguments?.getString("titleId") ?: return@composable
            val episodeId = backStackEntry.arguments?.getLong("episodeId") ?: return@composable
            PlayerScreen(
                titleId = titleId,
                episodeId = episodeId,
                onBack = { navController.popBackStack() },
                onNextEpisode = { nextId ->
                    navController.navigate("player/$titleId/$nextId") {
                        popUpTo("player/$titleId/$episodeId") { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.StudioSearch.ROUTE,
            arguments = listOf(navArgument("query") { type = NavType.StringType })
        ) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: ""
            StudioSearchScreen(
                initialQuery = query,
                onNavigateToEpisodes = { source, id, title ->
                    navController.navigate("studio_episodes/$source/$id/$title")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.StudioEpisodes.ROUTE,
            arguments = listOf(
                navArgument("source") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: return@composable
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            val title = backStackEntry.arguments?.getString("title") ?: return@composable
            StudioEpisodesScreen(
                source = source,
                animeId = id,
                title = title,
                onBack = { navController.popBackStack() },
                onPlayEpisode = { s, epId ->
                    navController.navigate("studio_player/$s/$epId")
                }
            )
        }
        composable(
            route = Screen.StudioPlayer.ROUTE,
            arguments = listOf(
                navArgument("source") { type = NavType.StringType },
                navArgument("episodeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: return@composable
            val episodeId = backStackEntry.arguments?.getString("episodeId") ?: return@composable
            StudioPlayerScreen(
                source = source,
                episodeId = episodeId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
