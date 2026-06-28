package com.anilibrix.plus.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val anilibriaRepository: AnilibriaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init {
        loadSchedule()
    }

    fun onIntent(intent: ScheduleIntent) {
        when (intent) {
            ScheduleIntent.Load -> loadSchedule()
            ScheduleIntent.Refresh -> refresh()
            is ScheduleIntent.SelectDay -> _state.update { it.copy(selectedDayIndex = intent.index) }
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
                                    selectedDayIndex = minOf(it.selectedDayIndex, maxOf(0, days.size - 1))
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
                                    selectedDayIndex = minOf(it.selectedDayIndex, maxOf(0, days.size - 1))
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
