package com.morphocore.content.sources

import android.content.res.AssetManager
import com.morphocore.content.api.AssetSource
import java.io.IOException

// AssetManager paths strip the leading "assets/" prefix.
// A file stored at assets/content/karate/manifest.json is opened as
// "content/karate/manifest.json".
class BundledAssetSource(private val assets: AssetManager) : AssetSource {

    override val id: String = "bundled"

    override suspend fun listDisciplineIds(): List<String> =
        assets.list("content")?.toList() ?: emptyList()

    override suspend fun readManifest(disciplineId: String): String? =
        try {
            assets.open("content/$disciplineId/manifest.json").bufferedReader().readText()
        } catch (e: IOException) {
            null
        }
}
