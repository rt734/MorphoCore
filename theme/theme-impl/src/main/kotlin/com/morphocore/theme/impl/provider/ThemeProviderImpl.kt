package com.morphocore.theme.impl.provider

import com.morphocore.domain.Theme
import com.morphocore.theme.api.ThemeProvider
import com.morphocore.theme.impl.registry.ThemeRegistryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeProviderImpl(
    private val registry: ThemeRegistryImpl,
    private val prefs: ThemePreferences
) : ThemeProvider {

    private val _activeTheme = MutableStateFlow(resolveInitialTheme())
    override val activeTheme: StateFlow<Theme> = _activeTheme.asStateFlow()

    override suspend fun setActiveTheme(themeId: String) {
        val found = registry.themes.value.find { it.id == themeId } ?: return
        _activeTheme.value = found
        prefs.saveThemeId(themeId)
    }

    private fun resolveInitialTheme(): Theme {
        val themes = registry.themes.value
        val savedId = prefs.getLastThemeId()
        if (savedId != null) {
            val saved = themes.find { it.id == savedId }
            if (saved != null) return saved
        }
        return themes.find { it.isDefault } ?: themes.first()
    }
}
