package com.morphocore.theme.api

import com.morphocore.domain.Theme
import kotlinx.coroutines.flow.StateFlow

interface ThemeRegistry {
    val themes: StateFlow<List<Theme>>
    suspend fun refresh()
}
