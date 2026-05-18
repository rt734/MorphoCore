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
}
