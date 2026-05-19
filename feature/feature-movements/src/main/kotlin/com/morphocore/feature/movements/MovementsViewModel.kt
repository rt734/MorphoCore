package com.morphocore.feature.movements

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morphocore.content.api.ContentRepository
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

    val uiState: StateFlow<MovementsUiState> = combine(
        contentRepository.observeMovements(disciplineId),
        contentRepository.observeDisciplines(),
        _selectedTags
    ) { movements, disciplines, selectedTags ->
        val disciplineName = disciplines.find { it.id == disciplineId }?.name ?: disciplineId
        val availableTags = movements.flatMap { it.tags }.distinct().sorted()
        val filtered = if (selectedTags.isEmpty()) movements
            else movements.filter { m -> m.tags.any { it in selectedTags } }
        MovementsUiState.Ready(disciplineName, filtered, availableTags, selectedTags)
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
}
