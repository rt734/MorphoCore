package com.morphocore.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morphocore.content.api.ContentRegistry
import com.morphocore.content.api.ContentRepository
import com.morphocore.content.api.RegistryState
import com.morphocore.domain.Difficulty
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
import kotlinx.coroutines.flow.update
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

    private val _selectedDifficulty = MutableStateFlow<Difficulty?>(null)

    val uiState: StateFlow<BrowseUiState> = combine(
        contentRepository.observeDisciplines(),
        contentRepository.observeAllMovements(),
        contentRegistry.state,
        _query.debounce(300),
        _selectedDifficulty
    ) { disciplines, movements, registryState, query, selectedDifficulty ->
        when {
            registryState is RegistryState.Error ->
                BrowseUiState.Error("Content failed to load. Tap retry to try again.")
            registryState is RegistryState.Loading ->
                BrowseUiState.Loading
            query.isBlank() -> {
                val breakdown = movements.groupingBy { it.difficulty }.eachCount()
                val byDiscipline = movements.groupBy { it.disciplineId }
                val disciplineBreakdowns = byDiscipline.mapValues { (_, ms) ->
                    ms.groupingBy { it.difficulty }.eachCount()
                }
                val filteredDisciplines = if (selectedDifficulty == null) {
                    disciplines
                } else {
                    val idsWithDifficulty = byDiscipline
                        .filterValues { ms -> ms.any { it.difficulty == selectedDifficulty } }
                        .keys
                    disciplines.filter { it.id in idsWithDifficulty }
                }
                BrowseUiState.Ready(
                    disciplines = filteredDisciplines,
                    totalMovementCount = movements.size,
                    difficultyBreakdown = breakdown,
                    selectedDifficulty = selectedDifficulty,
                    disciplineBreakdowns = disciplineBreakdowns
                )
            }
            else -> {
                val q = query.trim().lowercase()
                val matchingDisciplines = disciplines.filter { d ->
                    d.name.lowercase().contains(q) || d.description.lowercase().contains(q)
                }
                val matchingMovements = movements.filter { m ->
                    m.name.lowercase().contains(q) ||
                    m.description.lowercase().contains(q) ||
                    m.tags.any { it.lowercase().contains(q) } ||
                    m.muscles.any { it.searchToken().contains(q) }
                }.sortedBy { it.name }
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

    fun toggleDifficultyFilter(difficulty: Difficulty) {
        _selectedDifficulty.update { if (it == difficulty) null else difficulty }
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
