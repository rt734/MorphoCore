package com.morphocore.feature.movements

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morphocore.content.api.ContentRepository
import com.morphocore.domain.Difficulty
import com.morphocore.feature.movements.MovementsSort.BY_DIFFICULTY
import com.morphocore.feature.movements.MovementsSort.BY_NAME
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MovementsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val disciplineId: String = checkNotNull(savedStateHandle["disciplineId"])

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    private val _selectedDifficulties = MutableStateFlow<Set<Difficulty>>(emptySet())
    private val _sort = MutableStateFlow(MovementsSort.BY_DIFFICULTY)

    val uiState: StateFlow<MovementsUiState> = combine(
        contentRepository.observeMovements(disciplineId),
        contentRepository.observeDisciplines(),
        _selectedTags,
        _selectedDifficulties,
        _sort
    ) { movements, disciplines, selectedTags, selectedDifficulties, sort ->
        val disciplineName = disciplines.find { it.id == disciplineId }?.name ?: disciplineId
        val availableTags = movements.flatMap { it.tags }.distinct().sorted()
        val filtered = movements
            .filter { m -> selectedTags.isEmpty() || m.tags.any { it in selectedTags } }
            .filter { m -> selectedDifficulties.isEmpty() || m.difficulty in selectedDifficulties }
        val sorted = when (sort) {
            BY_DIFFICULTY -> filtered.sortedWith(compareBy({ it.difficulty.ordinal }, { it.name }))
            BY_NAME -> filtered.sortedBy { it.name }
        }
        MovementsUiState.Ready(disciplineName, sorted, availableTags, selectedTags, selectedDifficulties, sort)
    }
        .catch { e -> emit(MovementsUiState.Error(e.message ?: "Failed to load movements")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MovementsUiState.Loading
        )

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
}
