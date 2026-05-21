package com.morphocore.feature.movements

import com.morphocore.domain.Difficulty
import com.morphocore.domain.Movement

sealed class MovementsUiState {
    object Loading : MovementsUiState()
    data class Ready(
        val disciplineName: String,
        val movements: List<Movement>,
        val totalCount: Int = 0,
        val difficultyBreakdown: Map<Difficulty, Int> = emptyMap(),
        val availableTags: List<String> = emptyList(),
        val selectedTags: Set<String> = emptySet(),
        val selectedDifficulties: Set<Difficulty> = emptySet(),
        val sort: MovementsSort = MovementsSort.BY_DIFFICULTY,
        val query: String = ""
    ) : MovementsUiState()
    data class Error(val message: String) : MovementsUiState()
}
