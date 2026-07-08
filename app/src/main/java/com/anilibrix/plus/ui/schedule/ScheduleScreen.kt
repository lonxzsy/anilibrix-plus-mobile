package com.anilibrix.plus.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anilibrix.plus.domain.model.ScheduleDay
import com.anilibrix.plus.ui.components.ErrorView
import com.anilibrix.plus.ui.components.LoadingIndicator
import com.anilibrix.plus.ui.components.TitleCardGrid
import com.anilibrix.plus.ui.components.TitleCardShimmer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
    onTitleClick: (Long) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        when {
            state.loading -> {
                LoadingIndicator()
            }
            state.error != null && state.days.isEmpty() -> {
                ErrorView(
                    message = state.error ?: "Ошибка загрузки",
                    onRetry = { viewModel.onIntent(ScheduleIntent.Load) }
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = { viewModel.onIntent(ScheduleIntent.Refresh) }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val days = state.days
                        val selectedIndex = state.selectedDayIndex

                        ScrollableTabRow(
                            selectedTabIndex = selectedIndex.coerceIn(0, maxOf(0, days.size - 1)),
                            modifier = Modifier.fillMaxWidth(),
                            edgePadding = 16.dp,
                            divider = { HorizontalDivider() }
                        ) {
                            days.forEachIndexed { index, day ->
                                Tab(
                                    selected = index == selectedIndex,
                                    onClick = {
                                        viewModel.onIntent(ScheduleIntent.SelectDay(index))
                                    },
                                    text = {
                                        Text(
                                            text = day.day,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                )
                            }
                        }

                        if (days.isNotEmpty() && selectedIndex in days.indices) {
                            val selectedDay = days[selectedIndex]

                            if (selectedDay.releases.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Нет релизов в этот день",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(vertical = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = selectedDay.releases,
                                        key = { it.id }
                                    ) { title ->
                                        TitleCardGrid(
                                            title = title,
                                            onClick = { onTitleClick(title.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
