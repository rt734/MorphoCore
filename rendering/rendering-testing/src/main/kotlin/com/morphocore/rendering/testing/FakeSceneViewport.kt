package com.morphocore.rendering.testing

import com.morphocore.domain.CameraPreset
import com.morphocore.domain.SceneEnvironment
import com.morphocore.rendering.api.LoadedModelFactory
import com.morphocore.rendering.api.ModelLoadResult
import com.morphocore.rendering.api.SceneViewport

class FakeSceneViewport(
    private val loadResult: ModelLoadResult = ModelLoadResult.Success(LoadedModelFactory.create("fake"))
) : SceneViewport {

    val calls = mutableListOf<ViewportCall>()

    override suspend fun loadModel(modelPath: String): ModelLoadResult {
        calls += ViewportCall.LoadModel(modelPath)
        return loadResult
    }

    override fun play(clipName: String, loop: Boolean) {
        calls += ViewportCall.Play(clipName, loop)
    }

    override fun pause() {
        calls += ViewportCall.Pause
    }

    override fun seekTo(timeSeconds: Float) {
        calls += ViewportCall.SeekTo(timeSeconds)
    }

    override fun setPlaybackSpeed(speed: Float) {
        calls += ViewportCall.SetPlaybackSpeed(speed)
    }

    override fun setCamera(preset: CameraPreset, animated: Boolean) {
        calls += ViewportCall.SetCamera(preset, animated)
    }

    override fun applySceneEnvironment(env: SceneEnvironment) {
        calls += ViewportCall.ApplyEnvironment(env)
    }
}
