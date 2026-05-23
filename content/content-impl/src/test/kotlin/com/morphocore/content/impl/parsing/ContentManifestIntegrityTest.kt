package com.morphocore.content.impl.parsing

import com.morphocore.domain.Difficulty
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Reads the live asset manifests from app/src/main/assets/content/ and asserts
 * structural invariants. These tests run in the content-impl JVM test task; Gradle
 * sets the working directory to the module root so the relative path resolves to
 * the app module assets.
 */
class ContentManifestIntegrityTest {

    private val assetsRoot = File("../../app/src/main/assets/content")

    private fun readManifest(disciplineId: String): ParseResult {
        val file = assetsRoot.resolve("$disciplineId/manifest.json")
        assumeTrue(file.exists(), "Skipping: manifest not found at ${file.absolutePath}")
        return parseManifest("content/$disciplineId/manifest.json", file.readText())
    }

    @ParameterizedTest
    @ValueSource(strings = ["karate", "yoga", "kung-fu", "gym", "calisthenics"])
    fun `manifest parses without error`(disciplineId: String) {
        val result = readManifest(disciplineId)
        assertIs<ParseResult.Success>(result, "Parse failed for $disciplineId")
    }

    @ParameterizedTest
    @ValueSource(strings = ["karate", "yoga", "kung-fu", "gym", "calisthenics"])
    fun `each discipline has exactly 12 movements`(disciplineId: String) {
        val result = readManifest(disciplineId) as ParseResult.Success
        assertEquals(12, result.movements.size,
            "$disciplineId should have 12 movements but has ${result.movements.size}")
    }

