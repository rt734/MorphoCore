package com.morphocore.feature.movements.ui

import com.morphocore.domain.AnimationClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovementRowClipLabelTest {

    private fun clip(duration: Float) = AnimationClip(name = "default", durationSeconds = duration, fps = 30)

    @Test
    fun `empty clips returns null`() {
        assertNull(clipMetaLabel(emptyList()))
    }

    @Test
    fun `single clip uses singular noun`() {
        assertEquals("1 clip · 1.4s", clipMetaLabel(listOf(clip(1.4f))))
    }

    @Test
    fun `two clips uses plural noun and sums duration`() {
        assertEquals("2 clips · 2.8s", clipMetaLabel(listOf(clip(1.4f), clip(1.4f))))
    }

    @Test
    fun `duration is formatted to one decimal place`() {
        assertEquals("1 clip · 1.0s", clipMetaLabel(listOf(clip(1.0f))))
    }

    @Test
    fun `duration rounds to one decimal`() {
        assertEquals("1 clip · 1.7s", clipMetaLabel(listOf(clip(1.65f))))
    }

    @Test
    fun `three clips uses plural noun and sums duration`() {
        assertEquals("3 clips · 4.2s", clipMetaLabel(listOf(clip(1.4f), clip(1.4f), clip(1.4f))))
    }

    @Test
    fun `single clip with large duration is formatted correctly`() {
        assertEquals("1 clip · 10.0s", clipMetaLabel(listOf(clip(10.0f))))
    }

    @Test
    fun `five clips sums all durations`() {
        val five = listOf(clip(1.0f), clip(1.0f), clip(1.0f), clip(1.0f), clip(1.0f))
        assertEquals("5 clips · 5.0s", clipMetaLabel(five))
    }

    @Test
    fun `clips with different durations sum correctly`() {
        val clips = listOf(
            AnimationClip("ready", 0.5f, 30),
            AnimationClip("kick", 1.5f, 30)
        )
        assertEquals("2 clips · 2.0s", clipMetaLabel(clips))
    }
}
