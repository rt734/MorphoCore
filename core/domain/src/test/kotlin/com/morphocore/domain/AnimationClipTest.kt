package com.morphocore.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class AnimationClipTest {

    @Test
    fun `valid clip constructs successfully`() {
        val clip = AnimationClip(name = "kick_loop", durationSeconds = 2.0f, fps = 30)
        assertEquals("kick_loop", clip.name)
        assertEquals(2.0f, clip.durationSeconds)
        assertEquals(30, clip.fps)
    }

    @Test
    fun `zero duration is rejected`() {
        assertThrows<IllegalArgumentException> {
            AnimationClip(name = "idle", durationSeconds = 0f, fps = 30)
        }
    }

    @Test
    fun `negative duration is rejected`() {
        assertThrows<IllegalArgumentException> {
            AnimationClip(name = "idle", durationSeconds = -1f, fps = 30)
        }
    }

    @Test
    fun `fps of zero is rejected`() {
        assertThrows<IllegalArgumentException> {
            AnimationClip(name = "idle", durationSeconds = 1f, fps = 0)
        }
    }

    @Test
    fun `fps of 121 is rejected`() {
        assertThrows<IllegalArgumentException> {
            AnimationClip(name = "idle", durationSeconds = 1f, fps = 121)
        }
    }

    @Test
    fun `fps of 120 is accepted`() {
        val clip = AnimationClip(name = "idle", durationSeconds = 1f, fps = 120)
        assertEquals(120, clip.fps)
    }
}
