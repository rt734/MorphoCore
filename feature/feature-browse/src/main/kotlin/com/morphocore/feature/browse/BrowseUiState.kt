package com.morphocore.feature.browse

import com.morphocore.domain.Discipline

sealed class BrowseUiState {
    object Loading : BrowseUiState()
    data class Ready(val disciplines: List<Discipline>) : BrowseUiState()
    data class Error(val message: String) : BrowseUiState()
}
