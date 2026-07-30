package com.anilibrix.plus.ui.schedule

import com.anilibrix.plus.domain.model.ScheduleDay
import com.anilibrix.plus.domain.model.ScheduleEntry

data class ScheduleUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val days: List<ScheduleDay> = emptyList(),
    val selectedDayIndex: Int = 0,
    /**
     * Показывать только то, что человек отслеживает.
     *
     * Расписание Anilibria — это весь сезон целиком; без фильтра найти в нём
     * свои три тайтла тяжелее, чем открыть библиотеку.
     */
    val onlyTracked: Boolean = false,
    val trackedIds: Set<Long> = emptySet(),
    val error: String? = null
) {
    /** Записи выбранного дня с учётом фильтра. */
    fun entriesFor(dayIndex: Int): List<ScheduleEntry> {
        val entries = days.getOrNull(dayIndex)?.entries.orEmpty()
        return if (onlyTracked) entries.filter { it.title.id in trackedIds } else entries
    }

    /** Сколько отслеживаемых релизов выходит в этот день — для подписи на табе. */
    fun trackedCount(dayIndex: Int): Int =
        days.getOrNull(dayIndex)?.entries.orEmpty().count { it.title.id in trackedIds }
}

sealed interface ScheduleIntent {
    data object Load : ScheduleIntent
    data object Refresh : ScheduleIntent
    data class SelectDay(val index: Int) : ScheduleIntent
    data object ToggleOnlyTracked : ScheduleIntent
}
