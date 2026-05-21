package com.morphocore.content.impl.parsing

import com.morphocore.content.api.ContentError
import com.morphocore.domain.AnimationClip
import com.morphocore.domain.Difficulty
import com.morphocore.domain.Discipline
import com.morphocore.domain.Movement
import com.morphocore.domain.MuscleGroup
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class ManifestDto(
    val schemaVersion: String,
    val disciplineId: String,
    val disciplineName: String,
    val description: String = "",
    val iconPath: String? = null,
    val movements: List<MovementDto>
)

@Serializable
internal data class MovementDto(
    val id: String,
    val name: String,
    val modelPath: String,
    val defaultClip: String,
    val clips: List<AnimationClipDto>,
    val muscles: List<String>,
    val difficulty: String,
    val tags: List<String> = emptyList(),
    val cameraPreset: String? = null,
    val prerequisites: List<String> = emptyList(),
    val commonMistakes: List<CommonMistakeDto> = emptyList(),
    val audioCuesPath: String? = null
)

@Serializable
internal data class AnimationClipDto(
    val name: String,
    val durationSeconds: Float,
    val fps: Int
)

@Serializable
internal data class CommonMistakeDto(val description: String)

internal sealed class ParseResult {
    data class Success(val discipline: Discipline, val movements: List<Movement>) : ParseResult()
    data class Failure(val error: ContentError.ManifestParseFailure) : ParseResult()
}

private val json = Json { ignoreUnknownKeys = true }

internal fun parseManifest(path: String, jsonString: String): ParseResult =
    try {
        val dto = json.decodeFromString<ManifestDto>(jsonString)
        if (dto.schemaVersion != "1.0") {
            return ParseResult.Failure(
                ContentError.ManifestParseFailure(
                    path = path,
                    cause = IllegalArgumentException("Unsupported schemaVersion: ${dto.schemaVersion}")
                )
            )
        }
        val movements = dto.movements.map { m ->
            Movement(
                id = m.id,
                disciplineId = dto.disciplineId,
                name = m.name,
                modelPath = m.modelPath,
                defaultClip = m.defaultClip,
                clips = m.clips.map { c -> AnimationClip(c.name, c.durationSeconds, c.fps) },
                muscles = m.muscles.map { MuscleGroup.fromString(it) },
                difficulty = Difficulty.fromString(m.difficulty),
                tags = m.tags,
                cameraPreset = m.cameraPreset,
                prerequisites = m.prerequisites,
                commonMistakes = m.commonMistakes.map { it.description }
            )
        }
        val discipline = Discipline(
            id = dto.disciplineId,
            name = dto.disciplineName,
            description = dto.description,
            iconPath = dto.iconPath,
            movementIds = movements.map { it.id }
        )
        ParseResult.Success(discipline, movements)
    } catch (e: Exception) {
        ParseResult.Failure(ContentError.ManifestParseFailure(path, e))
    }
