package com.morphocore.content.impl.parsing

import com.morphocore.domain.Difficulty
import com.morphocore.domain.MuscleGroup
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ManifestParserTest {

    private val karateJson: String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/karate-sample.json"))
            .bufferedReader().readText()

    private val yogaJson: String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/yoga-sample.json"))
            .bufferedReader().readText()

    @Test
    fun `parses karate manifest discipline fields correctly`() {
        val result = parseManifest("test:karate", karateJson)
        assertIs<ParseResult.Success>(result)
        assertEquals("karate", result.discipline.id)
        assertEquals("Karate", result.discipline.name)
        assertEquals("content/karate/icon.png", result.discipline.iconPath)
    }

    @Test
    fun `karate manifest produces three movements`() {
        val result = parseManifest("test:karate", karateJson) as ParseResult.Success
        assertEquals(3, result.movements.size)
    }

    @Test
    fun `movement ids are all prefixed with disciplineId`() {
        val result = parseManifest("test:karate", karateJson) as ParseResult.Success
        result.movements.forEach { m ->
            assertTrue(
                m.id.startsWith("karate."),
                "Expected id to start with 'karate.' but was '${m.id}'"
            )
        }
    }

    @Test
    fun `roundhouse kick has correct difficulty and muscles`() {
        val result = parseManifest("test:karate", karateJson) as ParseResult.Success
        val kick = result.movements.first { it.id == "karate.roundhouse_kick" }
        assertEquals(Difficulty.INTERMEDIATE, kick.difficulty)
        assertTrue(kick.muscles.contains(MuscleGroup.Quadriceps))
        assertTrue(kick.muscles.contains(MuscleGroup.HipFlexors))
        assertTrue(kick.muscles.contains(MuscleGroup.Core))
    }

    @Test
    fun `discipline movementIds matches parsed movement ids`() {
        val result = parseManifest("test:karate", karateJson) as ParseResult.Success
        assertEquals(result.movements.map { it.id }, result.discipline.movementIds)
    }

    @Test
    fun `yoga manifest parses two movements`() {
        val result = parseManifest("test:yoga", yogaJson) as ParseResult.Success
        assertEquals(2, result.movements.size)
    }

    @Test
    fun `malformed JSON returns Failure not thrown exception`() {
        val result = parseManifest("test:bad", "{ not: valid json }")
        assertIs<ParseResult.Failure>(result)
    }

    @Test
    fun `missing schemaVersion field returns Failure`() {
        val json = """{"disciplineId":"x","disciplineName":"X","movements":[]}"""
        val result = parseManifest("test:noversion", json)
        assertIs<ParseResult.Failure>(result)
    }

    @Test
    fun `unsupported schemaVersion returns Failure`() {
        val json = """{"schemaVersion":"99.0","disciplineId":"x","disciplineName":"X","movements":[]}"""
        val result = parseManifest("test:wrongver", json)
        assertIs<ParseResult.Failure>(result)
    }

    @Test
    fun `unrecognised muscle string becomes MuscleGroup Unknown`() {
        val json = """
        {
          "schemaVersion": "1.0", "disciplineId": "x", "disciplineName": "X",
          "movements": [{
            "id": "x.move", "name": "Move", "modelPath": "x.glb",
            "defaultClip": "idle",
            "clips": [{"name":"idle","durationSeconds":1.0,"fps":30}],
            "muscles": ["mystery_muscle"],
            "difficulty": "BEGINNER"
          }]
        }
        """.trimIndent()
        val result = parseManifest("test:x", json) as ParseResult.Success
        assertIs<MuscleGroup.Unknown>(result.movements.first().muscles.first())
    }

    @Test
    fun `parse failure carries the source path`() {
        val result = parseManifest("source:path/to/manifest.json", "bad json") as ParseResult.Failure
        assertEquals("source:path/to/manifest.json", result.error.path)
    }

    @Test
    fun `tags are parsed from movement`() {
        val result = parseManifest("test:karate", karateJson) as ParseResult.Success
        val kick = result.movements.first { it.id == "karate.roundhouse_kick" }
        assertEquals(listOf("strike", "kick"), kick.tags)
    }

    @Test
    fun `prerequisites are parsed from movement`() {
        val result = parseManifest("test:karate", karateJson) as ParseResult.Success
        val kick = result.movements.first { it.id == "karate.roundhouse_kick" }
        assertEquals(listOf("karate.front_kick"), kick.prerequisites)
    }

    @Test
    fun `commonMistakes descriptions are parsed from movement`() {
        val result = parseManifest("test:karate", karateJson) as ParseResult.Success
        val kick = result.movements.first { it.id == "karate.roundhouse_kick" }
        assertEquals(listOf("Not pivoting the supporting foot"), kick.commonMistakes)
    }

    @Test
    fun `movement with no tags has empty tags list`() {
        val result = parseManifest("test:karate", karateJson) as ParseResult.Success
        val kick = result.movements.first { it.id == "karate.front_kick" }
        assertEquals(listOf("strike", "kick"), kick.tags)
        val withoutOptionals = result.movements.first { it.id == "karate.roundhouse_kick" }
        assertTrue(withoutOptionals.prerequisites.isNotEmpty())
    }

    @Test
    fun `movement with no prerequisites has empty prerequisites list`() {
        val result = parseManifest("test:karate", karateJson) as ParseResult.Success
        val frontKick = result.movements.first { it.id == "karate.front_kick" }
        assertEquals(emptyList(), frontKick.prerequisites)
    }

    @Test
    fun `movement with no commonMistakes has empty list`() {
        val json = """
        {
          "schemaVersion": "1.0", "disciplineId": "x", "disciplineName": "X",
          "movements": [{
            "id": "x.move", "name": "Move", "modelPath": "x.glb",
            "defaultClip": "idle",
            "clips": [{"name":"idle","durationSeconds":1.0,"fps":30}],
            "muscles": ["core"], "difficulty": "BEGINNER"
          }]
        }
        """.trimIndent()
        val result = parseManifest("test:x", json) as ParseResult.Success
        assertEquals(emptyList(), result.movements.first().commonMistakes)
    }

    @Test
    fun `discipline description is parsed`() {
        val result = parseManifest("test:karate", karateJson) as ParseResult.Success
        assertTrue(result.discipline.description.isNotBlank())
    }

    @Test
    fun `movement description is parsed`() {
        val result = parseManifest("test:karate", karateJson) as ParseResult.Success
        val kick = result.movements.first { it.id == "karate.roundhouse_kick" }
        assertTrue(kick.description.isNotBlank())
    }

    @Test
    fun `discipline with no description field defaults to empty string`() {
        val json = """{"schemaVersion":"1.0","disciplineId":"x","disciplineName":"X","movements":[]}"""
        val result = parseManifest("test:x", json) as ParseResult.Success
        assertEquals("", result.discipline.description)
    }

    @Test
    fun `movement with no description field defaults to empty string`() {
        val json = """
        {
          "schemaVersion": "1.0", "disciplineId": "x", "disciplineName": "X",
          "movements": [{
            "id": "x.move", "name": "Move", "modelPath": "x.glb",
            "defaultClip": "idle",
            "clips": [{"name":"idle","durationSeconds":1.0,"fps":30}],
            "muscles": ["core"],
            "difficulty": "BEGINNER"
          }]
        }
        """.trimIndent()
        val result = parseManifest("test:x", json) as ParseResult.Success
        assertEquals("", result.movements.first().description)
    }

    @Test
    fun `yoga discipline description is parsed`() {
        val result = parseManifest("test:yoga", yogaJson) as ParseResult.Success
        assertTrue(result.discipline.description.isNotBlank())
    }

    @Test
    fun `iconPath defaults to null when absent from JSON`() {
        val result = parseManifest("test:yoga", yogaJson) as ParseResult.Success
        assertEquals(null, result.discipline.iconPath)
    }

    @Test
    fun `difficulty is parsed case-insensitively from lowercase string`() {
        val json = """
        {
          "schemaVersion": "1.0", "disciplineId": "x", "disciplineName": "X",
          "movements": [{
            "id": "x.move", "name": "Move", "modelPath": "x.glb",
            "defaultClip": "idle",
            "clips": [{"name":"idle","durationSeconds":1.0,"fps":30}],
            "muscles": ["core"],
            "difficulty": "advanced"
          }]
        }
        """.trimIndent()
        val result = parseManifest("test:x", json) as ParseResult.Success
        assertEquals(Difficulty.ADVANCED, result.movements.first().difficulty)
    }

    @Test
    fun `animation clip name, duration and fps are parsed correctly`() {
        val result = parseManifest("test:karate", karateJson) as ParseResult.Success
        val clip = result.movements.first { it.id == "karate.roundhouse_kick" }.clips.first()
        assertEquals("kick_loop", clip.name)
        assertEquals(2.0f, clip.durationSeconds)
        assertEquals(30, clip.fps)
    }

    @Test
    fun `cameraPreset is null when absent from movement`() {
        val json = """
        {
          "schemaVersion": "1.0", "disciplineId": "x", "disciplineName": "X",
          "movements": [{
            "id": "x.move", "name": "Move", "modelPath": "x.glb",
            "defaultClip": "idle",
            "clips": [{"name":"idle","durationSeconds":1.0,"fps":30}],
            "muscles": ["core"], "difficulty": "BEGINNER"
          }]
        }
        """.trimIndent()
        val result = parseManifest("test:x", json) as ParseResult.Success
        assertEquals(null, result.movements.first().cameraPreset)
    }

    @Test
    fun `cameraPreset is parsed when present`() {
        val json = """
        {
          "schemaVersion": "1.0", "disciplineId": "x", "disciplineName": "X",
          "movements": [{
            "id": "x.move", "name": "Move", "modelPath": "x.glb",
            "defaultClip": "idle",
            "clips": [{"name":"idle","durationSeconds":1.0,"fps":30}],
            "muscles": ["core"], "difficulty": "BEGINNER",
            "cameraPreset": "three_quarter"
          }]
        }
        """.trimIndent()
        val result = parseManifest("test:x", json) as ParseResult.Success
        assertEquals("three_quarter", result.movements.first().cameraPreset)
    }

    @Test
    fun `multiple commonMistakes are all parsed`() {
        val json = """
        {
          "schemaVersion": "1.0", "disciplineId": "x", "disciplineName": "X",
          "movements": [{
            "id": "x.move", "name": "Move", "modelPath": "x.glb",
            "defaultClip": "idle",
            "clips": [{"name":"idle","durationSeconds":1.0,"fps":30}],
            "muscles": ["core"], "difficulty": "BEGINNER",
            "commonMistakes": [
              {"description": "First mistake"},
              {"description": "Second mistake"}
            ]
          }]
        }
        """.trimIndent()
        val result = parseManifest("test:x", json) as ParseResult.Success
        assertEquals(listOf("First mistake", "Second mistake"), result.movements.first().commonMistakes)
    }

    @Test
    fun `defaultClip is parsed correctly`() {
        val result = parseManifest("test:karate", karateJson) as ParseResult.Success
        val kick = result.movements.first { it.id == "karate.roundhouse_kick" }
        assertEquals("kick_loop", kick.defaultClip)
    }
}
