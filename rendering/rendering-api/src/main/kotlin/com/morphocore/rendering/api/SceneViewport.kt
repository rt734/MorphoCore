package com.morphocore.rendering.api

import com.morphocore.domain.CameraPreset
import com.morphocore.domain.SceneEnvironment

interface SceneViewport {
    suspend fun loadModel(modelPath: String): ModelLoadResult
    fun play(clipName: String, loop: Boolean = true)
    fun pause()
    fun seekTo(timeSeconds: Float)
    fun setCamera(preset: CameraPreset, animated: Boolean = true)
    fun applySceneEnvironment(env: SceneEnvironment)
}
