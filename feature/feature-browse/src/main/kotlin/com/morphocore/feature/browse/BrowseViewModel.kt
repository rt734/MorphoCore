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
    private val _selectedMuscle = MutableStateFlow<MuscleGroup?>(null)

    private val filterState = combine(
        _query.debounce(300),
        _selectedDifficulty,
        _selectedMuscle
    ) { query, difficulty, muscle -> BrowseFilterState(query, difficulty, muscle) }

    val uiState: StateFlow<BrowseUiState> = combine(
        contentRepository.observeDisciplines(),
        contentRepository.observeAllMovements(),
        contentRegistry.state,
        filterState
    ) { disciplines, movements, registryState, filter ->
        when {
            registryState is RegistryState.Error ->
                BrowseUiState.Error("Content failed to load. Tap retry to try again.")
            registryState is RegistryState.Loading ->
                BrowseUiState.Loading
            filter.query.isBlank() -> {
                val breakdown = movements.groupingBy { it.difficulty }.eachCount()
                val byDiscipline = movements.groupBy { it.disciplineId }
                val disciplineBreakdowns = byDiscipline.mapValues { (_, ms) ->
                    ms.groupingBy { it.difficulty }.eachCount()
                }
                val disciplineMuscleBreakdowns = byDiscipline.mapValues { (_, ms) ->
                    ms.flatMap { m -> m.muscles.map { it to m } }
                        .groupBy({ it.first }, { it.second })
                        .mapValues { (_, moves) -> moves.size }
                }
                val availableMuscles = movements.flatMap { it.muscles }.distinct()
                    .sortedBy { it.searchToken() }
                var filteredDisciplines = if (filter.difficulty == null) disciplines
                    else {
                        val idsWithDifficulty = byDiscipline
                            .filterValues { ms -> ms.any { it.difficulty == filter.difficulty } }
                            .keys
                        disciplines.filter { it.id in idsWithDifficulty }
                    }
                if (filter.muscle != null) {
                    val idsWithMuscle = byDiscipline
                        .filterValues { ms -> ms.any { filter.muscle in it.muscles } }
                        .keys
                    filteredDisciplines = filteredDisciplines.filter { it.id in idsWithMuscle }
                }
                val disciplineFilteredCounts: Map<String, Int> = when {
                    filter.difficulty != null && filter.muscle != null ->
                        byDiscipline.mapValues { (_, ms) ->
                            ms.count { m ->
                                m.difficulty == filter.difficulty && filter.muscle in m.muscles
                            }
                        }
                    filter.difficulty != null ->
                        disciplineBreakdowns.mapValues { (_, d) -> d[filter.difficulty] ?: 0 }
                    filter.muscle != null ->
                        disciplineMuscleBreakdowns.mapValues { (_, m) -> m[filter.muscle] ?: 0 }
                    else -> emptyMap()
                }
                BrowseUiState.Ready(
                    disciplines = filteredDisciplines,
                    totalMovementCount = movements.size,
                    difficultyBreakdown = breakdown,
                    selectedDifficulty = filter.difficulty,
                    disciplineBreakdowns = disciplineBreakdowns,
                    availableMuscles = availableMuscles,
                    selectedMuscle = filter.muscle,
                    disciplineMuscleBreakdowns = disciplineMuscleBreakdowns,
                    disciplineFilteredCounts = disciplineFilteredCounts
                )
            }
            else -> {
                val q = filter.query.trim().lowercase()
                val matchingDisciplines = disciplines.filter { d ->
                    d.name.lowercase().contains(q) || d.description.lowercase().contains(q)
                }
                val matchingMovements = movements.filter { m ->
                    m.name.lowercase().contains(q) ||
                    m.description.lowercase().contains(q) ||
                    m.tags.any { it.lowercase().contains(q) } ||
                    m.muscles.any { it.searchToken().contains(q) } ||
                    m.commonMistakes.any { it.lowercase().contains(q) }
                }.sortedBy { it.name }
                BrowseUiState.Ready(matchingDisciplines, matchingMovements, filter.query, movements.size)
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

    fun clearDifficultyFilter() {
        _selectedDifficulty.value = null
    }

    fun toggleMuscleFilter(muscle: MuscleGroup) {
        _selectedMuscle.update { if (it == muscle) null else muscle }
    }

    fun clearFilters() {
        _selectedDifficulty.value = null
        _selectedMuscle.value = null
    }

    fun retry() {
        viewModelScope.launch { contentRegistry.refresh() }
    }

    private data class BrowseFilterState(
        val query: String,
        val difficulty: Difficulty?,
        val muscle: MuscleGroup?
    )
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
