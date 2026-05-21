package com.morphocore.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morphocore.content.api.ContentRegistry
import com.morphocore.content.api.ContentRepository
import com.morphocore.content.api.RegistryState
import com.morphocore.domain.MuscleGroup
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val contentRegistry: ContentRegistry
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val uiState: StateFlow<BrowseUiState> = combine(
        contentRepository.observeDisciplines(),
        contentRepository.observeAllMovements(),
        contentRegistry.state,
        _query.debounce(300)
    ) { disciplines, movements, registryState, query ->
        when {
            registryState is RegistryState.Error ->
                BrowseUiState.Error("Content failed to load. Tap retry to try again.")
            registryState is RegistryState.Loading ->
                BrowseUiState.Loading
            query.isBlank() -> {
                val breakdown = movements.groupingBy { it.difficulty }.eachCount()
                BrowseUiState.Ready(
                    disciplines = disciplines,
                    totalMovementCount = movements.size,
                    difficultyBreakdown = breakdown
                )
            }
            else -> {
                val q = query.trim().lowercase()
                val matchingDisciplines = disciplines.filter { it.name.lowercase().contains(q) }
                val matchingMovements = movements.filter { m ->
                    m.name.lowercase().contains(q) ||
                    m.tags.any { it.lowercase().contains(q) } ||
                    m.muscles.any { it.searchToken().contains(q) }
                }
                BrowseUiState.Ready(matchingDisciplines, matchingMovements, query, movements.size)
            }
        }
    }
        .catch { e -> emit(BrowseUiState.Error(e.message ?: "Failed to load disciplines")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BrowseUiState.Loading
        )

    fun setQuery(query: String) {
        _query.value = query
    }

    fun retry() {
        viewModelScope.launch { contentRegistry.refresh() }
    }
}

private fun MuscleGroup.searchToken(): String = when (this) {
    MuscleGroup.Quadriceps -> "quadriceps"
    MuscleGroup.Hamstrings -> "hamstrings"
    MuscleGroup.Glutes     -> "glutes"
    MuscleGroup.Core       -> "core"
    MuscleGroup.Shoulders  -> "shoulders"
    MuscleGroup.Back       -> "back"
    MuscleGroup.Chest      -> "chest"
    MuscleGroup.Calves     -> "calves"
    MuscleGroup.HipFlexors -> "hip flexors"
    is MuscleGroup.Unknown -> this.raw.replace('_', ' ')
}
