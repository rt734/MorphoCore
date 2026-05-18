package com.morphocore.theme.api

interface ThemeAssetSource {
    val id: String
    suspend fun listThemeIds(): List<String>
    suspend fun readThemeManifest(themeId: String): String?
}
