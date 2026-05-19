package com.morphocore.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morphocore.theme.api.ThemeProvider
import com.morphocore.theme.api.ThemeRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeProvider: ThemeProvider,
    private val themeRegistry: ThemeRegistry
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        themeProvider.activeTheme,
        themeRegistry.themes
    ) { active, available ->
        SettingsUiState.Ready(activeThemeId = active.id, themes = available)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState.Loading
    )

    fun selectTheme(themeId: String) {
        viewModelScope.launch {
            themeProvider.setActiveTheme(themeId)
        }
    }
}
