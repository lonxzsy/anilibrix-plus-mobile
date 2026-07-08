package com.anilibrix.plus.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.anilibrix.plus.ui.catalog.CatalogScreen
import com.anilibrix.plus.ui.character.CharacterDetailScreen
import com.anilibrix.plus.ui.changelog.ChangelogScreen
import com.anilibrix.plus.ui.detail.TitleDetailScreen
import com.anilibrix.plus.ui.home.HomeScreen
import com.anilibrix.plus.ui.issues.IssuesScreen
import com.anilibrix.plus.ui.library.LibraryScreen
import com.anilibrix.plus.ui.player.PlayerScreen
import com.anilibrix.plus.ui.profile.ProfileScreen
import com.anilibrix.plus.ui.schedule.ScheduleScreen
import com.anilibrix.plus.ui.studio.StudioEpisodesScreen
import com.anilibrix.plus.ui.studio.StudioPlayerScreen
import com.anilibrix.plus.ui.studio.StudioSearchScreen
import com.anilibrix.plus.ui.trending.TrendingScreen
import com.anilibrix.plus.ui.navigation.Screen.Companion.urlDecode

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
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
                    navController.navigate(Screen.StudioSearch(query).route)
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
                onNavigateToChangelog = { navController.navigate(Screen.Changelog.route) },
                onNavigateToIssues = { navController.navigate(Screen.Issues.route) }
            )
        }
        composable(Screen.Trending.route) {
            TrendingScreen()
        }
        composable(Screen.Changelog.route) {
            ChangelogScreen()
        }
        composable(Screen.Issues.route) {
            IssuesScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.TitleDetail.ROUTE,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            TitleDetailScreen(
                id = id,
                onPlayEpisode = { titleId, episodeId ->
                    navController.navigate("player/$titleId/$episodeId")
                },
                onCharacterClick = { malId ->
                    navController.navigate(Screen.CharacterDetail(malId).route)
                },
                onTitleClick = { titleId ->
                    navController.navigate(Screen.TitleDetail(titleId.toString()).route)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Player.ROUTE,
            arguments = listOf(
                navArgument("titleId") { type = NavType.StringType },
                navArgument("episodeId") { type = NavType.LongType }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(400)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(400)
                )
            }
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
            route = Screen.CharacterDetail.ROUTE,
            arguments = listOf(navArgument("malId") { type = NavType.LongType }),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) { backStackEntry ->
            val malId = backStackEntry.arguments?.getLong("malId") ?: return@composable
            CharacterDetailScreen(
                malId = malId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.StudioSearch.ROUTE,
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query")?.urlDecode() ?: ""
            StudioSearchScreen(
                initialQuery = query,
                onNavigateToEpisodes = { source, id, title ->
                    navController.navigate(Screen.StudioEpisodes(source, id, title).route)
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
            val title = backStackEntry.arguments?.getString("title")?.urlDecode() ?: return@composable
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
