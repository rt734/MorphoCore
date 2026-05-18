package com.morphocore.feature.movements

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morphocore.content.api.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MovementsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val disciplineId: String = checkNotNull(savedStateHandle["disciplineId"])

    val uiState: StateFlow<MovementsUiState> = combine(
        contentRepository.observeMovements(disciplineId),
        contentRepository.observeDisciplines()
    ) { movements, disciplines ->
        if (movements.isEmpty()) {
            MovementsUiState.Loading
        } else {
            val disciplineName = disciplines.find { it.id == disciplineId }?.name ?: disciplineId
            MovementsUiState.Ready(disciplineName = disciplineName, movements = movements)
        }
    }
        .catch { e -> emit(MovementsUiState.Error(e.message ?: "Failed to load movements")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MovementsUiState.Loading
        )
}
