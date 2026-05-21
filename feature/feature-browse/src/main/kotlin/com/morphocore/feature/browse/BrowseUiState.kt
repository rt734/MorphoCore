package com.morphocore.feature.browse

import com.morphocore.domain.Discipline
import com.morphocore.domain.Movement

sealed class BrowseUiState {
    object Loading : BrowseUiState()
    data class Ready(
        val disciplines: List<Discipline>,
        val movementResults: List<Movement> = emptyList(),
        val query: String = "",
        val totalMovementCount: Int = 0
    ) : BrowseUiState()
    data class Error(val message: String) : BrowseUiState()
}
