package com.morphocore.theme.impl.registry

import com.morphocore.domain.Theme
import com.morphocore.theme.api.ThemeAssetSource
import com.morphocore.theme.api.ThemeRegistry
import com.morphocore.theme.impl.parsing.ThemeParseResult
import com.morphocore.theme.impl.parsing.parseTheme
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class ThemeRegistryImpl(
    private val source: ThemeAssetSource,
    private val ioDispatcher: CoroutineDispatcher,
    @Suppress("UnusedPrivateMember") private val scope: CoroutineScope
) : ThemeRegistry {

    private val _themes = MutableStateFlow<List<Theme>>(emptyList())
    override val themes: StateFlow<List<Theme>> = _themes.asStateFlow()

    override suspend fun refresh() {
        withContext(ioDispatcher) {
            val loaded = mutableListOf<Theme>()
            for (themeId in source.listThemeIds()) {
                val raw = source.readThemeManifest(themeId) ?: continue
                val path = "${source.id}:themes/$themeId/theme.json"
                when (val result = parseTheme(path, raw)) {
                    is ThemeParseResult.Success -> loaded += result.theme
                    is ThemeParseResult.Failure -> { /* log and continue */ }
                }
            }
            _themes.value = loaded
        }
    }
}
