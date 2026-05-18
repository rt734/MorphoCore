package com.morphocore.content.impl.registry

import com.morphocore.content.api.AssetSource
import com.morphocore.content.impl.parsing.ParseResult
import com.morphocore.content.impl.parsing.parseManifest
import com.morphocore.domain.Discipline
import com.morphocore.domain.Movement

internal data class ScanResult(
    val disciplines: List<Discipline>,
    val movements: List<Movement>,
    val failures: List<ParseResult.Failure>
)

internal suspend fun scanSource(source: AssetSource): ScanResult {
    val disciplines = mutableListOf<Discipline>()
    val movements = mutableListOf<Movement>()
    val failures = mutableListOf<ParseResult.Failure>()

    for (disciplineId in source.listDisciplineIds()) {
        val raw = source.readManifest(disciplineId) ?: continue
        val path = "${source.id}:content/$disciplineId/manifest.json"
        when (val result = parseManifest(path, raw)) {
            is ParseResult.Success -> {
                disciplines += result.discipline
                movements += result.movements
            }
            is ParseResult.Failure -> failures += result
        }
    }
    return ScanResult(disciplines, movements, failures)
}
