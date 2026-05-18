package com.morphocore.content.api

interface AssetSource {
    val id: String
    suspend fun listDisciplineIds(): List<String>
    // Returns the raw JSON string for the discipline manifest, or null if unavailable.
    suspend fun readManifest(disciplineId: String): String?
}
