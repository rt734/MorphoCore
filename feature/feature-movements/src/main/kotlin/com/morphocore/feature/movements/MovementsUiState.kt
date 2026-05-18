package com.morphocore.feature.movements

import com.morphocore.domain.Movement

sealed class MovementsUiState {
    object Loading : MovementsUiState()
    data class Ready(val disciplineName: String, val movements: List<Movement>) : MovementsUiState()
    data class Error(val message: String) : MovementsUiState()
}
