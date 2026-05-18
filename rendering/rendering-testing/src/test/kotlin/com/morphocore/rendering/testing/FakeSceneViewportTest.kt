package com.morphocore.rendering.testing

import com.morphocore.domain.CameraPreset
import com.morphocore.domain.SceneEnvironment
import com.morphocore.rendering.api.LoadedModelFactory
import com.morphocore.rendering.api.ModelLoadResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FakeSceneViewportTest {

    @Test
    fun `loadModel records call and returns success by default`() = runTest {
        val fake = FakeSceneViewport()
        val result = fake.loadModel("content/karate/kick.glb")
        assertIs<ModelLoadResult.Success>(result)
        assertEquals(1, fake.calls.size)
        assertEquals(ViewportCall.LoadModel("content/karate/kick.glb"), fake.calls.first())
    }

    @Test
    fun `loadModel returns configured failure`() = runTest {
        val fake = FakeSceneViewport(loadResult = ModelLoadResult.Failure.FileNotFound)
        val result = fake.loadModel("missing.glb")
        assertIs<ModelLoadResult.Failure.FileNotFound>(result)
    }

    @Test
    fun `play records call`() {
        val fake = FakeSceneViewport()
        fake.play("kick_loop", loop = true)
        assertEquals(ViewportCall.Play("kick_loop", true), fake.calls.first())
    }

    @Test
    fun `pause records call`() {
        val fake = FakeSceneViewport()
        fake.pause()
        assertEquals(ViewportCall.Pause, fake.calls.first())
    }

    @Test
    fun `seekTo records call`() {
        val fake = FakeSceneViewport()
        fake.seekTo(1.5f)
        assertEquals(ViewportCall.SeekTo(1.5f), fake.calls.first())
    }

    @Test
    fun `setCamera records call`() {
        val fake = FakeSceneViewport()
        val preset = CameraPreset("side")
        fake.setCamera(preset, animated = false)
        assertEquals(ViewportCall.SetCamera(preset, false), fake.calls.first())
    }

    @Test
    fun `applySceneEnvironment records call`() {
        val fake = FakeSceneViewport()
        val env = SceneEnvironment("ibl.ktx2", 0xFFFFFFFFL, 50000f)
        fake.applySceneEnvironment(env)
        assertEquals(ViewportCall.ApplyEnvironment(env), fake.calls.first())
    }

    @Test
    fun `multiple calls are recorded in order`() = runTest {
        val fake = FakeSceneViewport()
        fake.loadModel("model.glb")
        fake.play("idle", true)
        fake.pause()
        assertEquals(3, fake.calls.size)
        assertIs<ViewportCall.LoadModel>(fake.calls[0])
        assertIs<ViewportCall.Play>(fake.calls[1])
        assertIs<ViewportCall.Pause>(fake.calls[2])
    }
}
