package com.morphocore.content.impl

import com.morphocore.content.api.AssetSource

class FakeAssetSource(
    override val id: String = "fake",
    private val manifests: Map<String, String> = emptyMap()
) : AssetSource {
    override suspend fun listDisciplineIds(): List<String> = manifests.keys.toList()
    override suspend fun readManifest(disciplineId: String): String? = manifests[disciplineId]
}
