package com.morphocore.feature.detail

import com.morphocore.domain.Movement

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Ready(
        val movement: Movement,
        val relatedMovements: List<Movement> = emptyList(),
        val unlockedMovements: List<Movement> = emptyList(),
        val prerequisiteMovements: List<Movement> = emptyList()
    ) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}
