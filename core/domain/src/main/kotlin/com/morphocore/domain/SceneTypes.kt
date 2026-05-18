package com.morphocore.domain

data class CameraPreset(val name: String)

data class SceneEnvironment(
    val iblPath: String,
    val directionalLightColor: Long,
    val directionalLightIntensity: Float
)
