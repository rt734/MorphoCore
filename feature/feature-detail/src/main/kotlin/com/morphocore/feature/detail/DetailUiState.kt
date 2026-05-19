package com.morphocore.feature.detail

import com.morphocore.domain.Movement

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Ready(val movement: Movement) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}
