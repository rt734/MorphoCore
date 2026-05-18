package com.morphocore.rendering.testing

import com.morphocore.domain.CameraPreset
import com.morphocore.domain.SceneEnvironment

sealed class ViewportCall {
    data class LoadModel(val path: String) : ViewportCall()
    data class Play(val clipName: String, val loop: Boolean) : ViewportCall()
    object Pause : ViewportCall()
    data class SeekTo(val timeSeconds: Float) : ViewportCall()
    data class SetCamera(val preset: CameraPreset, val animated: Boolean) : ViewportCall()
    data class ApplyEnvironment(val env: SceneEnvironment) : ViewportCall()
}
