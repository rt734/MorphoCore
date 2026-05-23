package com.morphocore.feature.browse

import com.morphocore.domain.Difficulty
import com.morphocore.domain.Discipline
import com.morphocore.domain.Movement
import com.morphocore.domain.MuscleGroup

sealed class BrowseUiState {
    object Loading : BrowseUiState()
    data class Ready(
        val disciplines: List<Discipline>,
        val movementResults: List<Movement> = emptyList(),
        val query: String = "",
        val totalMovementCount: Int = 0,
        val difficultyBreakdown: Map<Difficulty, Int> = emptyMap(),
        val selectedDifficulty: Difficulty? = null,
        val disciplineBreakdowns: Map<String, Map<Difficulty, Int>> = emptyMap(),
        val availableMuscles: List<MuscleGroup> = emptyList(),
        val selectedMuscle: MuscleGroup? = null,
        val disciplineMuscleBreakdowns: Map<String, Map<MuscleGroup, Int>> = emptyMap(),
        val disciplineFilteredCounts: Map<String, Int> = emptyMap()
    ) : BrowseUiState()
    data class Error(val message: String) : BrowseUiState()
}
