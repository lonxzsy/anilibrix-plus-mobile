package com.anilibrix.plus.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Catalog : Screen("catalog")
    data object Schedule : Screen("schedule")
    data object Library : Screen("library")
    data object Profile : Screen("profile")
    data object Trending : Screen("trending")
    data object Changelog : Screen("changelog")

    data class TitleDetail(val id: String) : Screen("title_detail/${id}") {
        companion object {
            const val ROUTE = "title_detail/{id}"
        }
    }

    data class Player(val titleId: String, val episodeId: Long) : Screen("player/${titleId}/${episodeId}") {
        companion object {
            const val ROUTE = "player/{titleId}/{episodeId}"
        }
    }

    data class StudioSearch(val query: String = "") : Screen("studio_search/${query}") {
        companion object {
            const val ROUTE = "studio_search/{query}"
        }
    }

    data class StudioEpisodes(val source: String, val id: String, val title: String) : Screen("studio_episodes/${source}/${id}/${title}") {
        companion object {
            const val ROUTE = "studio_episodes/{source}/{id}/{title}"
        }
    }

    data class StudioPlayer(val source: String, val episodeId: String) : Screen("studio_player/${source}/${episodeId}") {
        companion object {
            const val ROUTE = "studio_player/{source}/{episodeId}"
        }
    }
}
