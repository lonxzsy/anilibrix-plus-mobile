package com.anilibrix.plus.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import com.anilibrix.plus.domain.repository.LocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Индекс сегодняшнего дня в списке, если он там есть.
 *
 * Расписание логичнее открывать на текущем дне, а не на первом попавшемся.
 */
private fun List<com.anilibrix.plus.domain.model.ScheduleDay>.indexOfToday(): Int? {
    val today = LocalDate.now().dayOfWeek.value
    return indexOfFirst { it.dayOfWeek == today }.takeIf { it >= 0 }
}

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val anilibriaRepository: AnilibriaRepository,
    private val localRepository: LocalRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init {
        loadSchedule()
        observeTracked()
    }

    /**
     * Что человек отслеживает — избранное плюс активные списки.
     *
     * Подпиской, а не разовым чтением: статус можно поставить, не уходя с
     * расписания (через экран тайтла), и фильтр должен это сразу учесть.
     */
    private fun observeTracked() {
        viewModelScope.launch {
            combine(
                localRepository.getFavorites(),
                localRepository.getCollections(CollectionType.WATCHING),
                localRepository.getCollections(CollectionType.WATCH_LATER),
                localRepository.getCollections(CollectionType.ON_HOLD),
            ) { favorites, watching, planned, onHold ->
                (favorites + watching + planned + onHold).map { it.titleId }.toSet()
            }.collect { ids ->
                _state.update { it.copy(trackedIds = ids) }
            }
        }
    }

    fun onIntent(intent: ScheduleIntent) {
        when (intent) {
            ScheduleIntent.Load -> loadSchedule()
            ScheduleIntent.Refresh -> refresh()
            is ScheduleIntent.SelectDay -> _state.update { it.copy(selectedDayIndex = intent.index) }
            ScheduleIntent.ToggleOnlyTracked -> _state.update { it.copy(onlyTracked = !it.onlyTracked) }
        }
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            anilibriaRepository.getSchedule()
                .catch { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "Ошибка загрузки расписания") }
                }
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            val days = result.data
                            _state.update {
                                it.copy(
                                    days = days,
                                    loading = false,
                                    selectedDayIndex = days.indexOfToday()
                                        ?: minOf(it.selectedDayIndex, maxOf(0, days.size - 1))
                                )
                            }
                        }
                        is NetworkResult.Error -> {
                            _state.update { it.copy(loading = false, error = result.message) }
                        }
                        is NetworkResult.Loading -> {}
                    }
                }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }

            anilibriaRepository.getSchedule()
                .catch { e ->
                    _state.update { it.copy(refreshing = false, error = e.message) }
                }
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            val days = result.data
                            _state.update {
                                it.copy(
                                    days = days,
                                    refreshing = false,
                                    selectedDayIndex = days.indexOfToday()
                                        ?: minOf(it.selectedDayIndex, maxOf(0, days.size - 1))
                                )
                            }
                        }
                        is NetworkResult.Error -> {
                            _state.update { it.copy(refreshing = false, error = result.message) }
                        }
                        is NetworkResult.Loading -> {}
                    }
                }
        }
    }
}
