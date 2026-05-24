package com.morphocore.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MovementTest {

    private fun clip() = AnimationClip("idle", 1.0f, 30)

    private fun validMovement(id: String = "karate.roundhouse_kick", disciplineId: String = "karate") =
        Movement(
            id = id,
            disciplineId = disciplineId,
            name = "Roundhouse Kick",
            modelPath = "content/karate/roundhouse_kick.glb",
            defaultClip = "idle",
            clips = listOf(clip()),
            muscles = listOf(MuscleGroup.Quadriceps),
            difficulty = Difficulty.INTERMEDIATE,
            tags = listOf("kick"),
            cameraPreset = null,
            prerequisites = emptyList(),
            commonMistakes = emptyList()
        )

    @Test
    fun `valid movement constructs successfully`() {
        val m = validMovement()
        assertEquals("karate", m.disciplineId)
        assertEquals("karate.roundhouse_kick", m.id)
    }

    @Test
    fun `id not prefixed with disciplineId is rejected`() {
        assertThrows<IllegalArgumentException> {
            validMovement(id = "yoga.roundhouse_kick", disciplineId = "karate")
        }
    }

    @Test
    fun `id missing dot separator is rejected`() {
        assertThrows<IllegalArgumentException> {
            validMovement(id = "karateroundhouse_kick", disciplineId = "karate")
        }
    }

    @Test
    fun `MuscleGroup Unknown wraps unrecognised string`() {
        val group = MuscleGroup.fromString("mystery_muscle")
        assertTrue(group is MuscleGroup.Unknown)
        assertEquals("mystery_muscle", (group as MuscleGroup.Unknown).raw)
    }

    @Test
    fun `Difficulty fromString is case-insensitive`() {
        assertEquals(Difficulty.BEGINNER, Difficulty.fromString("beginner"))
        assertEquals(Difficulty.ADVANCED, Difficulty.fromString("ADVANCED"))
    }

    @Test
    fun `Difficulty fromString throws on unknown value`() {
        assertThrows<IllegalArgumentException> { Difficulty.fromString("expert") }
    }

    // ── MuscleGroup.fromString ────────────────────────────────────────────

    @Test
    fun `MuscleGroup fromString returns HipFlexors for underscore variant`() {
        assertEquals(MuscleGroup.HipFlexors, MuscleGroup.fromString("hip_flexors"))
    }

    @Test
    fun `MuscleGroup fromString returns HipFlexors for hyphen variant`() {
        // Manifests written since Sprint 88 use "hip-flexors" (hyphen).
        // Without this mapping the parser silently produced Unknown("hip-flexors").
        assertEquals(MuscleGroup.HipFlexors, MuscleGroup.fromString("hip-flexors"))
    }

    @Test
    fun `MuscleGroup fromString is case-insensitive for all known groups`() {
        assertEquals(MuscleGroup.Quadriceps, MuscleGroup.fromString("QUADRICEPS"))
        assertEquals(MuscleGroup.Core,       MuscleGroup.fromString("Core"))
        assertEquals(MuscleGroup.Shoulders,  MuscleGroup.fromString("shoulders"))
        assertEquals(MuscleGroup.HipFlexors, MuscleGroup.fromString("HIP-FLEXORS"))
    }

    @Test
    fun `MuscleGroup fromString returns correct singleton for each known name`() {
        val expected = mapOf(
            "quadriceps" to MuscleGroup.Quadriceps,
            "hamstrings" to MuscleGroup.Hamstrings,
            "glutes"     to MuscleGroup.Glutes,
            "core"       to MuscleGroup.Core,
            "shoulders"  to MuscleGroup.Shoulders,
            "back"       to MuscleGroup.Back,
            "chest"      to MuscleGroup.Chest,
            "calves"     to MuscleGroup.Calves
        )
        expected.forEach { (token, group) ->
            assertEquals(group, MuscleGroup.fromString(token), "fromString($token) should return $group")
        }
    }

    @Test
    fun `Movement description defaults to empty string when not supplied`() {
        val m = validMovement()
        assertEquals("", m.description)
    }
}
