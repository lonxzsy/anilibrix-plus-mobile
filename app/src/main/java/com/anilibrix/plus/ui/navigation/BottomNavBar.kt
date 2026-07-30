package com.anilibrix.plus.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.anilibrix.plus.ui.theme.MotionTokens
import com.anilibrix.plus.ui.theme.Sizing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import com.anilibrix.plus.ui.components.rememberHaptics
import com.anilibrix.plus.ui.theme.AnilibrixPolygons
import com.anilibrix.plus.ui.theme.MorphPolygonShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Главная", Screen.Home.route, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("Каталог", Screen.Catalog.route, Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem("Расписание", Screen.Schedule.route, Icons.Filled.Schedule, Icons.Outlined.Schedule),
    BottomNavItem("Библиотека", Screen.Library.route, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    BottomNavItem("Профиль", Screen.Profile.route, Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    visible: Boolean = true
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(MotionTokens.spatialDefault()) { it } +
            fadeIn(MotionTokens.effectsDefault()),
        exit = slideOutVertically(MotionTokens.spatialFast()) { it } +
            fadeOut(MotionTokens.effectsFast())
    ) {
        NavigationBar(
            // Дефолт MD3 — surfaceContainer. С `surface` бар сливался с фоном.
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            val haptics = rememberHaptics()

            bottomNavItems.forEach { item ->
                val selected = currentRoute == item.route
                // Индикатор выбранной вкладки перетекает из круга в «печеньку».
                // Форма считается через graphics-shapes; сами полигоны кэшируются
                // в AnilibrixPolygons — строить их покадрово нельзя.
                val morphProgress by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = MotionTokens.spatialDefault(),
                    label = "navIndicatorMorph",
                )

                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            haptics.tick()
                            onNavigate(item.route)
                        }
                    },
                    icon = {
                        // Бокс индикатора занимает место ВСЕГДА, даже когда
                        // вкладка не выбрана. Раньше он появлялся только у
                        // выбранной, слот иконки скакал с 24 на 32dp — и подпись
                        // из-за этого резко уезжала вниз при переключении.
                        Box(
                            modifier = Modifier.size(
                                width = INDICATOR_WIDTH,
                                height = INDICATOR_HEIGHT,
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = morphProgress }
                                    .clip(
                                        MorphPolygonShape(
                                            morph = AnilibrixPolygons.selectionMorph,
                                            progress = morphProgress,
                                        )
                                    )
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            )
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                modifier = Modifier.size(Sizing.iconMd)
                            )
                        }
                    },
                    // Без этого «Расписание» и «Библиотека» переносились на две
                    // строки и раздували высоту бара — видно на скриншотах.
                    label = {
                        Text(
                            text = item.label,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        // Свой morph-индикатор рисуется в слоте иконки.
                        indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            }
        }
    }
}

// Размер индикатора выбранной вкладки. Ширина как у пилюли MD3 — узкий
// кружок вокруг иконки читался как случайная точка, а не как выделение.
private val INDICATOR_WIDTH = 64.dp
private val INDICATOR_HEIGHT = 32.dp
