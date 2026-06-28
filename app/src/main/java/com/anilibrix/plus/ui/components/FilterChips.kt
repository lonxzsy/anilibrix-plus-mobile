package com.anilibrix.plus.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class FilterOption(
    val id: String,
    val label: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterChipsRow(
    options: List<FilterOption>,
    selectedIds: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    singleSelection: Boolean = false
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val selected = option.id in selectedIds
            FilterChip(
                selected = selected,
                onClick = {
                    val newSelection = if (singleSelection) {
                        if (selected) emptySet() else setOf(option.id)
                    } else {
                        if (selected) selectedIds - option.id
                        else selectedIds + option.id
                    }
                    onSelectionChanged(newSelection)
                },
                label = { Text(option.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun GenreFilterChips(
    genres: List<FilterOption>,
    selectedGenres: Set<String>,
    onGenresChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Genres",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            genres.forEach { genre ->
                val selected = genre.id in selectedGenres
                FilterChip(
                    selected = selected,
                    onClick = {
                        val newSelection = if (selected) selectedGenres - genre.id
                        else selectedGenres + genre.id
                        onGenresChanged(newSelection)
                    },
                    label = { Text(genre.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
fun YearFilterChip(
    selectedYear: Int?,
    onYearSelected: (Int?) -> Unit,
    years: List<Int> = (1990..2026).toList(),
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "Year",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FilterChip(
            selected = selectedYear != null,
            onClick = { expanded = true },
            label = { Text(selectedYear?.toString() ?: "All") }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.height(300.dp)
        ) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = {
                    onYearSelected(null)
                    expanded = false
                }
            )
            years.reversed().forEach { year ->
                DropdownMenuItem(
                    text = { Text(year.toString()) },
                    onClick = {
                        onYearSelected(year)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CatalogFilters(
    genres: List<FilterOption>,
    selectedGenres: Set<String>,
    onGenresChanged: (Set<String>) -> Unit,
    selectedYear: Int?,
    onYearSelected: (Int?) -> Unit,
    types: List<FilterOption>,
    selectedTypes: Set<String>,
    onTypesChanged: (Set<String>) -> Unit,
    seasons: List<FilterOption>,
    selectedSeason: String?,
    onSeasonSelected: (String?) -> Unit,
    statuses: List<FilterOption>,
    selectedStatuses: Set<String>,
    onStatusesChanged: (Set<String>) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = onClearAll) {
                Text("Clear all")
            }
        }
        GenreFilterChips(
            genres = genres,
            selectedGenres = selectedGenres,
            onGenresChanged = onGenresChanged
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            YearFilterChip(
                selectedYear = selectedYear,
                onYearSelected = onYearSelected,
                modifier = Modifier.weight(1f)
            )
            FilterChipsRow(
                options = types,
                selectedIds = selectedTypes,
                onSelectionChanged = onTypesChanged,
                modifier = Modifier.weight(1f),
                singleSelection = true
            )
        }
        FilterChipsRow(
            options = seasons,
            selectedIds = if (selectedSeason != null) setOf(selectedSeason) else emptySet(),
            onSelectionChanged = { ids -> onSeasonSelected(ids.firstOrNull()) },
            singleSelection = true
        )
        FilterChipsRow(
            options = statuses,
            selectedIds = selectedStatuses,
            onSelectionChanged = onStatusesChanged
        )
    }
}
