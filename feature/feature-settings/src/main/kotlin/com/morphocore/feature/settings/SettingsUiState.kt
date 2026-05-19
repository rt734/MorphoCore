package com.morphocore.feature.settings

import com.morphocore.domain.Theme

sealed class SettingsUiState {
    object Loading : SettingsUiState()
    data class Ready(val activeThemeId: String, val themes: List<Theme>) : SettingsUiState()
}
