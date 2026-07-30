package com.anilibrix.plus.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Catalog : Screen("catalog")
    data object Schedule : Screen("schedule")
    data object Library : Screen("library")
    data object Profile : Screen("profile")
    data object Trending : Screen("trending")
    data object Changelog : Screen("changelog")
    data object Issues : Screen("issues")
    data object Downloads : Screen("downloads")
    data object Stats : Screen("stats")

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

    data class CharacterDetail(val malId: Long) : Screen("character/${malId}") {
        companion object {
            const val ROUTE = "character/{malId}"
        }
    }

    data class StudioSearch(val query: String = "") : Screen(
        if (query.isBlank()) {
            "studio_search"
        } else {
            "studio_search?query=${query.urlEncode()}"
        }
    ) {
        companion object {
            const val ROUTE = "studio_search?query={query}"
        }
    }

    data class StudioEpisodes(val source: String, val id: String, val title: String) : Screen("studio_episodes/${source}/${id}/${title.urlEncode()}") {
        companion object {
            const val ROUTE = "studio_episodes/{source}/{id}/{title}"
        }
    }

    data class StudioPlayer(val source: String, val episodeId: String) : Screen("studio_player/${source}/${episodeId}") {
        companion object {
            const val ROUTE = "studio_player/{source}/{episodeId}"
        }
    }

    companion object {
        fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")
        fun String.urlDecode(): String = URLDecoder.decode(this, "UTF-8")
    }
}
