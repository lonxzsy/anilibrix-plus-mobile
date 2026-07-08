package com.anilibrix.plus.ui.schedule

import com.anilibrix.plus.domain.model.ScheduleDay

data class ScheduleUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val days: List<ScheduleDay> = emptyList(),
    val selectedDayIndex: Int = 0,
    val error: String? = null
)

sealed interface ScheduleIntent {
    data object Load : ScheduleIntent
    data object Refresh : ScheduleIntent
    data class SelectDay(val index: Int) : ScheduleIntent
}
