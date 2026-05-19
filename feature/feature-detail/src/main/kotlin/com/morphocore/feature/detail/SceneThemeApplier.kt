package com.morphocore.feature.detail

import com.morphocore.domain.SceneConfig
import com.morphocore.domain.SceneEnvironment

fun SceneConfig.toSceneEnvironment(): SceneEnvironment = SceneEnvironment(
    iblPath = iblEnvironmentPath,
    directionalLightColor = directLight.color,
    directionalLightIntensity = directLight.intensityLux
)
