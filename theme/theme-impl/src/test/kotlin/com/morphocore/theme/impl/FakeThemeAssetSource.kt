package com.morphocore.theme.impl

import com.morphocore.theme.api.ThemeAssetSource

class FakeThemeAssetSource(
    override val id: String = "fake",
    private val manifests: Map<String, String> = emptyMap()
) : ThemeAssetSource {
    override suspend fun listThemeIds(): List<String> = manifests.keys.toList()
    override suspend fun readThemeManifest(themeId: String): String? = manifests[themeId]
}