    @ParameterizedTest
    @ValueSource(strings = ["karate", "yoga", "kung-fu", "gym", "calisthenics"])
    fun `all movement ids are prefixed with disciplineId`(disciplineId: String) {
        val result = readManifest(disciplineId) as ParseResult.Success
        result.movements.forEach { m ->
            assertTrue(m.id.startsWith("$disciplineId."),
                "Movement '${m.id}' in $disciplineId does not start with '$disciplineId.'")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["karate", "yoga", "kung-fu", "gym", "calisthenics"])
    fun `defaultClip names a clip declared in clips list`(disciplineId: String) {
        val result = readManifest(disciplineId) as ParseResult.Success
        result.movements.forEach { m ->
            val clipNames = m.clips.map { it.name }.toSet()
            assertTrue(m.defaultClip in clipNames,
                "${m.id}: defaultClip '${m.defaultClip}' not found in clips $clipNames")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["karate", "yoga", "kung-fu", "gym", "calisthenics"])
    fun `all clips have positive duration and valid fps`(disciplineId: String) {
        val result = readManifest(disciplineId) as ParseResult.Success
        result.movements.forEach { m ->
            m.clips.forEach { clip ->
                assertTrue(clip.durationSeconds > 0f,
                    "${m.id} clip '${clip.name}': durationSeconds must be > 0")
                assertTrue(clip.fps in 1..120,
                    "${m.id} clip '${clip.name}': fps ${clip.fps} not in 1..120")
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["karate", "yoga", "kung-fu", "gym", "calisthenics"])
    fun `prerequisite ids exist within the same discipline`(disciplineId: String) {
        val result = readManifest(disciplineId) as ParseResult.Success
        val allIds = result.movements.map { it.id }.toSet()
        result.movements.forEach { m ->
            m.prerequisites.forEach { prereqId ->
                assertTrue(prereqId in allIds,
                    "${m.id}: prerequisite '$prereqId' not found in $disciplineId movements")
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["karate", "yoga", "kung-fu", "gym", "calisthenics"])
    fun `movement ids are unique within each discipline`(disciplineId: String) {
        val result = readManifest(disciplineId) as ParseResult.Success
        val ids = result.movements.map { it.id }
        assertEquals(ids.size, ids.distinct().size,
            "$disciplineId has duplicate movement IDs: ${ids.groupBy { it }.filter { it.value.size > 1 }.keys}")
    }

    @ParameterizedTest
    @ValueSource(strings = ["karate", "yoga", "kung-fu", "gym", "calisthenics"])
    fun `discipline movementIds matches parsed movement order`(disciplineId: String) {
        val result = readManifest(disciplineId) as ParseResult.Success
        assertEquals(result.movements.map { it.id }, result.discipline.movementIds)
    }

    @ParameterizedTest
    @ValueSource(strings = ["karate", "yoga", "kung-fu", "gym", "calisthenics"])
    fun `discipline has a non-blank description`(disciplineId: String) {
        val result = readManifest(disciplineId) as ParseResult.Success
        assertTrue(result.discipline.description.isNotBlank(),
            "$disciplineId: discipline description is blank")
    }

    @ParameterizedTest
    @ValueSource(strings = ["karate", "yoga", "kung-fu", "gym", "calisthenics"])
    fun `every movement has a non-blank description`(disciplineId: String) {
        val result = readManifest(disciplineId) as ParseResult.Success
        result.movements.forEach { m ->
            assertTrue(m.description.isNotBlank(), "${m.id}: movement description is blank")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["karate", "yoga", "kung-fu", "gym", "calisthenics"])
    fun `every movement has at least one tag`(disciplineId: String) {
        val result = readManifest(disciplineId) as ParseResult.Success
        result.movements.forEach { m ->
            assertTrue(m.tags.isNotEmpty(), "${m.id} has no tags")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["karate", "yoga", "kung-fu", "gym", "calisthenics"])
    fun `every non-beginner movement has at least one prerequisite`(disciplineId: String) {
        val result = readManifest(disciplineId) as ParseResult.Success
        result.movements
            .filter { it.difficulty != Difficulty.BEGINNER }
            .forEach { m ->
                assertTrue(m.prerequisites.isNotEmpty(),
                    "${m.id} is ${m.difficulty} but has no prerequisites")
            }
    }

    @ParameterizedTest
    @ValueSource(strings = ["karate", "yoga", "kung-fu", "gym", "calisthenics"])
    fun `every movement has at least one common mistake`(disciplineId: String) {
        val result = readManifest(disciplineId) as ParseResult.Success
        result.movements.forEach { m ->
            assertTrue(m.commonMistakes.isNotEmpty(), "${m.id} has no common mistakes")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["karate", "yoga", "kung-fu", "gym", "calisthenics"])
    fun `every movement has at least one muscle group`(disciplineId: String) {
        val result = readManifest(disciplineId) as ParseResult.Success
        result.movements.forEach { m ->
            assertTrue(m.muscles.isNotEmpty(), "${m.id} has no muscle groups")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["karate", "yoga", "kung-fu", "gym", "calisthenics"])
    fun `cameraPreset is null or one of the four known values`(disciplineId: String) {
        val knownPresets = setOf("front", "side", "top", "three_quarter")
        val result = readManifest(disciplineId) as ParseResult.Success
        result.movements.forEach { m ->
            val preset = m.cameraPreset
            if (preset != null) {
                assertTrue(preset in knownPresets,
                    "${m.id}: cameraPreset '$preset' is not in $knownPresets")
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["karate", "yoga", "kung-fu", "gym", "calisthenics"])
    fun `every movement has at least one clip`(disciplineId: String) {
        val result = readManifest(disciplineId) as ParseResult.Success
        result.movements.forEach { m ->
            assertTrue(m.clips.isNotEmpty(), "${m.id} has no animation clips")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["karate", "yoga", "kung-fu", "gym", "calisthenics"])
    fun `prerequisite chains contain no cycles`(disciplineId: String) {
        val result = readManifest(disciplineId) as ParseResult.Success
        val adjMap = result.movements.associate { m -> m.id to m.prerequisites.toSet() }

        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()

        fun hasCycle(node: String): Boolean {
            if (node in inStack) return true
            if (node in visited) return false
            visited += node
            inStack += node
            val hasCycle = adjMap[node].orEmpty().any { hasCycle(it) }
            inStack -= node
            return hasCycle
        }

        adjMap.keys.forEach { id ->
            assertTrue(!hasCycle(id), "$disciplineId: prerequisite cycle detected starting from $id")
        }
    }
}
