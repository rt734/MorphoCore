package com.morphocore.feature.movements

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morphocore.content.api.ContentRepository
import com.morphocore.domain.Difficulty
import com.morphocore.feature.movements.MovementsSort.BY_DIFFICULTY
import com.morphocore.feature.movements.MovementsSort.BY_NAME
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class MovementsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val disciplineId: String = checkNotNull(savedStateHandle["disciplineId"])

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    private val _selectedDifficulties = MutableStateFlow<Set<Difficulty>>(emptySet())
    private val _sort = MutableStateFlow(MovementsSort.BY_DIFFICULTY)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // Combine the four filter inputs into one intermediate to stay within the 5-flow limit.
    private val filterState = combine(
        _selectedTags,
        _selectedDifficulties,
        _sort,
        _query.debounce(300)
    ) { tags, difficulties, sort, query ->
        FilterState(tags, difficulties, sort, query)
    }

    val uiState: StateFlow<MovementsUiState> = combine(
        contentRepository.observeMovements(disciplineId),
        contentRepository.observeDisciplines(),
        filterState
    ) { movements, disciplines, filter ->
        val disciplineName = disciplines.find { it.id == disciplineId }?.name ?: disciplineId
        val availableTags = movements.flatMap { it.tags }.distinct().sorted()
        val tagCounts = movements.flatMap { it.tags }.groupingBy { it }.eachCount()
        val afterQuery = if (filter.query.isBlank()) movements
            else {
                val q = filter.query.trim().lowercase()
                movements.filter { m ->
                    m.name.lowercase().contains(q) || m.tags.any { it.lowercase().contains(q) }
                }
            }
        val filtered = afterQuery
            .filter { m -> filter.tags.isEmpty() || m.tags.any { it in filter.tags } }
            .filter { m -> filter.difficulties.isEmpty() || m.difficulty in filter.difficulties }
        val sorted = when (filter.sort) {
            BY_DIFFICULTY -> filtered.sortedWith(compareBy({ it.difficulty.ordinal }, { it.name }))
            BY_NAME       -> filtered.sortedBy { it.name }
        }
        val breakdown = movements.groupingBy { it.difficulty }.eachCount()
        MovementsUiState.Ready(
            disciplineName, sorted, movements.size, breakdown, availableTags, tagCounts,
            filter.tags, filter.difficulties, filter.sort, filter.query
        )
    }
        .catch { e -> emit(MovementsUiState.Error(e.message ?: "Failed to load movements")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MovementsUiState.Loading
        )

    fun setQuery(query: String) { _query.value = query }

    fun toggleTag(tag: String) {
        _selectedTags.update { tags -> if (tag in tags) tags - tag else tags + tag }
    }

    fun toggleDifficulty(difficulty: Difficulty) {
        _selectedDifficulties.update { d -> if (difficulty in d) d - difficulty else d + difficulty }
    }

    fun toggleSort() {
        _sort.update { if (it == BY_DIFFICULTY) BY_NAME else BY_DIFFICULTY }
    }

    fun clearFilters() {
        _selectedTags.value = emptySet()
        _selectedDifficulties.value = emptySet()
    }

    private data class FilterState(
        val tags: Set<String>,
        val difficulties: Set<Difficulty>,
        val sort: MovementsSort,
        val query: String
    )
}
