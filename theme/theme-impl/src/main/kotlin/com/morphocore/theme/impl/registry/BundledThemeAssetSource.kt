package com.morphocore.theme.impl.registry

import android.content.res.AssetManager
import com.morphocore.theme.api.ThemeAssetSource
import java.io.IOException

class BundledThemeAssetSource(private val assets: AssetManager) : ThemeAssetSource {
    override val id: String = "bundled-themes"

    override suspend fun listThemeIds(): List<String> =
        assets.list("themes")?.toList() ?: emptyList()

    override suspend fun readThemeManifest(themeId: String): String? =
        try {
            assets.open("themes/$themeId/theme.json").bufferedReader().readText()
        } catch (e: IOException) {
            null
        }
}
