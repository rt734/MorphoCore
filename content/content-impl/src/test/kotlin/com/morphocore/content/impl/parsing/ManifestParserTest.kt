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
}
