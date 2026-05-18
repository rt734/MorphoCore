package com.morphocore.domain

data class SceneConfig(
    val skyboxPath: String?,
    val iblEnvironmentPath: String,
    val iblIntensity: Float,
    val directLight: DirectLightConfig,
    val ambientIntensity: Float,
    val groundPlane: GroundPlaneConfig,
    val postProcessing: PostProcessingConfig
)

data class DirectLightConfig(
    val color: Long,
    val intensityLux: Float,
    val azimuthDegrees: Float,
    val elevationDegrees: Float
)

data class GroundPlaneConfig(
    val enabled: Boolean,
    val color: Long,
    val opacity: Float
)

data class PostProcessingConfig(
    val bloomIntensity: Float,
    val vignetteIntensity: Float,
    val toneMapping: String
)
