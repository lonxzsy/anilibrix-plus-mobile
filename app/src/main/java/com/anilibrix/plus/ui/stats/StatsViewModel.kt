package com.anilibrix.plus.ui.stats

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.EpisodeProgress
import com.anilibrix.plus.domain.model.HistoryEntry
import com.anilibrix.plus.domain.repository.LocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Immutable
data class StatsUiState(
    val loading: Boolean = true,
    val totalMs: Long = 0L,
    val weekMs: Long = 0L,
    val monthMs: Long = 0L,
    val episodesWatched: Int = 0,
    val titlesStarted: Int = 0,
    val titlesCompleted: Int = 0,
    /** Дней подряд с хотя бы одной просмотренной серией. */
    val streakDays: Int = 0,
    val byStatus: Map<CollectionType, Int> = emptyMap(),
    val recent: List<HistoryEntry> = emptyList(),
)

/**
 * Статистика просмотра.
 *
 * Все данные для неё лежали в таблице `history` с самого начала — не хватало
 * только запроса и экрана. Показывается локально: ничего никуда не
 * отправляется.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val localRepository: LocalRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }

            val history = localRepository.getHistory().first()
            val now = System.currentTimeMillis()
            val weekAgo = now - TimeUnit.DAYS.toMillis(7)
            val monthAgo = now - TimeUnit.DAYS.toMillis(30)

            val counts = localRepository.getCollectionCounts().first()

            _state.update {
                StatsUiState(
                    loading = false,
                    totalMs = localRepository.getTotalWatchTimeMs(),
                    weekMs = history.filter { e -> e.watchedAt >= weekAgo }.sumOf { e -> e.effectiveMs() },
                    monthMs = history.filter { e -> e.watchedAt >= monthAgo }.sumOf { e -> e.effectiveMs() },
                    episodesWatched = history.count { e -> e.isWatched() },
                    titlesStarted = history.distinctBy { e -> e.titleId }.size,
                    titlesCompleted = counts[CollectionType.COMPLETED] ?: 0,
                    streakDays = history.streakDays(now),
                    byStatus = counts,
                    recent = history.sortedByDescending { e -> e.watchedAt }.take(10),
                )
            }
        }
    }

    private fun HistoryEntry.isWatched(): Boolean =
        duration > 0 && timestamp.toFloat() / duration >= EpisodeProgress.WATCHED_THRESHOLD

    private fun HistoryEntry.effectiveMs(): Long = if (isWatched()) duration else timestamp

    /**
     * Серия дней подряд.
     *
     * Считается назад от сегодня; вчерашний день тоже засчитывается за начало
     * серии — иначе она обнулялась бы каждое утро до первого просмотра, и
     * число выглядело бы сломанным.
     */
    private fun List<HistoryEntry>.streakDays(now: Long): Int {
        if (isEmpty()) return 0
        val days = map { it.watchedAt.toDayIndex() }.toSet()
        val today = now.toDayIndex()

        var streak = 0
        var cursor = if (days.contains(today)) today else today - 1
        while (days.contains(cursor)) {
            streak++
            cursor--
        }
        return streak
    }

    private fun Long.toDayIndex(): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = this@toDayIndex }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis / TimeUnit.DAYS.toMillis(1)
    }
}
