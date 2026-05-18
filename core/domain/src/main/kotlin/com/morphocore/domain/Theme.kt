package com.morphocore.domain

data class Theme(
    val id: String,
    val name: String,
    val description: String,
    val isDefault: Boolean,
    val colors: MorphoColors,
    val typography: MorphoTypography,
    val shapes: MorphoShapes,
    val motion: MorphoMotion,
    val scene: SceneConfig
)
