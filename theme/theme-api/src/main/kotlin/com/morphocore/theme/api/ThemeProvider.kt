package com.morphocore.theme.api

import com.morphocore.domain.Theme
import kotlinx.coroutines.flow.StateFlow

interface ThemeProvider {
    val activeTheme: StateFlow<Theme>
    suspend fun setActiveTheme(themeId: String)
}
